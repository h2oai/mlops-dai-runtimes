package ai.h2o.mojos.deploy.common.jdbc

import ai.h2o.mojos.deploy.common.jdbc.utils._
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.scalatest.junit.AssertionsForJUnit

class CastToIntTest extends AssertionsForJUnit {
  var bigDecimal: BigDecimal = BigDecimal.valueOf(128837.000)
  var long: Long = 1234
  var double: Double = 54321.00000
  var integer: Integer = 12345
  var string: String = "9876"

  @Test
  def castBigDecimalToInt: Unit = {
    assertTrue(castToInt(bigDecimal).isInstanceOf[Int])
  }

  @Test
  def castLongToInt: Unit = {
    assertTrue(castToInt(long).isInstanceOf[Int])
  }

  @Test
  def castDoubleToInt: Unit = {
    assertTrue(castToInt(double).isInstanceOf[Int])
  }

  @Test
  def castIntegerToInt: Unit = {
    assertTrue(castToInt(integer).isInstanceOf[Int])
  }

  @Test
  def castStringToInt: Unit = {
    assertTrue(castToInt(string).isInstanceOf[Int])
  }

  @Test
  def castDefaultToInt: Unit = {
    val int: Int = 8754
    assertTrue(castToInt(int).isInstanceOf[Int])
  }
}
