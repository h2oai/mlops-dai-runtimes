package ai.h2o.mojos.deploy.common.jdbc

import java.nio.charset.StandardCharsets

import ai.h2o.mojos.deploy.common.rest.jdbc.model.ScoreRequest
import ai.h2o.mojos.deploy.common.rest.jdbc.model.ScoreRequest.SaveMethodEnum
import org.apache.spark.sql.{DataFrame, SaveMode}

object utils {
  def castToInt(number: Any): Int = {
    number match {
      case bigDecimal: BigDecimal => bigDecimal.intValue
      case long: Long => long.toInt
      case double: Double => double.toInt
      case integer: Integer => integer.toInt
      case string: String => string.toInt
      case _ => number.asInstanceOf[Int]
    }
  }

  def castSaveModeFromRequest(scoreRequest: ScoreRequest): SaveMode = {
    scoreRequest.getSaveMethod match {
      case SaveMethodEnum.APPEND => SaveMode.Append
      case SaveMethodEnum.OVERWRITE => SaveMode.Overwrite
      case SaveMethodEnum.IGNORE => SaveMode.Ignore
      case SaveMethodEnum.ERROR => SaveMode.ErrorIfExists
      // NOTE: Fall through case catches SaveMethodEnum.PREVIEW
      // It should never reach here anyways.
      case _ => SaveMode.Append
    }
  }

  def castDataFrameToArray(df: DataFrame, limit: Int): Array[String] = {
    df.limit(limit).collect().map(_.toString())
  }

  def sanitizeInputString(input: String): String = {
    input.stripSuffix("\"").stripPrefix("\"").stripSuffix("'").stripPrefix("'")
  }
}
