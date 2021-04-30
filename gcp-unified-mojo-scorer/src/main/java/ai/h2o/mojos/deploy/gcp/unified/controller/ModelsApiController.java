package ai.h2o.mojos.deploy.gcp.unified.controller;

import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.unified.api.ModelApi;
import ai.h2o.mojos.deploy.common.rest.unified.model.Model;
import ai.h2o.mojos.deploy.common.rest.unified.model.ScoreResponse;
import ai.h2o.mojos.deploy.common.transform.MojoScorer;
import com.google.common.base.Strings;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ModelsApiController implements ModelApi {
  private static final Logger log = LoggerFactory.getLogger(ModelsApiController.class);

  private final MojoScorer scorer;

  /**
   * Simple Api controller. Inherits from {@link ModelApi}, which controls global, expected request
   * mappings for the rest service.
   *
   * @param scorer {@link MojoScorer} initialized class containing loaded mojo, and mojo interaction
   *     methods
   */
  @Autowired
  public ModelsApiController(MojoScorer scorer) {
    this.scorer = scorer;
  }

  @Override
  public ResponseEntity<String> getModelId() {
    return ResponseEntity.ok(scorer.getModelId());
  }

  @Override
  public ResponseEntity<ScoreResponse> getScore(ai.h2o.mojos.deploy.common.rest.unified.model.ScoreRequest gcpRequest) {
    try {
      log.info("Got scoring request");
      // Convert GCP request to REST request
      ScoreRequest request = getRestScoreRequest(gcpRequest);
      ScoreResponse response = getGcpScoreResponse(scorer.score(request));
      // return ResponseEntity.ok(scorer.score(request));
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.info("Failed scoring request: {}, due to: {}", gcpRequest, e.getMessage());
      log.debug(" - failure cause: ", e);
      return ResponseEntity.badRequest().build();
    }
  }
  
  /**
   * Converts GCP AI Unified request to REST module request.
   *
   * @param gcpRequest {@link ai.h2o.mojos.deploy.common.rest.unified.model.ScoreRequest} GCP unified request to be converted
   */
  public static ScoreRequest getRestScoreRequest(ai.h2o.mojos.deploy.common.rest.unified.model.ScoreRequest gcpRequest) {
    ScoreRequest request = new ScoreRequest();
    
    if (gcpRequest.getParameters().getIncludeFieldsInOutput() != null) {
      request.setIncludeFieldsInOutput(gcpRequest.getParameters().getIncludeFieldsInOutput());
    }
    
    request.setNoFieldNamesInOutput(gcpRequest.getParameters().isNoFieldNamesInOutput());
    request.setIdField(gcpRequest.getParameters().getIdField());
    request.setFields(gcpRequest.getParameters().getFields());
    
    Row row;
    for (ai.h2o.mojos.deploy.common.rest.unified.model.Row gcpRow: gcpRequest.getInstances()) {
      row = new Row();
      for (int i = 0; i < gcpRow.size(); i++) {
        row.add(gcpRow.get(i));
      }
      
      request.addRowsItem(row);
    }
    
    return request;
  }
  
  /**
   * Converts REST module response to GCP AI Unified response.
   *
   * @param restResponse {@link ai.h2o.mojos.deploy.common.rest.model.ScoreResponse} REST module response to convert
   */
  public static ScoreResponse getGcpScoreResponse(ai.h2o.mojos.deploy.common.rest.model.ScoreResponse restResponse) {
    ScoreResponse response = new ScoreResponse();
    
    response.setId(restResponse.getId());
    response.setFields(restResponse.getFields());
    
    ai.h2o.mojos.deploy.common.rest.unified.model.Row row;
    for (Row restRow: restResponse.getScore()) {
      row = new ai.h2o.mojos.deploy.common.rest.unified.model.Row();
      for (int i = 0; i < restRow.size(); i++) {
        row.add(restRow.get(i));
      }
      
      response.addPredictionsItem(row);
    }
    
    return response;
  }
}
