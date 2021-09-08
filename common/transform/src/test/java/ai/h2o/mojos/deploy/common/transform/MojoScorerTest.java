package ai.h2o.mojos.deploy.common.transform;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import ai.h2o.mojos.deploy.common.rest.model.ContributionRequest;
import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.deploy.common.rest.model.ShapleyType;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MojoScorerTest {
  private static final String MOJO_PIPELINE_PATH = "src/test/resources/multinomial-pipeline.mojo";

  @Mock private ScoreRequestToMojoFrameConverter scoreRequestConverter;
  @Mock private MojoFrameToScoreResponseConverter scoreResponseConverter;
  @Mock private MojoFrameToContributionResponseConverter contributionResponseConverter;
  @Mock private ContributionRequestToMojoFrameConverter contributionRequestConverter;
  @Mock private MojoPipelineToModelInfoConverter modelInfoConverter;
  @Mock private CsvToMojoFrameConverter csvConverter;

  @InjectMocks
  private MojoScorer scorer;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @BeforeAll
  static void setup() {
    System.setProperty("mojo.path", MOJO_PIPELINE_PATH);
    // disable shapley
    System.setProperty("shapley.enable", "false");
  }

  @AfterAll
  static void clear() {
    // reset properties
    System.clearProperty("mojo.path");
    System.clearProperty("shapley.enable");
  }

  @Test
  void verifyScoreRequestWithoutShapley_succeeds() {
    // Given
    ScoreRequest request = new ScoreRequest();
    request.addFieldsItem("field1");
    request.addRowsItem(toRow("text"));
    MojoFrame dummyMojoFrame = generateDummyTransformedMojoFrame();
    given(scoreRequestConverter.apply(any(), any()))
            .willReturn(dummyMojoFrame);
    ScoreResponse dummyResponse = generateDummyResponse();
    given(scoreResponseConverter.apply(any(), any()))
            .willReturn(dummyResponse);

    // When
    scorer.score(request);

    // Then all ok
  }

  @Test
  void verifyScoreRequestWithTransformedShapley_fails() {
    ScoreRequest request = new ScoreRequest();
    request.addFieldsItem("field1");
    request.addRowsItem(toRow("text"));
    request.setRequestShapleyValueType(ShapleyType.TRANSFORMED);
    MojoFrame dummyMojoFrame = generateDummyTransformedMojoFrame();
    given(scoreRequestConverter.apply(any(), any()))
            .willReturn(dummyMojoFrame);
    ScoreResponse dummyResponse = generateDummyResponse();
    given(scoreResponseConverter.apply(any(), any()))
            .willReturn(dummyResponse);

    // When & Then
    assertThrows(
            IllegalArgumentException.class, () -> scorer.score(request));
  }

  @Test
  void verifyScoreRequestWithOriginalShapley_fails() {
    ScoreRequest request = new ScoreRequest();
    request.addFieldsItem("field1");
    request.addRowsItem(toRow("text"));
    request.setRequestShapleyValueType(ShapleyType.TRANSFORMED);
    MojoFrame dummyMojoFrame = generateDummyTransformedMojoFrame();
    given(scoreRequestConverter.apply(any(), any()))
            .willReturn(dummyMojoFrame);
    ScoreResponse dummyResponse = generateDummyResponse();
    given(scoreResponseConverter.apply(any(), any()))
            .willReturn(dummyResponse);

    // When & Then
    assertThrows(
            IllegalArgumentException.class, () -> scorer.score(request));
  }

  @Test
  void verifyContributionWithOriginalShapley_fails() {
    ContributionRequest request = new ContributionRequest();
    request.addFieldsItem("field1");
    request.addRowsItem(toRow("text"));
    request.setRequestShapleyValueType(ShapleyType.ORIGINAL);

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> scorer
            .computeContribution(request));
  }

  @Test
  void verifyContributionWithTransformedShapley_fails() {
    ContributionRequest request = new ContributionRequest();
    request.addFieldsItem("field1");
    request.addRowsItem(toRow("text"));
    request.setRequestShapleyValueType(ShapleyType.TRANSFORMED);

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> scorer
            .computeContribution(request));
  }

  private static Row toRow(String... values) {
    Row row = new Row();
    row.addAll(Arrays.asList(values));
    return row;
  }

  private MojoFrame generateDummyTransformedMojoFrame() {
    final List<MojoColumnMeta> columns = new ArrayList<>();
    columns.add(MojoColumnMeta.newOutput("Prediction.0", MojoColumn.Type.Float64));
    final MojoFrameMeta meta = new MojoFrameMeta(columns);
    final MojoFrameBuilder frameBuilder =
            new MojoFrameBuilder(meta, Collections.emptyList(), Collections.emptyMap());
    MojoRowBuilder mojoRowBuilder = frameBuilder.getMojoRowBuilder();
    mojoRowBuilder.setValue("Prediction.0", "0.64");
    frameBuilder.addRow(mojoRowBuilder);
    mojoRowBuilder = frameBuilder.getMojoRowBuilder();
    mojoRowBuilder.setValue("Prediction.0", "0.13");
    frameBuilder.addRow(mojoRowBuilder);
    return frameBuilder.toMojoFrame();
  }

  private ScoreResponse generateDummyResponse() {
    ScoreResponse response = new ScoreResponse();
    List<Row> outputRows =
            Stream.generate(Row::new).limit(4).collect(Collectors.toList());
    response.setScore(outputRows);
    response.setFields(Arrays.asList("field1"));
    return response;
  }
}
