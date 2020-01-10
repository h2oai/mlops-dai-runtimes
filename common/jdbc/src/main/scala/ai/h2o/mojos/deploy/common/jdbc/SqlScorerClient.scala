package ai.h2o.mojos.deploy.common.jdbc

import java.io.{File, IOException}
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.{Arrays, Properties}

import ai.h2o.mojos.deploy.common.rest.jdbc.model.ScoreRequest.SaveMethodEnum
import ai.h2o.mojos.deploy.common.rest.jdbc.model.{Model, ModelSchema, ScoreRequest}
import ai.h2o.mojos.runtime.lic.LicenseException
import ai.h2o.sparkling.ml.models.H2OMOJOPipelineModel
import com.google.common.base.{Preconditions, Strings}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
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
  private final val jdbcConfig: JdbcConfig = loadJdbcConfigFromFile()

  private final val sqlProperties: Properties = setSqlProperties()

  def scoreQuery(scoreRequest: ScoreRequest): Array[String] = {
    var preds: DataFrame = null
    val df: DataFrame = read(scoreRequest)
    if (!scoreRequest.getIdColumn.isEmpty) {
      preds = pipeline
        .transform(df)
        .select(sanitizeInputString(scoreRequest.getIdColumn), "prediction.*")
    } else {
      preds = pipeline.transform(df).select("prediction.*")
    }
    if (!scoreRequest.getSaveMethod.equals(SaveMethodEnum.PREVIEW)) {
      logger.info(
        "Received expected save method {}, attempting to write to database",
        scoreRequest.getSaveMethod.toString
      )
      write(scoreRequest, preds)
    }
    castDataFrameToArray(preds)
  }

  def getModelInfo: Array[String] = {
    pipeline.getFeaturesCols()
  }

  def getModelId: String = {
    pipeline.uid
  }

  private def read(scoreRequest: ScoreRequest): DataFrame = {
    logger.info("Recieved request to score sql query")
    logger.info(
      String.format(
        "Query inputs: query - %s, outputTable - %s, idColumn - %s",
        scoreRequest.getSqlQuery,
        scoreRequest.getOutputTable,
        scoreRequest.getIdColumn
      )
    )
    val sqlQuery: String = String.format(
      "(%s) queryTable",
      sanitizeInputString(scoreRequest.getSqlQuery)
    )
    if (!scoreRequest.getIdColumn.isEmpty) {
      logger.info("Generating partitions for query: {}", sqlQuery)
      generatePartitionMapping(
        jdbcConfig.dbConnectionString,
        sqlQuery,
        sanitizeInputString(scoreRequest.getIdColumn)
      )
    }
    logger.info("Executing query")
    doQuery(jdbcConfig.dbConnectionString, sqlQuery)
  }

  private def write(scoreRequest: ScoreRequest, predsDf: DataFrame): Unit = {
    logger.info("Writing scored data to table")
    predsDf
      .write
      .mode(castSaveModeFromRequest(scoreRequest))
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
      logger.info("Mojo pipeline successfully loaded ({})", pipeline.uid)
      pipeline
    } catch {
      case e: IOException =>
        throw new RuntimeException("Unable to load mojo", e)
      case e: LicenseException =>
        throw new RuntimeException("License file not found", e)
    }
  }

  private def loadJdbcConfigFromFile(): JdbcConfig = {
    Preconditions.checkArgument(
      !Strings.isNullOrEmpty(JDBC_CONFIG_FILE),
      "Path to jdbc config file is not specified, set the %s property",
      JDBC_CONFIG_FILE_PROPERTY
    )
    logger.info("Loading the JDBC Config file from path {}", JDBC_CONFIG_FILE)
    try {
      new JdbcConfig(JDBC_CONFIG_FILE)
    } catch {
      case e: IOException =>
        throw new RuntimeException("Unable to load JDBC Config file", e)
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

  private def castSaveModeFromRequest(scoreRequest: ScoreRequest): SaveMode = {
    scoreRequest.getSaveMethod match {
      case SaveMethodEnum.APPEND => SaveMode.Append
      case SaveMethodEnum.OVERWRITE => SaveMode.Overwrite
      case SaveMethodEnum.IGNORE => SaveMode.Ignore
      case SaveMethodEnum.ERROR => SaveMode.ErrorIfExists
      // Fall through case catches SaveMethodEnum.PREVIEW since it should never get passed here anyways
      case _ => SaveMode.Append
    }
  }

  private def castDataFrameToArray(df: DataFrame): Array[String] = {
    df.limit(5).collect().map(_.toString())
  }

  private def sanitizeInputString(input: String): String = {
    val cleanedString: String = new String(input.getBytes(StandardCharsets.UTF_8))
    cleanedString.stripSuffix("\"").stripPrefix("\"")
  }
}