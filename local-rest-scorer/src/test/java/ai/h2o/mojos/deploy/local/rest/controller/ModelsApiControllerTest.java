package ai.h2o.mojos.deploy.local.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.h2o.mojos.deploy.common.rest.model.CapabilityType;
import ai.h2o.mojos.deploy.common.transform.MojoScorer;
import ai.h2o.mojos.deploy.common.transform.SampleRequestBuilder;
import ai.h2o.mojos.deploy.common.transform.ShapleyLoadOption;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.api.BasePipelineListener;
import ai.h2o.mojos.runtime.api.MojoPipelineService;
import ai.h2o.mojos.runtime.frame.MojoColumn;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ModelsApiControllerTest {
  private static final String MOJO_PIPELINE_PATH = "src/test/resources/multinomial-pipeline.mojo";
  private static final String TEST_UUID = "TEST_UUID";

  @Mock private SampleRequestBuilder sampleRequestBuilder;

  @BeforeAll
  static void setup() {
    System.setProperty("mojo.path", "src/test/resources/multinomial-pipeline.mojo");
    mockDummyPipeline();
  }

  private static void mockDummyPipeline() {
    MojoPipeline dummyPipeline =
        new DummyPipeline(TEST_UUID, MojoFrameMeta.getEmpty(), MojoFrameMeta.getEmpty());
    MockedStatic<MojoPipelineService> theMock = Mockito.mockStatic(MojoPipelineService.class);
    theMock.when(() -> MojoPipelineService
        .loadPipeline(new File(MOJO_PIPELINE_PATH))).thenReturn(dummyPipeline);
  }

  @Test
  void verifyCapabilities_DefaultShapley_ReturnsExpected() {
    // Given
    List<CapabilityType> expectedCapabilities = Arrays.asList(CapabilityType.SCORE);

    MojoScorer scorer = mock(MojoScorer.class);
    when(scorer.getEnabledShapleyTypes()).thenReturn(ShapleyLoadOption.NONE);

    ModelsApiController controller = new ModelsApiController(scorer, sampleRequestBuilder);

    // When
    ResponseEntity<List<CapabilityType>> response = controller.getCapabilities();

    // Then
    assertEquals(expectedCapabilities, response.getBody());
  }

  @Test
  void verifyCapabilities_AllShapleyEnabled_ReturnsExpected() {
    // Given
    List<CapabilityType> expectedCapabilities = Arrays.asList(
        CapabilityType.SCORE,
        CapabilityType.CONTRIBUTION_ORIGINAL,
        CapabilityType.CONTRIBUTION_TRANSFORMED);
    MojoScorer scorer = mock(MojoScorer.class);
    when(scorer.getEnabledShapleyTypes()).thenReturn(ShapleyLoadOption.ALL);

    ModelsApiController controller = new ModelsApiController(scorer, sampleRequestBuilder);

    // When
    ResponseEntity<List<CapabilityType>> response = controller.getCapabilities();

    // Then
    assertEquals(expectedCapabilities, response.getBody());
  }

  @Test
  void verifyCapabilities_OriginalShapleyEnabled_ReturnsExpected() {
    // Given
    List<CapabilityType> expectedCapabilities = Arrays.asList(
        CapabilityType.SCORE,
        CapabilityType.CONTRIBUTION_ORIGINAL);
    MojoScorer scorer = mock(MojoScorer.class);
    when(scorer.getEnabledShapleyTypes()).thenReturn(ShapleyLoadOption.ORIGINAL);

    ModelsApiController controller = new ModelsApiController(scorer, sampleRequestBuilder);

    // When
    ResponseEntity<List<CapabilityType>> response = controller.getCapabilities();

    // Then
    assertEquals(expectedCapabilities, response.getBody());
  }

  @Test
  void verifyCapabilities_TransformedShapleyEnabled_ReturnsExpected() {
    // Given
    List<CapabilityType> expectedCapabilities = Arrays.asList(
        CapabilityType.SCORE,
        CapabilityType.CONTRIBUTION_TRANSFORMED);
    MojoScorer scorer = mock(MojoScorer.class);
    when(scorer.getEnabledShapleyTypes()).thenReturn(ShapleyLoadOption.TRANSFORMED);

    ModelsApiController controller = new ModelsApiController(scorer, sampleRequestBuilder);

    // When
    ResponseEntity<List<CapabilityType>> response = controller.getCapabilities();

    // Then
    assertEquals(expectedCapabilities, response.getBody());
  }

  /** Dummy pipeline {@link MojoPipeline} just to mock the static methods used inside scoring. */
  private static class DummyPipeline extends MojoPipeline {
    private final MojoFrameMeta inputMeta;
    private final MojoFrameMeta outputMeta;

    private DummyPipeline(String uuid, MojoFrameMeta inputMeta, MojoFrameMeta outputMeta) {
      super(uuid, null, null);
      this.inputMeta = inputMeta;
      this.outputMeta = outputMeta;
    }

    @Override
    public MojoFrameMeta getInputMeta() {
      return inputMeta;
    }

    @Override
    public MojoFrameMeta getOutputMeta() {
      return outputMeta;
    }

    @Override
    protected MojoFrameBuilder getFrameBuilder(MojoColumn.Kind kind) {
      return new MojoFrameBuilder(outputMeta);
    }

    @Override
    protected MojoFrameMeta getMeta(MojoColumn.Kind kind) {
      return outputMeta;
    }

    @Override
    public MojoFrame transform(MojoFrame inputFrame, MojoFrame outputFrame) {
      return outputFrame;
    }

    @Override
    public void setShapPredictContrib(boolean enable) {
    }

    @Override
    public void setShapPredictContribOriginal(boolean enable) {
    }

    @Override
    public void setListener(BasePipelineListener listener) {
    }
  }
}
