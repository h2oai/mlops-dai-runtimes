package ai.h2o.mojos.deploy.local.rest.controller;

import ai.h2o.mojos.deploy.common.rest.api.ModelsApi;
import ai.h2o.mojos.deploy.common.rest.model.Model;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.List;

import static java.util.Collections.singletonList;

@Controller
public class ModelsApiController implements ModelsApi {

    private static final Logger log = LoggerFactory.getLogger(ModelsApiController.class);

    private final MojoScorer scorer;

    @org.springframework.beans.factory.annotation.Autowired
    public ModelsApiController(MojoScorer scorer) {
        this.scorer = scorer;
    }

    public ResponseEntity<Model> getModelInfo(String id) {
        if (!id.equals(scorer.getModelId())) {
            log.info("Model {} not found", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(scorer.getModelInfo());
    }

    public ResponseEntity<List<String>> getModels() {
        return ResponseEntity.ok(singletonList(scorer.getModelId()));
    }

    public ResponseEntity<ScoreResponse> getScore(ScoreRequest request, String id) {
        if (!id.equals(scorer.getModelId())) {
            log.info("Model {} not found", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(scorer.score(request));
    }

    public ResponseEntity<ScoreResponse> getScoreByFile(String id, String file) {
        log.error("Ignoring request getScoreByFile for model id: {}, file: {}", id, file);
        return new ResponseEntity<ScoreResponse>(HttpStatus.NOT_IMPLEMENTED);
    }

}
