package ai.h2o.mojos.deploy.local.rest.error;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ModelsExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ModelsExceptionHandler.class);

  /** Custom Exception handler for ResponseStatusException type. */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Object> handleResponseStatusException(
      ResponseStatusException exception, WebRequest request) {
    log.error("Runtime exception occurred : {}", exception.getMessage(), exception);
    return ResponseEntity.status(exception.getStatusCode())
        .body(ImmutableMap.builder().put("detail", exception.getMessage()).build());
  }

  /** Custom Exception handler for illegal request exception type. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> handleIllegalArgumentException(
      IllegalArgumentException exception, WebRequest request) {
    log.error("Illegal request exception occurred : {}", exception.getMessage(), exception);
    return ResponseEntity.badRequest()
        .body(ImmutableMap.builder().put("detail", exception.getMessage()).build());
  }

  /** Custom Exception handler for illegal state exception type. */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Object> handleIllegalStateException(
      IllegalStateException exception, WebRequest request) {
    log.error("Illegal state exception occurred : {}", exception.getMessage(), exception);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ImmutableMap.builder().put("detail", exception.getMessage()).build());
  }

  /** Custom Exception handler for unsupported exception type. */
  @ExceptionHandler(UnsupportedOperationException.class)
  public ResponseEntity<Object> handleUnsupportedException(
      UnsupportedOperationException exception, WebRequest request) {
    log.error(
        "Unsupported request exception occurred {} : {}",
        request,
        exception.getMessage(),
        exception);
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
        .body(ImmutableMap.builder().put("detail", exception.getMessage()).build());
  }

  /** Custom Exception handler for all Exception type. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleAllException(Exception exception, WebRequest request) {
    log.error("Unexpected exception occurred : {}", exception.getMessage(), exception);
    return ResponseEntity.internalServerError()
        .body(ImmutableMap.builder().put("detail", exception.getMessage()).build());
  }
}
