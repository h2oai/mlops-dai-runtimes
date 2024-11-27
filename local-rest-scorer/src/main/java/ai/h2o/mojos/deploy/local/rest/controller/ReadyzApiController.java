package ai.h2o.mojos.deploy.local.rest.controller;

import ai.h2o.mojos.deploy.common.rest.api.ReadyzApi;
import ai.h2o.mojos.deploy.common.rest.model.ModelSchema;
import ai.h2o.mojos.deploy.common.transform.MojoScorer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReadyzApiController implements ReadyzApi {

  private final MojoScorer scorer;
  private static final Logger log = LoggerFactory.getLogger(ReadyzApiController.class);

  public ReadyzApiController(MojoScorer scorer) {
    this.scorer = scorer;
  }

  @Override
  public ResponseEntity<String> getReadyz() {
    try {
      ModelSchema modelSchema = scorer.getModelInfo().getSchema();
      log.trace("model is ready: {}", modelSchema);
      return ResponseEntity.ok("Ready");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Not ready");
    }
  }
}
