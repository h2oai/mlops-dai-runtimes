package ai.h2o.mojos.deploy.common.transform;

import static ai.h2o.mojos.runtime.frame.MojoColumn.Type.Bool;
import static ai.h2o.mojos.runtime.frame.MojoColumn.Type.Float32;
import static ai.h2o.mojos.runtime.frame.MojoColumn.Type.Float64;
import static ai.h2o.mojos.runtime.frame.MojoColumn.Type.Int32;
import static ai.h2o.mojos.runtime.frame.MojoColumn.Type.Int64;
import static ai.h2o.mojos.runtime.frame.MojoColumn.Type.Str;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import ai.h2o.mojos.deploy.common.rest.model.ContributionResponse;
import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.runtime.api.MojoColumnMeta;
import ai.h2o.mojos.runtime.frame.MojoColumn;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MojoFrameToContributionResponseConverterTest {
  private final MojoFrameToContributionResponseConverter converter
            = new MojoFrameToContributionResponseConverter();

  @Test
  void convertEmptyRowsResponse_succeeds() {
    // Given
    MojoFrame mojoFrame = new MojoFrameBuilder(
          MojoFrameMeta.getEmpty(), Collections.emptyList(), Collections.emptyMap())
                        .toMojoFrame();

    // When
    ContributionResponse result = converter.contributionResponseWithNoOutputGroup(mojoFrame);

    // Then
    assertThat(result.getContributionByOutputGroup().size()).isEqualTo(1);
    assertThat(result.getContributionByOutputGroup().get(0).getContributions()).isEmpty();
    assertThat(result.getContributionByOutputGroup().get(0).getOutputGroup()).isNull();
    assertThat(result.getFeatures()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("provideValues_convertMoreTypesResponse_succeeds")
  void convertMoreTypesResponse_succeeds(String[][] contributions) {
    // Given
    MojoColumn.Type[] types = {Str, Float32, Float64, Bool, Int32, Int64};

    // When
    ContributionResponse result = converter.contributionResponseWithNoOutputGroup(buildMojoFrame(
        Stream.of(types).map(Object::toString).toArray(String[]::new), types, contributions));

    // Then
    assertThat(result.getContributionByOutputGroup().size()).isEqualTo(1);
    assertThat(result.getContributionByOutputGroup().get(0).getContributions())
        .containsExactly(Stream.of(contributions)
        .map(MojoFrameToContributionResponseConverterTest::asRow).toArray());
    assertThat(result.getFeatures())
     .containsExactly("Str", "Float32", "Float64", "Bool", "Int32", "Int64")
        .inOrder();
  }

  @SuppressWarnings("unused")
  private static Stream<Arguments> provideValues_convertMoreTypesResponse_succeeds() {
    return Stream.of(
       Arguments.of((Object) new String[][] {{"str", "1.1", "2.2", "1", "123", "123456789"}}),
       Arguments.of((Object) new String[][] {{null, null, null, null, null, null}}));
  }

  @Test
  void convertEmptyRowsResponse_withEmptyOutputGroup_succeeds() {
    // Given
    MojoFrame mojoFrame = new MojoFrameBuilder(
        MojoFrameMeta.getEmpty(), Collections.emptyList(), Collections.emptyMap())
            .toMojoFrame();

    List<String> outputGroupNames = new ArrayList<>();

    // When
    ContributionResponse result = converter
            .contributionResponseWithOutputGroup(mojoFrame, outputGroupNames);

    // Then
    assertThat(result.getContributionByOutputGroup().size()).isEqualTo(0);
    assertThat(result.getFeatures()).isEmpty();
  }

  @Test
  void convertSingleFeatureResponse_succeeds() {
    // Given
    String[] features = {"feature"};
    MojoColumn.Type[] types = {Float32};
    String[][] contributions = {{"23.6"}};

    // When
    ContributionResponse result = converter
            .contributionResponseWithNoOutputGroup(buildMojoFrame(features, types, contributions));

    // Then
    assertThat(result.getContributionByOutputGroup().size()).isEqualTo(1);
    assertThat(result.getContributionByOutputGroup().get(0).getContributions().size()).isEqualTo(1);
    assertThat(result.getContributionByOutputGroup().get(0).getContributions())
            .containsExactly(asRow("23.6"));
    assertThat(result.getContributionByOutputGroup().get(0).getOutputGroup()).isNull();
    assertThat(result.getFeatures()).containsExactly("feature");
  }

  @Test
  void convertSingleFeatureResponse_withOneOutputGroup_succeeds() {
    // Given
    String[] features = {"feature.test"};
    MojoColumn.Type[] types = {Float32};
    String[][] contributions = {{"122.2"}};

    List<String> outputGroupNames = Collections.singletonList("test");

    // When
    ContributionResponse result = converter
            .contributionResponseWithOutputGroup(
                    buildMojoFrame(features, types, contributions), outputGroupNames);

    // Then
    assertThat(result.getContributionByOutputGroup().size()).isEqualTo(1);
    assertThat(result.getContributionByOutputGroup().get(0).getContributions().size()).isEqualTo(1);
    assertThat(result.getContributionByOutputGroup().get(0).getContributions())
            .containsExactly(asRow("122.2"));
    assertThat(result.getContributionByOutputGroup().get(0).getOutputGroup()).isEqualTo("test");
    assertThat(result.getFeatures()).containsExactly("feature");
  }

  @Test
  void convertSingleFeatureResponse_withManyOutputGroup_succeeds() {
    // Given
    String[] features = {"feature.test1", "feature.test2"};
    MojoColumn.Type[] types = {Float32, Float32};
    String[][] contributions = {{"122.2", "34.6"}};
    List<String> outputGroupNames = Arrays.asList("test1", "test2");

    // When
    ContributionResponse result = converter.contributionResponseWithOutputGroup(
            buildMojoFrame(features, types, contributions), outputGroupNames);

    // Then
    assertThat(result.getContributionByOutputGroup().size()).isEqualTo(2);

    assertThat(result.getContributionByOutputGroup().get(0).getContributions().size()).isEqualTo(1);
    assertThat(result.getContributionByOutputGroup().get(0).getContributions())
            .containsExactly(asRow("122.2"));
    assertThat(result.getContributionByOutputGroup().get(0).getOutputGroup()).isEqualTo("test1");

    assertThat(result.getContributionByOutputGroup().get(1).getContributions().size()).isEqualTo(1);
    assertThat(result.getContributionByOutputGroup().get(1).getContributions())
            .containsExactly(asRow("34.6"));
    assertThat(result.getContributionByOutputGroup().get(1).getOutputGroup()).isEqualTo("test2");

    assertThat(result.getFeatures()).containsExactly("feature");
  }

  @Test
  void convertSingleFeatureResponse_withManyRowsAndOutputGroups_succeeds() {
    // Given
    String[] features = {"feature.test1", "feature.test2"};
    MojoColumn.Type[] types = {Float32, Float32};
    String[][] contributions = {{"122.2", "34.6"}, {"90.2", "45.6"}};

    List<String> outputGroupNames = Arrays.asList("test1", "test2");

    // When
    ContributionResponse result = converter.contributionResponseWithOutputGroup(
            buildMojoFrame(features, types, contributions), outputGroupNames);

    // Then
    assertThat(result.getContributionByOutputGroup().size()).isEqualTo(2);

    assertThat(result.getContributionByOutputGroup().get(0).getContributions().size()).isEqualTo(2);
    assertThat(result.getContributionByOutputGroup().get(0).getContributions())
            .containsExactly(asRow("122.2"), asRow("90.2"));
    assertThat(result.getContributionByOutputGroup().get(0).getOutputGroup()).isEqualTo("test1");

    assertThat(result.getContributionByOutputGroup().get(1).getContributions().size()).isEqualTo(2);
    assertThat(result.getContributionByOutputGroup().get(1).getContributions())
            .containsExactly(asRow("34.6"), asRow("45.6"));
    assertThat(result.getContributionByOutputGroup().get(1).getOutputGroup()).isEqualTo("test2");

    assertThat(result.getFeatures()).containsExactly("feature");
  }

  @Test
  void convertSingleFeatureResponse_withManyFeaturesAndOutputGroups_succeeds() {
    // Given
    String[] features = {"feature1.test1", "feature1.test2", "feature2.test1", "feature2.test2"};
    MojoColumn.Type[] types = {Float32, Float32, Float32, Float32};
    String[][] contributions = {
            {"122.2", "34.6", "90.9", "78.0"},
            {"90.2", "45.6", "56.9", "56.0"}};

    List<String> outputGroupNames = Arrays.asList("test1", "test2");

    // When
    ContributionResponse result = converter
            .contributionResponseWithOutputGroup(
                    buildMojoFrame(features, types, contributions), outputGroupNames);

    // Then
    assertThat(result.getContributionByOutputGroup().size()).isEqualTo(2);

    assertThat(result.getContributionByOutputGroup().get(0).getContributions().size()).isEqualTo(2);
    assertThat(result.getContributionByOutputGroup().get(0).getContributions())
            .containsExactly(asRow("122.2", "90.9"), asRow("90.2", "56.9"));
    assertThat(result.getContributionByOutputGroup().get(0).getOutputGroup()).isEqualTo("test1");

    assertThat(result.getContributionByOutputGroup().get(1).getContributions().size()).isEqualTo(2);
    assertThat(result.getContributionByOutputGroup().get(1).getContributions())
            .containsExactly(asRow("34.6", "78.0"), asRow("45.6", "56.0"));
    assertThat(result.getContributionByOutputGroup().get(1).getOutputGroup()).isEqualTo("test2");

    assertThat(result.getFeatures()).containsExactly("feature1", "feature2");
  }

  private static MojoFrame buildMojoFrame(String[] fields,
                                          MojoColumn.Type[] types,
                                          String[][] values) {
    return buildMojoFrame(fields, types, values, (rb, type, col, value) -> rb.setValue(col, value));
  }

  private static <T> MojoFrame buildMojoFrame(
        String[] fields, MojoColumn.Type[] types,
        T[][] values,
        MojoFrameToScoreResponseConverterTest.RowBuilderSetter<T> setter) {
    final List<MojoColumnMeta> columns = MojoColumnMeta.toColumns(
            fields,
            types,
            MojoColumn.Kind.Output);
    final MojoFrameMeta meta = new MojoFrameMeta(columns);
    final MojoFrameBuilder frameBuilder =
            new MojoFrameBuilder(meta, Collections.emptyList(), Collections.emptyMap());
    for (T[] row : values) {
      MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();
      int col = 0;
      for (T value : row) {
        MojoColumn.Type type = types[col];
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
}
