package ai.h2o.mojos.deploy.local.rest.controller;

import ai.h2o.mojos.deploy.common.rest.v1exp.api.ModelApi;
import ai.h2o.mojos.deploy.common.rest.v1exp.model.ScoreMediaRequest;
import ai.h2o.mojos.deploy.common.rest.v1exp.model.ScoreResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ModelsMediaController implements ModelApi {
  private static final Logger log = LoggerFactory.getLogger(ModelsMediaController.class);

  @Override
  public ResponseEntity<ScoreResponse> getMediaScore(
      ScoreMediaRequest request, List<Resource> files) {
    log.info("Got score media request");
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
        "score media files is not implemented");
  }
}
