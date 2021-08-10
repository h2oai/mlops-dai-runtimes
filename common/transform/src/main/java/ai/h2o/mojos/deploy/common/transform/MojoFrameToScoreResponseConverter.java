package ai.h2o.mojos.deploy.common.transform;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import ai.h2o.mojos.deploy.common.rest.model.ContributionResponse;
import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts the resulting predicted {@link MojoFrame} into the API response object {@link
 * ScoreResponse}.
 */
public class MojoFrameToScoreResponseConverter
    implements BiFunction<MojoFrame, ScoreRequest, ScoreResponse> {

  @Override
  public ScoreResponse apply(MojoFrame mojoFrame, ScoreRequest scoreRequest) {
    Set<String> includedFields = getSetOfIncludedFields(scoreRequest);
    List<Row> outputRows =
        Stream.generate(Row::new).limit(mojoFrame.getNrows()).collect(Collectors.toList());
    copyFilteredInputFields(scoreRequest, includedFields, outputRows);
    Utils.copyResultFields(mojoFrame, outputRows);

    ScoreResponse response = new ScoreResponse();
    response.setScore(outputRows);

    if (!Boolean.TRUE.equals(scoreRequest.isNoFieldNamesInOutput())) {
      List<String> outputFieldNames = getFilteredInputFieldNames(scoreRequest, includedFields);
      outputFieldNames.addAll(asList(mojoFrame.getColumnNames()));
      response.setFields(outputFieldNames);
    }

    return response;
  }

  private static void copyFilteredInputFields(
      ScoreRequest scoreRequest, Set<String> includedFields, List<Row> outputRows) {
    if (includedFields.isEmpty()) {
      return;
    }
    boolean generateRowIds = shouldGenerateRowIds(scoreRequest, includedFields);
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
      if (generateRowIds) {
        outputRow.add(UUID.randomUUID().toString());
      }
    }
  }

  private static Set<String> getSetOfIncludedFields(ScoreRequest scoreRequest) {
    List<String> includedFields =
        Optional.ofNullable(scoreRequest.getIncludeFieldsInOutput()).orElse(emptyList());
    if (includedFields.isEmpty()) {
      return emptySet();
    }
    return new HashSet<>(includedFields);
  }

  private static boolean shouldGenerateRowIds(
      ScoreRequest scoreRequest, Set<String> includedFields) {
    String idField = scoreRequest.getIdField();
    return !Strings.isNullOrEmpty(idField)
        && includedFields.contains(idField)
        && !scoreRequest.getFields().contains(idField);
  }

  private static List<String> getFilteredInputFieldNames(
      ScoreRequest scoreRequest, Set<String> includedFields) {
    List<String> outputFields = new ArrayList<>();
    if (includedFields.isEmpty()) {
      return outputFields;
    }
    for (String field : scoreRequest.getFields()) {
      if (includedFields.contains(field)) {
        outputFields.add(field);
      }
    }
    if (shouldGenerateRowIds(scoreRequest, includedFields)) {
      outputFields.add(scoreRequest.getIdField());
    }
    return outputFields;
  }
}
