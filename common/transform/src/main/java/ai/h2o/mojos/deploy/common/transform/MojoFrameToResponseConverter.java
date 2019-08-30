package ai.h2o.mojos.deploy.common.transform;

import static java.util.Collections.emptyList;

import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts the resulting predicted {@link MojoFrame} into the API response object {@link
 * ScoreResponse}.
 */
public class MojoFrameToResponseConverter
    implements BiFunction<MojoFrame, ScoreRequest, ScoreResponse> {

  @Override
  public ScoreResponse apply(MojoFrame mojoFrame, ScoreRequest scoreRequest) {
    List<Row> outputRows =
        Stream.generate(Row::new).limit(mojoFrame.getNrows()).collect(Collectors.toList());
    copyFilteredInputFields(scoreRequest, outputRows);
    copyResultFields(mojoFrame, outputRows);

    ScoreResponse response = new ScoreResponse();
    response.setScore(outputRows);
    return response;
  }

  private static void copyFilteredInputFields(ScoreRequest scoreRequest, List<Row> outputRows) {
    Set<String> includedFields =
        new HashSet<>(
            Optional.ofNullable(scoreRequest.getIncludeFieldsInOutput()).orElse(emptyList()));
    if (includedFields.isEmpty()) {
      return;
    }
    List<Row> inputRows = scoreRequest.getRows();
    for (int row = 0; row < outputRows.size(); row++) {
      Row inputRow = inputRows.get(row);
      Row outputRow = outputRows.get(row);
      List<String> inputFields = scoreRequest.getFields();
      for (int col = 0; col < inputFields.size(); col++) {
        if (includedFields.contains(inputFields.get(col))) {
          outputRow.add(inputRow.get(col));
        }
      }
    }
  }

  private static void copyResultFields(MojoFrame mojoFrame, List<Row> outputRows) {
    String[][] outputColumns = new String[mojoFrame.getNcols()][];
    for (int col = 0; col < mojoFrame.getNcols(); col++) {
      outputColumns[col] = mojoFrame.getColumn(col).getDataAsStrings();
    }
    for (int row = 0; row < mojoFrame.getNrows(); row++) {
      Row outputRow = outputRows.get(row);
      for (String[] resultColumn : outputColumns) {
        outputRow.add(resultColumn[row]);
      }
    }
  }
}
