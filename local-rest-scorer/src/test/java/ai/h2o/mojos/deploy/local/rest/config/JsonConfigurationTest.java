package ai.h2o.mojos.deploy.local.rest.config;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.h2o.mojos.deploy.common.rest.model.Row;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@JsonTest
@ContextConfiguration(classes = {JsonConfiguration.class})
class JsonConfigurationTest {

  @Autowired
  @SuppressWarnings("unused")
  private ObjectMapper objectMapper;

  private ObjectMapper defaultObjectMapper = new ObjectMapper();

  @ParameterizedTest
  @MethodSource
  public void objectMapperShouldHandleMixedValueRows(String givenRowJson, Row expRow)
      throws IOException {
    // When
    Row row = objectMapper.readValue(givenRowJson, Row.class);

    // Then
    assertThat(row).containsExactlyElementsIn(expRow);
  }

  @SuppressWarnings("unused")
  private static Stream<Arguments> objectMapperShouldHandleMixedValueRows() {
    return Stream.concat(
        Stream.of(
            Arguments.arguments("[1,2,3]", toRow("1", "2", "3")),
            Arguments.arguments("[1,\"NA\",3]", toRow("1", "NA", "3")),
            Arguments.arguments("[1,\"null\",3]", toRow("1", "null", "3")),
            Arguments.arguments("[]", toRow()),
            Arguments.arguments("[null]", toRow((String) null)),
            Arguments.arguments("[1,\"NaN\",3]", toRow("1", "NaN", "3")),
            Arguments.arguments("[0,1,2,\"Infinity\"]", toRow("0", "1", "2", "Infinity"))),
        defaultObjectMapperShouldFailOnNonNumericRows());
  }

  @ParameterizedTest
  @MethodSource
  public void defaultObjectMapperShouldFailOnNonNumericRows(
      String givenRowJson, @SuppressWarnings("unused") Row expRow) {
    // When & Then
    assertThrows(
        JsonMappingException.class, () -> defaultObjectMapper.readValue(givenRowJson, Row.class));
  }

  @SuppressWarnings("unused")
  private static Stream<Arguments> defaultObjectMapperShouldFailOnNonNumericRows() {
    return Stream.of(
        Arguments.arguments("[1,NaN,3]", toRow("1", "NaN", "3")),
        Arguments.arguments("[Infinity]", toRow("Infinity")),
        Arguments.arguments("[-Infinity, -2, -1, 0.0]", toRow("-Infinity", "-2", "-1", "0.0")));
  }

  private static Row toRow(String... values) {
    Row row = new Row();
    row.addAll(Arrays.asList(values));
    return row;
  }
}
