package ai.h2o.mojos.deploy.local.rest.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
  @NonNull private final int status;
  @NonNull private final String detail;
}
