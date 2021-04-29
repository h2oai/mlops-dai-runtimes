package ai.h2o.mojos.deploy.common.transform.unified;

import ai.h2o.mojos.deploy.common.rest.unified.model.ScoreRequest;

/** Thrown on issues with the {@link ScoreRequest}. */
public class ScoreRequestFormatException extends Exception {
  private final ScoreRequest exampleRequest;

  public ScoreRequestFormatException(String message, ScoreRequest exampleRequest) {
    super(message);
    this.exampleRequest = exampleRequest;
  }

  public ScoreRequest getExampleRequest() {
    return exampleRequest;
  }
}
