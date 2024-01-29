package ai.h2o.mojos.deploy.local.rest.error;

import org.springframework.http.HttpStatus;

public class ErrorUtil {

  /** Translate exception type into error response code. */
  public static HttpStatus translateErrorCode(Exception exception) {
    if (exception instanceof IllegalArgumentException) {
      return HttpStatus.BAD_REQUEST;
    } else if (exception instanceof IllegalStateException) {
      return HttpStatus.SERVICE_UNAVAILABLE;
    } else if (exception instanceof UnsupportedOperationException) {
      return HttpStatus.NOT_IMPLEMENTED;
    } else {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }
}
