package ai.h2o.mojos.deploy.common.transform;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.runtime.api.MojoColumnMeta;
import ai.h2o.mojos.runtime.frame.MojoColumn;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SampleRequestBuilderTest {

  private final SampleRequestBuilder builder = new SampleRequestBuilder();

  @Test
  void build_EmptyMeta() {
    // Given
    final MojoFrameMeta inputMeta = new MojoFrameMeta(Collections.emptyList());

    // When
    ScoreRequest result = builder.build(inputMeta);

    // Then
    ScoreRequest expected = new ScoreRequest();
    expected.fields(emptyList());
    expected.addRowsItem(asRow());
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void build_OneField() {
    // Given
    MojoFrameMeta inputMeta =
        new MojoFrameMeta(
            Collections.singletonList(MojoColumnMeta.newOutput("field", MojoColumn.Type.Str)));

    // When
    ScoreRequest result = builder.build(inputMeta);

    // Then
    ScoreRequest expected = new ScoreRequest();
    expected.addFieldsItem("field");
    expected.addRowsItem(asRow("text"));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void build_AllTypes() {
    // Given
    MojoColumn.Type[] types = {
      MojoColumn.Type.Str,
      MojoColumn.Type.Float32,
      MojoColumn.Type.Float64,
      MojoColumn.Type.Bool,
      MojoColumn.Type.Int32,
      MojoColumn.Type.Int64,
      MojoColumn.Type.Time64
    };
    String[] columns = Stream.of(types).map(Object::toString).toArray(String[]::new);
    final MojoFrameMeta inputMeta =
        new MojoFrameMeta(
            Stream.of(types)
                .map(t -> MojoColumnMeta.newInput(t.toString(), t))
                .collect(Collectors.toList()));

    // When
    ScoreRequest result = builder.build(inputMeta);

    // Then
    ScoreRequest expected = new ScoreRequest();
    expected.fields(asList(columns));
    expected.addRowsItem(asRow("text", "0", "0", "true", "0", "0", "2018-01-01"));
    assertThat(result).isEqualTo(expected);
  }

  private static Row asRow(String... values) {
    Row row = new Row();
    row.addAll(asList(values));
    return row;
  }
}
