package ai.h2o.mojos.deploy.common.transform.unified;

import ai.h2o.mojos.deploy.common.rest.unified.model.Row;
import ai.h2o.mojos.deploy.common.rest.unified.model.ScoreRequest;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Converts the original API request object {@link ScoreRequest} into the input {@link MojoFrame}.
 */
public class RequestToMojoFrameConverter
    implements BiFunction<ScoreRequest, MojoFrameBuilder, MojoFrame> {
  @Override
  public MojoFrame apply(ScoreRequest scoreRequest, MojoFrameBuilder frameBuilder) {
    List<String> fields = scoreRequest.getParameters().getFields();

    if (scoreRequest.getInstances() != null) {
      for (Row row : scoreRequest.getInstances()) {
        MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();
        for (int i = 0; i < row.size(); i++) {
          rowBuilder.setValue(fields.get(i), row.get(i));
        }
        frameBuilder.addRow(rowBuilder);
      }
    }

    return frameBuilder.toMojoFrame();
  }
}
