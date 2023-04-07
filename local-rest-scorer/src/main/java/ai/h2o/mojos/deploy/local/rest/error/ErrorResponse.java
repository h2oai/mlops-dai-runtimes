package ai.h2o.mojos.deploy.local.rest.error;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ErrorResponse {
  private final int status;
  @NonNull private final String detail;
}
