package ai.h2o.mojos.deploy.sql.db.controller;

import ai.h2o.mojos.deploy.common.rest.jdbc.api.ModelApi;
import ai.h2o.mojos.deploy.common.rest.jdbc.model.Model;
import ai.h2o.mojos.deploy.common.rest.jdbc.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.jdbc.model.ScoreResponse;
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
  public ResponseEntity<Model> getModelInfo() {
    return ResponseEntity.ok(scorer.getModelInfo());
  }

  @Override
  public ResponseEntity<ScoreResponse> getScore(ScoreRequest request) {
    try {
      log.info("Got scoring request");
      return ResponseEntity.ok(scorer.score(request));
    } catch (Exception e) {
      log.info("Failed scoring request: {}, due to: {}", request, e.getMessage());
      log.info(" - failure cause: ", e);
      return ResponseEntity.badRequest().build();
    }
  }

  @Override
  public ResponseEntity<ScoreResponse> getScoreByGet(
      String sqlQuery, String outputTable, String idColumn) {
    ScoreRequest scoreRequest = new ScoreRequest();
    scoreRequest.setIdColumn(idColumn);
    scoreRequest.setOutputTable(outputTable);
    scoreRequest.setQuery(sqlQuery);
    return getScore(scoreRequest);
  }
}
