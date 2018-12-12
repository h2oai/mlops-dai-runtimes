package ai.h2o.mojos.deploy.local.rest.controller;

import ai.h2o.mojos.deploy.common.rest.api.ModelsApi;
import ai.h2o.mojos.deploy.common.rest.model.Model;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.io.IOException;
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
        if (Strings.isNullOrEmpty(id)) {
            log.info("Request is missing a valid id");
            return ResponseEntity.badRequest().build();
        }
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
        if (Strings.isNullOrEmpty(id)) {
            log.info("Request is missing a valid id");
            return ResponseEntity.badRequest().build();
        }
        if (!id.equals(scorer.getModelId())) {
            log.info("Model {} not found", id);
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.ok(scorer.score(request));
        } catch (Exception e) {
            log.info("Failed scoring request: {}, due to: {}", request, e.getMessage());
            log.debug(" - failure cause: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    public ResponseEntity<ScoreResponse> getScoreByFile(String id, String file) {
        if (Strings.isNullOrEmpty(id)) {
            log.info("Request is missing a valid id");
            return ResponseEntity.badRequest().build();
        }
        if (Strings.isNullOrEmpty(file)) {
            log.info("Request is missing a valid CSV file path");
            return ResponseEntity.badRequest().build();
        }
        if (!id.equals(scorer.getModelId())) {
            log.info("Model {} not found", id);
            return ResponseEntity.notFound().build();
        }
        try {
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
}
