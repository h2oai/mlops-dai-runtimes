package ai.h2o.mojos.deploy.common.exceptions;

public class ScoringException extends RuntimeException {

  public ScoringException() {
    super();
  }

  public ScoringException(String message, Throwable cause) {
    super(message, cause);
  }

  public ScoringException(String message) {
    super(message);
  }

  public ScoringException(Throwable cause) {
    super(cause);
  }
}
