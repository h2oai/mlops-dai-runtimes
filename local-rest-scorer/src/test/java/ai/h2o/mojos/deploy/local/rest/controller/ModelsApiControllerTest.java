package ai.h2o.mojos.deploy.local.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.h2o.mojos.deploy.common.rest.model.CapabilityType;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.transform.MojoScorer;
import ai.h2o.mojos.deploy.common.transform.SampleRequestBuilder;
import ai.h2o.mojos.deploy.common.transform.ShapleyLoadOption;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.api.MojoPipelineService;
import ai.h2o.mojos.runtime.api.PipelineConfig;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ModelsApiControllerTest {
  @Mock private SampleRequestBuilder sampleRequestBuilder;

  @BeforeAll
  static void setup() throws IOException {
    File tmpModel = File.createTempFile("pipeline", ".mojo");
    System.setProperty("mojo.path", tmpModel.getAbsolutePath());
    mockMojoPipeline(tmpModel);
  }

  private static void mockMojoPipeline(File tmpModel) {
    MojoPipeline mojoPipeline = Mockito.mock(MojoPipeline.class);
    MockedStatic<MojoPipelineService> theMock = Mockito.mockStatic(MojoPipelineService.class);
    theMock.when(() -> MojoPipelineService
        .loadPipeline(Mockito.eq(new File(tmpModel.getAbsolutePath())), any(PipelineConfig.class)))
      .thenReturn(mojoPipeline);
  }

  @Test
  void verifyCapabilities_DefaultShapley_ReturnsExpected() {
    // Given
    List<CapabilityType> expectedCapabilities = Arrays.asList(CapabilityType.SCORE);

    MojoScorer scorer = mock(MojoScorer.class);
    when(scorer.getEnabledShapleyTypes()).thenReturn(ShapleyLoadOption.NONE);
    when(scorer.isPredictionIntervalSupport()).thenReturn(false);

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
    when(scorer.isPredictionIntervalSupport()).thenReturn(false);

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
    when(scorer.isPredictionIntervalSupport()).thenReturn(false);

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
    when(scorer.isPredictionIntervalSupport()).thenReturn(false);

    ModelsApiController controller = new ModelsApiController(scorer, sampleRequestBuilder);

    // When
    ResponseEntity<List<CapabilityType>> response = controller.getCapabilities();

    // Then
    assertEquals(expectedCapabilities, response.getBody());
  }

  @Test
  void verifyScore_Fails_ReturnsException() {
    // Given
    MojoScorer scorer = mock(MojoScorer.class);
    when(scorer.getEnabledShapleyTypes()).thenReturn(ShapleyLoadOption.TRANSFORMED);
    when(scorer.isPredictionIntervalSupport()).thenReturn(false);
    when(scorer.score(any())).thenThrow(new IllegalStateException("Test Exception"));

    ModelsApiController controller = new ModelsApiController(scorer, sampleRequestBuilder);

    // When & Then
    try {
      controller.getScore(new ScoreRequest());
    } catch (Exception ex) {
      assertTrue(ex instanceof ResponseStatusException);
      assertTrue(ex.getCause() instanceof IllegalStateException);
      assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ((ResponseStatusException) ex).getStatus());
    }
  }

  @Test
  void verifyScoreByFile_Fails_ReturnsException() throws IOException {
    // Given
    MojoScorer scorer = mock(MojoScorer.class);
    when(scorer.getEnabledShapleyTypes()).thenReturn(ShapleyLoadOption.TRANSFORMED);
    when(scorer.isPredictionIntervalSupport()).thenReturn(false);

    ModelsApiController controller = new ModelsApiController(scorer, sampleRequestBuilder);

    // When & Then
    try {
      controller.getScore(new ScoreRequest());
    } catch (Exception ex) {
      assertTrue(ex instanceof ResponseStatusException);
      assertTrue(ex.getCause() instanceof IllegalStateException);
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ((ResponseStatusException) ex).getStatus());
    }
  }

  @Test
  void verifyScoreContribution_Fails_ReturnsException() {
    // Given
    MojoScorer scorer = mock(MojoScorer.class);
    when(scorer.getEnabledShapleyTypes()).thenReturn(ShapleyLoadOption.TRANSFORMED);
    when(scorer.isPredictionIntervalSupport()).thenReturn(false);

    ModelsApiController controller = new ModelsApiController(scorer, sampleRequestBuilder);

    // When & Then
    try {
      controller.getScore(new ScoreRequest());
    } catch (Exception ex) {
      assertTrue(ex instanceof ResponseStatusException);
      assertTrue(ex.getCause() instanceof IllegalStateException);
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ((ResponseStatusException) ex).getStatus());
    }
  }
}
