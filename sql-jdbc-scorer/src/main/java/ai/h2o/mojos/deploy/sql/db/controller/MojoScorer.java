package ai.h2o.mojos.deploy.sql.db.controller;

import ai.h2o.mojos.deploy.common.jdbc.SqlScorerClient;
import ai.h2o.mojos.deploy.common.rest.jdbc.model.Model;
import ai.h2o.mojos.deploy.common.rest.jdbc.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.jdbc.model.ScoreResponse;

public class MojoScorer {

  private final SqlScorerClient sqlScorerClient;

  public MojoScorer() {
    this.sqlScorerClient = SqlScorerClient.init();
  }

  ScoreResponse score(ScoreRequest request) {
    sqlScorerClient.scoreQuery(request);
    ScoreResponse scoreResponse = new ScoreResponse();
    scoreResponse.setId(getModelId());
    scoreResponse.setSuccess(true);
    return scoreResponse;
  }

  String getModelId() {
    return sqlScorerClient.getModelId();
  }

  Model getModelInfo() {
    return sqlScorerClient.getModelInfo();
  }
}
