package ai.h2o.mojos.deploy.local.rest.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice("ai.h2o.mojos.deploy.local.rest")
@RequestMapping(produces = "application/vnd.error+json")
public class ModelsExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ModelsExceptionHandler.class);

  /**
   * Custom Exception handler for ResponseStatusException type.
   */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatusException(
      ResponseStatusException exception, WebRequest request) {
    log.error("Runtime exception occurred : {}", exception.getMessage(), exception);
    ErrorResponse errorResponse = new ErrorResponse(
        exception.getStatus().value(), exception.getMessage());
    return ResponseEntity.status(exception.getStatus()).body(errorResponse);
  }

  /**
   * Custom Exception handler for all Exception type.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAllException(
      Exception exception, WebRequest request) {
    log.error("Unexpected exception occurred : {}", exception.getMessage(), exception);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }
}
