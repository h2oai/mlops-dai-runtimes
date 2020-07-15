package ai.h2o.mojos.deploy.common.transform;

import static ai.h2o.mojos.runtime.frame.MojoColumn.Type.Bool;
import static ai.h2o.mojos.runtime.frame.MojoColumn.Type.Float32;
import static ai.h2o.mojos.runtime.frame.MojoColumn.Type.Float64;
import static ai.h2o.mojos.runtime.frame.MojoColumn.Type.Int32;
import static ai.h2o.mojos.runtime.frame.MojoColumn.Type.Int64;
import static ai.h2o.mojos.runtime.frame.MojoColumn.Type.Str;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.runtime.frame.MojoColumn.Type;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MojoFrameToResponseConverterTest {
  private final MojoFrameToResponseConverter converter = new MojoFrameToResponseConverter();

  @Test
  void convertEmptyRowsResponse_succeeds() {
    // Given
    ScoreRequest scoreRequest = new ScoreRequest();
    MojoFrame mojoFrame = new MojoFrameBuilder(MojoFrameMeta.getEmpty()).toMojoFrame();

    // When
    ScoreResponse result = converter.apply(mojoFrame, scoreRequest);

    // Then
    assertThat(result.getScore()).isEmpty();
    assertThat(result.getFields()).isEmpty();
  }

  @Test
  void convertSingleFieldResponse_succeeds() {
    // Given
    String[] fields = {"field"};
    Type[] types = {Str};
    String[][] values = {{"value"}};
    ScoreRequest scoreRequest = new ScoreRequest();

    // When
    ScoreResponse result = converter.apply(buildMojoFrame(fields, types, values), scoreRequest);

    // Then
    assertThat(result.getScore()).containsExactly(asRow("value"));
    assertThat(result.getFields()).containsExactly("field");
  }

  @Test
  void convertSingleFieldResponse_withoutFieldNames_succeeds() {
    // Given
    String[] fields = {"field"};
    Type[] types = {Str};
    String[][] values = {{"value"}};
    ScoreRequest scoreRequest = new ScoreRequest();
    scoreRequest.setNoFieldNamesInOutput(true);

    // When
    ScoreResponse result = converter.apply(buildMojoFrame(fields, types, values), scoreRequest);

    // Then
    assertThat(result.getScore()).containsExactly(asRow("value"));
    assertThat(result.getFields()).isNull();
  }

  @Test
  void convertIncludesOneField_succeeds() {
    // Given
    String[] fields = {"outputField"};
    Type[] types = {Str};
    String[][] values = {{"outputValue"}};
    ScoreRequest scoreRequest = new ScoreRequest();
    scoreRequest.addFieldsItem("inputField");
    scoreRequest.addIncludeFieldsInOutputItem("inputField");
    scoreRequest.addRowsItem(asRow("inputValue"));

    // When
    ScoreResponse result = converter.apply(buildMojoFrame(fields, types, values), scoreRequest);

    // Then
    assertThat(result.getScore()).containsExactly(asRow("inputValue", "outputValue"));
    assertThat(result.getFields()).containsExactly("inputField", "outputField").inOrder();
  }

  @Test
  void convertIncludesSomeFields_succeeds() {
    // Given
    String[] fields = {"outputField1", "outputField2"};
    Type[] types = {Str, Str};
    String[][] values = {{"outputValue1", "outputValue2"}};
    ScoreRequest scoreRequest = new ScoreRequest();
    scoreRequest.setFields(asList("inputField1", "inputField2", "inputField3"));
    scoreRequest.setIncludeFieldsInOutput(asList("inputField1", "inputField3"));
    scoreRequest.addRowsItem(asRow("inputValue1", "omittedValue", "inputValue3"));

    // When
    ScoreResponse result = converter.apply(buildMojoFrame(fields, types, values), scoreRequest);

    // Then
    assertThat(result.getScore())
        .containsExactly(asRow("inputValue1", "inputValue3", "outputValue1", "outputValue2"));
    assertThat(result.getFields())
        .containsExactly("inputField1", "inputField3", "outputField1", "outputField2")
        .inOrder();
  }

  @Test
  void convertIncludePresentIdField_succeeds() {
    // Given
    String[] fields = {"outputField"};
    Type[] types = {Str};
    String[][] values = {{"outputValue"}};
    ScoreRequest scoreRequest = new ScoreRequest();
    scoreRequest.setFields(asList("inputField", "omittedField", "id"));
    scoreRequest.setIncludeFieldsInOutput(asList("inputField", "id"));
    scoreRequest.addRowsItem(asRow("inputValue", "omittedValue", "testId"));
    scoreRequest.setIdField("id");

    // When
    ScoreResponse result = converter.apply(buildMojoFrame(fields, types, values), scoreRequest);

    // Then
    assertThat(result.getScore()).containsExactly(asRow("inputValue", "testId", "outputValue"));
    assertThat(result.getFields()).containsExactly("inputField", "id", "outputField").inOrder();
  }

  @Test
  void convertIncludeMissingIdField_generateUuid() {
    // Given
    String[] fields = {"outputField"};
    Type[] types = {Str};
    String[][] values = {{"outputValue"}};
    ScoreRequest scoreRequest = new ScoreRequest();
    scoreRequest.setFields(asList("inputField", "omittedField"));
    scoreRequest.setIncludeFieldsInOutput(asList("inputField", "id"));
    scoreRequest.addRowsItem(asRow("inputValue", "omittedValue", "testId"));
    scoreRequest.setIdField("id");

    // When
    ScoreResponse result = converter.apply(buildMojoFrame(fields, types, values), scoreRequest);

    // Then
    assertThat(result.getScore()).hasSize(1);
    assertThat(result.getScore().get(0)).hasSize(3);
    assertThat(result.getScore().get(0).get(0)).isEqualTo("inputValue");
    assertThat(result.getScore().get(0).get(1))
        .matches(
            "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}");
    assertThat(result.getScore().get(0).get(2)).isEqualTo("outputValue");
    assertThat(result.getFields()).containsExactly("inputField", "id", "outputField").inOrder();
  }

  @Test
  void convertMoreRowsResponse_succeeds() {
    // Given
    String[] fields = {"field"};
    Type[] types = {Str};
    String[][] values = {{"value1"}, {"value2"}, {"value3"}};
    ScoreRequest scoreRequest = new ScoreRequest();

    // When
    ScoreResponse result = converter.apply(buildMojoFrame(fields, types, values), scoreRequest);

    // Then
    assertThat(result.getScore())
        .containsExactly(Stream.of(values).map(MojoFrameToResponseConverterTest::asRow).toArray())
        .inOrder();
    assertThat(result.getFields()).containsExactly("field");
  }

  @ParameterizedTest
  @MethodSource("provideValues_convertMoreTypesResponse_succeeds")
  void convertMoreTypesResponse_succeeds(String[][] values) {
    // Given
    Type[] types = {Str, Float32, Float64, Bool, Int32, Int64};
    ScoreRequest scoreRequest = new ScoreRequest();

    // When
    ScoreResponse result =
        converter.apply(
            buildMojoFrame(
                Stream.of(types).map(Object::toString).toArray(String[]::new), types, values),
            scoreRequest);

    // Then
    assertThat(result.getScore())
        .containsExactly(Stream.of(values).map(MojoFrameToResponseConverterTest::asRow).toArray());
    assertThat(result.getFields())
        .containsExactly("Str", "Float32", "Float64", "Bool", "Int32", "Int64")
        .inOrder();
  }

  @SuppressWarnings("unused")
  private static Stream<Arguments> provideValues_convertMoreTypesResponse_succeeds() {
    return Stream.of(
        Arguments.of((Object) new String[][] {{"str", "1.1", "2.2", "1", "123", "123456789"}}),
        Arguments.of((Object) new String[][] {{null, null, null, null, null, null}}));
  }

  @ParameterizedTest
  @MethodSource("provideValues_convertMoreTypesResponse_actualValues_succeeds")
  void convertMoreTypesResponse_actualValues_succeeds(Object[][] values, String[][] expValues) {
    // Given
    Type[] types = {Str, Float32, Float64, Bool, Int32, Int64};
    ScoreRequest scoreRequest = new ScoreRequest();

    // When
    ScoreResponse result =
        converter.apply(
            buildMojoFrame(
                Stream.of(types).map(Object::toString).toArray(String[]::new),
                types,
                values,
                (rb, type, col, value) -> setJavaValue(rb, type, col, value)),
            scoreRequest);

    // Then
    assertThat(result.getScore())
        .containsExactly(
            Stream.of(expValues).map(MojoFrameToResponseConverterTest::asRow).toArray());
    assertThat(result.getFields())
        .containsExactly("Str", "Float32", "Float64", "Bool", "Int32", "Int64")
        .inOrder();
  }

  @SuppressWarnings("unused")
  private static Stream<Arguments> provideValues_convertMoreTypesResponse_actualValues_succeeds() {
    return Stream.of(
        Arguments.of(
            aao("foo", -1.0f, -2.0, true, 1, 2L), aas("foo", "-1.0", "-2.0", "1", "1", "2")),
        Arguments.of(
            aao(null, Float.NaN, Double.NaN, null, null, null),
            aas(null, null, null, null, null, null)),
        Arguments.of(
            aao(
                null,
                Float.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                null,
                Integer.MIN_VALUE,
                Long.MIN_VALUE),
            aas(null, "-Infinity", "-Infinity", null, null, null)),
        Arguments.of(
            aao(
                null,
                Float.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                null,
                Integer.MAX_VALUE,
                Long.MAX_VALUE),
            aas(null, "Infinity", "Infinity", null, "2147483647", "9223372036854775807")));
  }

  private static MojoFrame buildMojoFrame(String[] fields, Type[] types, String[][] values) {
    return buildMojoFrame(fields, types, values, (rb, type, col, value) -> rb.setValue(col, value));
  }

  private static <T> MojoFrame buildMojoFrame(
      String[] fields, Type[] types, T[][] values, RowBuilderSetter<T> setter) {
    MojoFrameMeta meta = new MojoFrameMeta(fields, types);
    MojoFrameBuilder frameBuilder = new MojoFrameBuilder(meta);
    for (T[] row : values) {
      MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();
      int col = 0;
      for (T value : row) {
        Type type = types[col];
        setter.setValue(rowBuilder, type, col++, value);
      }
      frameBuilder.addRow(rowBuilder);
    }
    return frameBuilder.toMojoFrame();
  }

  private static Row asRow(String... values) {
    Row row = new Row();
    row.ensureCapacity(values.length);
    row.addAll(asList(values));
    return row;
  }

  private static Object[][] aao(Object... values) {
    return new Object[][] {values};
  }

  private static String[][] aas(String... values) {
    return new String[][] {values};
  }

  private static MojoRowBuilder setJavaValue(MojoRowBuilder rb, Type type, int col, Object value) {
    switch (type) {
      case Bool:
        return rb.setBool(col, (Boolean) value);
      case Str:
        return rb.setString(col, (String) value);
      case Int32:
        return rb.setInt(col, (Integer) value);
      case Int64:
        return rb.setLong(col, (Long) value);
      case Float32:
        return rb.setFloat(col, (Float) value);
      case Float64:
        return rb.setDouble(col, (Double) value);
      default:
        throw new IllegalArgumentException("Unsupported type " + type);
    }
  }

  @FunctionalInterface
  interface RowBuilderSetter<T> {
    void setValue(MojoRowBuilder rb, Type colType, int col, T value);
  }
}
