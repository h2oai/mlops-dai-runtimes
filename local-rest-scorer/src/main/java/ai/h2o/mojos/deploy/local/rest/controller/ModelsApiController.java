package ai.h2o.mojos.deploy.local.rest.controller;

import ai.h2o.mojos.deploy.common.rest.api.ModelApi;
import ai.h2o.mojos.deploy.common.rest.model.CapabilityType;
import ai.h2o.mojos.deploy.common.rest.model.ContributionRequest;
import ai.h2o.mojos.deploy.common.rest.model.ContributionResponse;
import ai.h2o.mojos.deploy.common.rest.model.Model;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.deploy.common.transform.MojoScorer;
import ai.h2o.mojos.deploy.common.transform.SampleRequestBuilder;
import ai.h2o.mojos.deploy.common.transform.ShapleyLoadOption;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ModelsApiController implements ModelApi {

  private static final String MODEL_ID = "MODEL_ID";
  private static final Logger log = LoggerFactory.getLogger(ModelsApiController.class);

  private final MojoScorer scorer;
  private final SampleRequestBuilder sampleRequestBuilder;

  private final List<CapabilityType> supportedCapabilities;

  /**
   * Simple Api controller. Inherits from {@link ModelApi}, which controls global, expected request
   * mappings for the rest service.
   *
   * @param scorer {@link MojoScorer} initialized class containing loaded mojo, and mojo interaction
   *     methods
   * @param sampleRequestBuilder {@link SampleRequestBuilder} initialized class, for generating
   *     sample request.
   */
  @Autowired
  public ModelsApiController(MojoScorer scorer, SampleRequestBuilder sampleRequestBuilder) {
    this.scorer = scorer;
    this.sampleRequestBuilder = sampleRequestBuilder;
    this.supportedCapabilities = assembleSupportedCapabilities(
        scorer.getEnabledShapleyTypes()
    );
  }

  @Override
  public ResponseEntity<Model> getModelInfo() {
    return ResponseEntity.ok(scorer.getModelInfo());
  }

  @Override
  public ResponseEntity<String> getModelId() {
    try {
      return ResponseEntity.ok(System.getenv(MODEL_ID));
    } catch (Exception ignored) {
      return ResponseEntity.ok(scorer.getModelId());
    }
  }

  @Override
  public ResponseEntity<List<CapabilityType>> getCapabilities() {
    return ResponseEntity.ok(supportedCapabilities);
  }

  @Override
  public ResponseEntity<ScoreResponse> getScore(ScoreRequest request) {
    try {
      log.info("Got scoring request");
      ScoreResponse scoreResponse = scorer.score(request);
      return ResponseEntity.ok(scoreResponse);
    } catch (Exception e) {
      log.info("Failed scoring request due to: {}", e.getMessage());
      log.debug(" - request content: ", request);
      log.debug(" - failure cause: ", e);
      return ResponseEntity.badRequest().build();
    }
  }

  @Override
  public ResponseEntity<ScoreResponse> getScoreByFile(String file) {
    if (Strings.isNullOrEmpty(file)) {
      log.info("Request is missing a valid CSV file path");
      return ResponseEntity.badRequest().build();
    }
    try {
      log.info("Got scoring request for CSV");
      return ResponseEntity.ok(scorer.scoreCsv(file));
    } catch (IOException e) {
      log.info("Failed loading CSV file: {}, due to: {}", file, e.getMessage());
      log.debug(" - failure cause: ", e);
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.info("Failed scoring CSV file: {}, due to: {}", file, e.getMessage());
      log.debug(" - failure cause: ", e);
      return ResponseEntity.badRequest().build();
    }
  }

  @Override
  public ResponseEntity<ContributionResponse> getContribution(
          ContributionRequest request) {
    try {
      log.info("Got shapley contribution request");
      ContributionResponse contributionResponse
              = scorer.computeContribution(request);
      return ResponseEntity.ok(contributionResponse);
    } catch (UnsupportedOperationException e) {
      log.info("Unsupported operation due to: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    } catch (Exception e) {
      log.info("Failed shapley contribution request due to: {}", e.getMessage());
      log.debug(" - failure cause: ", e);
      return ResponseEntity.badRequest().build();
    }
  }

  @Override
  public ResponseEntity<ScoreRequest> getSampleRequest() {
    return ResponseEntity.ok(sampleRequestBuilder.build(scorer.getPipeline().getInputMeta()));
  }

  private static List<CapabilityType> assembleSupportedCapabilities(
      ShapleyLoadOption enabledShapleyTypes) {
    switch (enabledShapleyTypes) {
      case ALL:
        return Arrays.asList(
            CapabilityType.SCORE,
            CapabilityType.CONTRIBUTION_ORIGINAL,
            CapabilityType.CONTRIBUTION_TRANSFORMED);
      case ORIGINAL:
        return Arrays.asList(CapabilityType.SCORE, CapabilityType.CONTRIBUTION_ORIGINAL);
      case TRANSFORMED:
        return Arrays.asList(CapabilityType.SCORE, CapabilityType.CONTRIBUTION_TRANSFORMED);
      case NONE:
      default:
        return Arrays.asList(CapabilityType.SCORE);
    }
  }
}
