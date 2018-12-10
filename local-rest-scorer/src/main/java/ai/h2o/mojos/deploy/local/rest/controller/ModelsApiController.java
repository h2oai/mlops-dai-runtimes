package ai.h2o.mojos.deploy.local.rest.controller;

import ai.h2o.mojos.deploy.local.rest.api.ModelsApi;
import ai.h2o.mojos.deploy.local.rest.model.Model;
import ai.h2o.mojos.deploy.local.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.local.rest.model.ScoreResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ModelsApiController implements ModelsApi {

    private static final Logger log = LoggerFactory.getLogger(ModelsApiController.class);

    @org.springframework.beans.factory.annotation.Autowired
    public ModelsApiController() {
    }

    public ResponseEntity<Model> getModelInfo(String id) {
        log.error("Ignoring request getModelInfo for model id: {}", id);
        return new ResponseEntity<Model>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<List<String>> getModels() {
        log.error("Ignoring request getModels");
        return new ResponseEntity<List<String>>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<ScoreResponse> getScore(ScoreRequest body, String id) {
        log.error("Ignoring request getScore for model id: {}, request: {}", id, body);
        return new ResponseEntity<ScoreResponse>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<ScoreResponse> getScoreByFile(String id, String file) {
        log.error("Ignoring request getScoreByFile for model id: {}, file: {}", id, file);
        return new ResponseEntity<ScoreResponse>(HttpStatus.NOT_IMPLEMENTED);
    }

}
