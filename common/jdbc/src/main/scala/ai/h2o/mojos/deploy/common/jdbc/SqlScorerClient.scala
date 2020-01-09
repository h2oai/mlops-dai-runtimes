package ai.h2o.mojos.deploy.common.jdbc

import java.io.{File, IOException}
import java.net.URL
import java.util
import java.util.Properties

import ai.h2o.mojos.deploy.common.rest.jdbc.model.{Model, ModelSchema, ScoreRequest}
import ai.h2o.mojos.runtime.lic.LicenseException
import ai.h2o.sparkling.ml.models.H2OMOJOPipelineModel
import com.google.common.base.{Preconditions, Strings}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession, SaveMode}
import org.slf4j.{Logger, LoggerFactory}

object SqlScorerClient {
  def init(): SqlScorerClient = {
    val sqlScorerClient = new SqlScorerClient()
    sqlScorerClient
  }
}

class SqlScorerClient {
  val logger: Logger = LoggerFactory.getLogger(classOf[SqlScorerClient])

  private final val conf: SparkConf = new SparkConf()
    .setAppName(System.getProperty("app.name", "h2oaiSparkSqlScorer"))
    .setMaster(System.getProperty("spark.master.node", "local[*]"))
    .set("spark.ui.enabled", "false")
  private final val sparkSession: SparkSession = SparkSession.builder().config(conf).getOrCreate()

  private final val MOJO_PIPELINE_PATH_PROPERTY: String = "mojo.path"
  private final val MOJO_PIPELINE_PATH: String = System.getProperty(MOJO_PIPELINE_PATH_PROPERTY)
  final val pipeline: H2OMOJOPipelineModel = loadMojoPipelineFromFile()

  private final val JDBC_CONFIG_FILE_PROPERTY: String = "jdbc.config.path"
  private final val JDBC_CONFIG_FILE: String = System.getProperty(JDBC_CONFIG_FILE_PROPERTY)
  private final val jdbcConfig: JdbcConfig = new JdbcConfig(JDBC_CONFIG_FILE)

  private final val sqlProperties: Properties = setSqlProperties()

  def scoreQuery(scoreRequest: ScoreRequest): Unit = {
    var preds: DataFrame = null
    val df: DataFrame = read(scoreRequest)
    if (!scoreRequest.getIdColumn.isEmpty) {
      preds = pipeline.transform(df).select(scoreRequest.getIdColumn, "prediction.*")
    } else {
      preds = pipeline.transform(df)
    }
    write(scoreRequest, preds)
  }

  def getModelInfo: Model = {
    val model: Model = new Model
    model.setId(pipeline.uid)
    val modelSchema: ModelSchema = new ModelSchema
    modelSchema.setInputFields(pipeline.getFeaturesCols().asInstanceOf[util.List[String]])
    model.setSchema(modelSchema)
    model
  }

  def getModelId: String = {
    pipeline.uid
  }

  private def read(scoreRequest: ScoreRequest): DataFrame = {
    logger.info("Recieved request to score sql query")
    logger.info(
      String.format(
        "Query inputs: query - %s, outputTable - %s, idColumn - %s",
        scoreRequest.getQuery,
        scoreRequest.getOutputTable,
        scoreRequest.getIdColumn
      )
    )
    if (!scoreRequest.getIdColumn.isEmpty) {
      logger.info("Generating partitions for query")
      generatePartitionMapping(
        jdbcConfig.dbConnectionString,
        "(SELECT * FROM creditcardtrain) testQuery",
        scoreRequest.getIdColumn
      )
    }
    logger.info("Executing query")
    doQuery(jdbcConfig.dbConnectionString, scoreRequest.getQuery)
  }

  private def write(scoreRequest: ScoreRequest, predsDf: DataFrame): Unit = {
    logger.info("Writing scored data to table")
    predsDf
      .write
      .mode(SaveMode.Append)
      .jdbc(
        url = jdbcConfig.dbConnectionString,
        table = scoreRequest.getOutputTable,
        sqlProperties
      )
  }

  private def loadMojoPipelineFromFile(): H2OMOJOPipelineModel = {
    Preconditions.checkArgument(
      !Strings.isNullOrEmpty(MOJO_PIPELINE_PATH),
      "Path to mojo pipeline not specified, set the %s property",
      MOJO_PIPELINE_PATH_PROPERTY
    )
    logger.info("Spark: Loading Mojo pipeline from path {}", MOJO_PIPELINE_PATH)
    var mojoFile: File = new File(MOJO_PIPELINE_PATH)
    if (!mojoFile.isFile) {
      val classLoader: ClassLoader = SqlScorerClient.getClass.getClassLoader
      val resourcePath: URL = classLoader.getResource(MOJO_PIPELINE_PATH)
      if (resourcePath != null) {
        mojoFile = new File(resourcePath.getFile)
      }
    }
    if (!mojoFile.isFile) {
      throw new RuntimeException("Could not load mojo")
    }
    try {
      val pipeline: H2OMOJOPipelineModel = H2OMOJOPipelineModel.createFromMojo(mojoFile.getPath)
      logger.info("Spark: Mojo pipeline successfully loaded ({})", pipeline.uid)
      pipeline
    } catch {
      case e: IOException =>
        throw new RuntimeException("Unable to load mojo", e)
      case e: LicenseException =>
        throw new RuntimeException("License file not found", e)
    }
  }

  private def generatePartitionMapping(url: String, query: String, idColumn: String): Unit = {
    val tmpDf: DataFrame = doQuery(url, query)
    val minimum: Int = castToInt(tmpDf.selectExpr(String.format("MIN(%s)", idColumn)).first().get(0))
    val maximum: Int = castToInt(tmpDf.selectExpr(String.format("MAX(%s)", idColumn)).first().get(0))
    val numPartitions: Int = math.ceil((maximum - minimum + 1) / 50000.0).toInt
    logger.info(String.format(
      "%s Partitions generated with: upper bound [%s], lower bound [%s], and partition column [%s]",
      numPartitions.toString,
      maximum.toString,
      minimum.toString,
      idColumn
    ))
    sqlProperties.setProperty("numPartitions", numPartitions.toString)
    sqlProperties.setProperty("upperBound", maximum.toString)
    sqlProperties.setProperty("lowerBound", minimum.toString)
    sqlProperties.setProperty("partitionColumn", idColumn)
  }

  private def doQuery(url: String, query: String): DataFrame = {
    sparkSession.read.jdbc(
      url = url,
      table = query,
      properties = sqlProperties
    )
  }

  private def setSqlProperties(): Properties = {
    val properties: Properties = new Properties()
    properties.setProperty("user", jdbcConfig.dbUser)
    properties.setProperty("password", jdbcConfig.dbPassword)
    properties.setProperty("driver", jdbcConfig.dbDriver)
    properties
  }

  private def castToInt(number: Any): Int = {
    logger.info("Converting scala numeric types to java types")
    number match {
      case bigDecimal: BigDecimal => bigDecimal.intValue
      case long: Long => long.toInt
      case double: Double => double.toInt
      case integer: Integer => integer.toInt
      case string: String => string.toInt
      case _ => number.asInstanceOf[Int]
    }
  }
}