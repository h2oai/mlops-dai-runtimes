package ai.h2o.mojos.deploy.local.rest.controller;

import ai.h2o.mojos.deploy.common.rest.v2.api.ModelApi;
import ai.h2o.mojos.deploy.common.rest.v2.model.ScoreMediaRequest;
import ai.h2o.mojos.deploy.common.rest.v2.model.ScoreResponse;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/**
 * Simple Javadoc.
 */
@Controller
public class ModelsApiControllerV2 implements ModelApi {

  private static final Logger log = LoggerFactory.getLogger(ModelsApiControllerV2.class);

  @Override
  public ResponseEntity<ScoreResponse> getScoreMedia(
      ScoreMediaRequest payload, List<Resource> files) {
    log.info("Received score media request");
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
