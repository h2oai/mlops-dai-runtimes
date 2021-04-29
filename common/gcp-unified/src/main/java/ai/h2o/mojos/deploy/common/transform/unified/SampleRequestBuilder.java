package ai.h2o.mojos.deploy.common.transform.unified;

import static java.util.Arrays.asList;

import ai.h2o.mojos.deploy.common.rest.unified.model.Parameter;
import ai.h2o.mojos.deploy.common.rest.unified.model.Row;
import ai.h2o.mojos.deploy.common.rest.unified.model.ScoreRequest;
import ai.h2o.mojos.runtime.api.MojoColumnMeta;
import ai.h2o.mojos.runtime.frame.MojoColumn;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SampleRequestBuilder builds sample requests that pass all request validation and get actually
 * scored. The resulting score is likely to be useless. The purpose is to give the caller an example
 * request to further play with and fill with actual meaningful data.
 */
public class SampleRequestBuilder {
  /** Builds a valid {@link ScoreRequest} based on the given mojo input {@link MojoFrameMeta}. */
  public ScoreRequest build(MojoFrameMeta inputMeta) {
    ScoreRequest request = new ScoreRequest();
    final List<String> fields = inputMeta.getColumns().stream()
        .map(MojoColumnMeta::getColumnName)
        .collect(Collectors.toList());
    Parameter parameters = new Parameter();
    parameters.setFields(fields);
    request.setParameters(parameters);
    Row row = new Row();
    for (MojoColumn.Type type : inputMeta.getColumnTypes()) {
      row.add(getExampleValue(type));
    }
    request.addInstancesItem(row);
    return request;
  }

  private static String getExampleValue(MojoColumn.Type type) {
    switch (type) {
      case Bool:
        return "true";
      case Int32:
      case Int64:
      case Float32:
      case Float64:
        return "0";
      case Str:
        return "text";
      case Time64:
        return "2018-01-01";
      default:
        return "";
    }
  }
}
