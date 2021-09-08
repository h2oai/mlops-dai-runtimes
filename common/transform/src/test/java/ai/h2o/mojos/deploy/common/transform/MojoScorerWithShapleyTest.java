package ai.h2o.mojos.deploy.common.transform;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
public class MojoScorerWithShapleyTest {
  @InjectMocks
  private MojoScorer scorer;

  @Mock
  private ScoreRequestToMojoFrameConverter scoreRequestConverter;
  @Mock private MojoFrameToScoreResponseConverter scoreResponseConverter;
  @Mock private MojoFrameToContributionResponseConverter contributionResponseConverter;
  @Mock private ContributionRequestToMojoFrameConverter contributionRequestConverter;
  private static String oldValue;
  MockitoSession mockito;

  /**
  * Verify that the request is valid and matches the expected.
  *
  */
  @Before
  public void setup1() {
    mockito = Mockito.mockitoSession()
            .initMocks(this)
            .strictness(Strictness.STRICT_STUBS)
            .startMocking();
  }

  /**
   * Verify that the request is valid and matches the expected.
   *
   */
  @After
  public void clear() {
    mockito.finishMocking();
  }

  @BeforeAll
  static void setup2() {
    System.setProperty("mojo.path", "src/test/resources/multinomial-pipeline.mojo");
    // enable shapley
    oldValue = System.setProperty("shapley.enable", "true");
  }

  @AfterAll
  static void clear2() {
    if (oldValue == null) {
      System.clearProperty("shapley.enable");
    } else {
      System.setProperty("shapley.enable", oldValue);
    }
  }

  @Test
  void verifyScoreRequestWithShapley_succeeds() {
    ScoreRequest request = new ScoreRequest();
    request.addFieldsItem("field1");
    request.addRowsItem(toRow("text"));
    request.setRequestShapleyValueType(ShapleyType.TRANSFORMED);
    given(scoreRequestConverter.apply(any(), any()))
            .willReturn(generateDummyTransformedMojoFrame());
    given(scoreResponseConverter.apply(any(), any())).willReturn(generateDummyResponse());

    // When
    scorer.score(request);

    // Then all ok
  }

  @Test
  void verifyContributionRequestWithOriginalShapley_fails() {
    ContributionRequest request = new ContributionRequest();
    request.addFieldsItem("field1");
    request.addRowsItem(toRow("text"));
    request.setRequestShapleyValueType(ShapleyType.ORIGINAL);

    // When & Then
    assertThrows(UnsupportedOperationException.class, () -> scorer.computeContribution(request));
  }

  @Test
  void verifyContributionRequestWithTransformedShapley_succeeds() {
    ContributionRequest request = new ContributionRequest();
    request.addFieldsItem("field1");
    request.addRowsItem(toRow("text"));
    request.setRequestShapleyValueType(ShapleyType.TRANSFORMED);
    given(contributionRequestConverter.apply(any(), any()))
            .willReturn(generateDummyTransformedMojoFrame());

    // When
    scorer.computeContribution(request);

    // Then
    verify(contributionResponseConverter)
            .contributionResponseWithOutputGroup(any(), any());
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
    final MojoFrameBuilder frameBuilder
            = new MojoFrameBuilder(meta, Collections.emptyList(), Collections.emptyMap());
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
