package ai.h2o.mojos.deploy.common.exceptions;

public class ScoringShapleyException extends RuntimeException {
  public ScoringShapleyException() {
    super();
  }

  public ScoringShapleyException(String message, Throwable cause) {
    super(message, cause);
  }

  public ScoringShapleyException(String message) {
    super(message);
  }

  public ScoringShapleyException(Throwable cause) {
    super(cause);
  }
}
