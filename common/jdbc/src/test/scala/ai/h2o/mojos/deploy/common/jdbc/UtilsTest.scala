package ai.h2o.mojos.deploy.common.jdbc

import ai.h2o.mojos.deploy.common.jdbc.utils._
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.junit.jupiter.api.{Disabled, Test}
import org.junit.jupiter.api.Assertions.{assertFalse, assertTrue}
import org.scalatest.junit.AssertionsForJUnit


class UtilsTest extends AssertionsForJUnit {
  val stringWithDoubleQuotes: String = "\"helloworld\""
  val stringWithSingleQuotes: String = "'helloworld'"
  val conf: SparkConf = new SparkConf()
    .setAppName(System.getProperty("app.name", "h2oaiSparkSqlScorer"))
    .setMaster(System.getProperty("spark.master.node", "local[*]"))
    .set("spark.ui.enabled", "false")
  var sparkSession: SparkSession = _

  @Test
  def sanitizeDoubleQuotedString: Unit = {
    val cleanedString: String = sanitizeInputString(stringWithDoubleQuotes)
    assertFalse(cleanedString.endsWith("\""))
    assertFalse(cleanedString.startsWith("\'"))
  }

  @Test
  def sanitizeSingleQuotedString: Unit = {
    val cleanedString: String = sanitizeInputString(stringWithSingleQuotes)
    assertFalse(cleanedString.startsWith("'"))
    assertFalse(cleanedString.endsWith("'"))
  }

  @Test
  @Disabled
  def testCastDataFrameShort: Unit = {
    sparkSession = SparkSession.builder().config(conf).getOrCreate()
    val data = Seq(("Java", 20000, 23234.234), ("Python", 100000, 56524.11), ("Scala", 3000, 9348.2))
    val df = sparkSession.createDataFrame(data)
    val responseDf: Array[String] = castDataFrameToArray(df, 5)
    assertTrue(responseDf.isInstanceOf[Array[String]])
    assertTrue(responseDf.length.equals(3))
  }

  @Test
  @Disabled
  def testCastDataFrameLong: Unit = {
    sparkSession = SparkSession.builder().config(conf).getOrCreate()
    val data = Seq(
      ("Java", 20000, 23234.234),
      ("Python", 100000, 56524.11),
      ("Scala", 3000, 9348.2),
      ("Java1", 200001, 123234.234),
      ("Python1", 1000001, 156524.11),
      ("Scala1", 30001, 19348.2),
      ("Java2", 200002, 223234.234),
      ("Python2", 1000002, 256524.11),
      ("Scala2", 30002, 29348.2)
    )
    val df = sparkSession.createDataFrame(data)
    val responseDf: Array[String] = castDataFrameToArray(df, 5)
    assertTrue(responseDf.isInstanceOf[Array[String]])
    assertTrue(responseDf.length.equals(5))
  }
}
