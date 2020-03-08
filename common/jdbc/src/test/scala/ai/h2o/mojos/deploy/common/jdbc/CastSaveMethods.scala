package ai.h2o.mojos.deploy.common.jdbc

import ai.h2o.mojos.deploy.common.jdbc.utils._
import ai.h2o.mojos.deploy.common.rest.jdbc.model.ScoreRequest
import ai.h2o.mojos.deploy.common.rest.jdbc.model.ScoreRequest.SaveMethodEnum
import org.apache.spark.sql.SaveMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.scalatest.junit.AssertionsForJUnit

class CastSaveMethods extends AssertionsForJUnit {
  val scoreRequestPreview: SaveMethodEnum = SaveMethodEnum.PREVIEW
  val scoreRequest: ScoreRequest = new ScoreRequest

  @Test
  def castFromSaveMethodAppend: Unit = {
    scoreRequest.setSaveMethod(SaveMethodEnum.APPEND)
    assertTrue(castSaveModeFromRequest(scoreRequest).equals(SaveMode.Append))
  }

  @Test
  def castFromSaveMethodError: Unit = {
    scoreRequest.setSaveMethod(SaveMethodEnum.ERROR)
    assertTrue(castSaveModeFromRequest(scoreRequest).equals(SaveMode.ErrorIfExists))
  }

  @Test
  def castFromSaveMethodIgnore: Unit = {
    scoreRequest.setSaveMethod(SaveMethodEnum.IGNORE)
    assertTrue(castSaveModeFromRequest(scoreRequest).equals(SaveMode.Ignore))
  }

  @Test
  def castFromSaveMethodOverwrite: Unit = {
    scoreRequest.setSaveMethod(SaveMethodEnum.OVERWRITE)
    assertTrue(castSaveModeFromRequest(scoreRequest).equals(SaveMode.Overwrite))
  }

  @Test
  def castFromSaveMethodPreview: Unit = {
    // Note: that since SaveMethod PREVIEW does not exist in apache spark get default method APPEND
    scoreRequest.setSaveMethod(SaveMethodEnum.PREVIEW)
    assertTrue(castSaveModeFromRequest(scoreRequest).equals(SaveMode.Append))
  }
}
