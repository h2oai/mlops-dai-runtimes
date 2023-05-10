package ai.h2o.mojos.deploy.common.transform;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import ai.h2o.mojos.deploy.common.rest.model.PredictionInterval;
import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

  private static final String LOWER_BOUND = ".lower";
  private static final String UPPER_BOUND = ".upper";

  // If true then pipeline support prediction interval, otherwise false.
  // Note: guarantee that pipeline supports Prediction interval.
  private Boolean supportPredictionInterval;

  public MojoFrameToScoreResponseConverter(boolean supportPredictionInterval) {
    this.supportPredictionInterval = supportPredictionInterval;
  }

  public MojoFrameToScoreResponseConverter() {
    supportPredictionInterval = false;
  }

  /**
   * Transform MOJO response frame into ScoreResponse.
   * @param mojoFrame mojo response frame.
   * @param scoreRequest score request.
   * @return score response.
   */
  @Override
  public ScoreResponse apply(
      MojoFrame mojoFrame, ScoreRequest scoreRequest) {
    Set<String> includedFields = getSetOfIncludedFields(scoreRequest);
    List<Row> outputRows =
        Stream.generate(Row::new).limit(mojoFrame.getNrows()).collect(Collectors.toList());
    copyFilteredInputFields(scoreRequest, includedFields, outputRows);
    fillOutputRows(mojoFrame, outputRows);

    ScoreResponse response = new ScoreResponse();
    response.setScore(outputRows);

    if (!Boolean.TRUE.equals(scoreRequest.isNoFieldNamesInOutput())) {
      List<String> outputFieldNames = getFilteredInputFieldNames(scoreRequest, includedFields);
      outputFieldNames.addAll(getTargetField(mojoFrame));
      response.setFields(outputFieldNames);
    }
    fillWithPredictionInterval(mojoFrame, scoreRequest, response);
    return response;
  }

  /**
   * Populate target column rows into outputRows.
   */
  private void fillOutputRows(
      MojoFrame mojoFrame, List<Row> outputRows) {
    List<Row> targetRows = getTargetRows(mojoFrame);
    for (int row = 0; row < mojoFrame.getNrows(); row++) {
      outputRows.get(row).addAll(targetRows.get(row));
    }
  }

  /**
   * Populate Prediction Interval value into response field.
   * Only when score request enable PredictionInterval and
   * response frame match expected return structure and pipeline support.
   */
  private void fillWithPredictionInterval(
      MojoFrame mojoFrame, ScoreRequest scoreRequest, ScoreResponse scoreResponse) {
    if (Boolean.TRUE.equals(scoreRequest.isRequestPredictionIntervals())) {
      if (!supportPredictionInterval || !isPredictionIntervalAvailable(mojoFrame)) {
        throw new IllegalStateException(
          "Unexpected error, prediction interval should not be supported");
      }
      PredictionInterval predictionInterval = new PredictionInterval();
      predictionInterval.setFields(getPredictionIntervalFields(mojoFrame));
      predictionInterval.setRows(getPredictionIntervalRows(mojoFrame));
      scoreResponse.setPredictionIntervals(predictionInterval);
    }
  }

  /**
   * Extract target column rows from MOJO response frame.
   * Note: To ensure backward compatibility,
   * if prediction interval enabled then only preserve target
   * column rows from response columns.
   */
  private List<Row> getTargetRows(MojoFrame mojoFrame) {
    List<Row> taretRows = Stream
        .generate(Row::new)
        .limit(mojoFrame.getNrows())
        .collect(Collectors.toList());
    for (int row = 0; row < mojoFrame.getNrows(); row++) {
      for (int col = 0; col < getTargetFieldCount(mojoFrame); col++) {
        String cell = mojoFrame.getColumn(col).getDataAsStrings()[row];
        taretRows.get(row).add(cell);
      }
    }
    return taretRows;
  }

  /**
   * Extract prediction interval columns rows from MOJO response frame.
   * Note: Assumption is prediction is already enabled and response frame
   * has expected structure.
   */
  private List<Row> getPredictionIntervalRows(MojoFrame mojoFrame) {
    List<Row> predictionIntervalRows = Stream
        .generate(Row::new)
        .limit(mojoFrame.getNrows())
        .collect(Collectors.toList());
    for (int row = 0; row < mojoFrame.getNrows(); row++) {
      for (int col = 1; col < mojoFrame.getNcols(); col++) {
        String cell = mojoFrame.getColumn(col).getDataAsStrings()[row];
        predictionIntervalRows.get(row).add(cell);
      }
    }
    return predictionIntervalRows;
  }

  /**
   * Extract target columns from MOJO response frame.
   * When prediction interval enabled, only returns the
   * header of response frame.
   */
  private List<String> getTargetField(
      MojoFrame mojoFrame) {
    if (mojoFrame.getNcols() > 0) {
      List<String> targetColumns = Arrays.asList(mojoFrame.getColumnNames());
      if (supportPredictionInterval) {
        if (!isPredictionIntervalAvailable(mojoFrame)) {
          throw new IllegalStateException(
            "Unexpected error, prediction interval should not be supported");
        }
        return targetColumns.subList(0, 1);
      }
      return targetColumns;
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Get number of target columns from MOJO response frame.
   */
  private int getTargetFieldCount(MojoFrame mojoFrame) {
    if (supportPredictionInterval) {
      if (!isPredictionIntervalAvailable(mojoFrame)) {
        throw new IllegalStateException(
          "Unexpected error, prediction interval should not be supported");
      }
      return Math.min(1, mojoFrame.getNcols());
    }
    return mojoFrame.getNcols();
  }

  /**
   * Extract Prediction interval columns names from MOJO response frame.
   * Note: Assumption is prediction is already enabled and response frame
   * has expected structure.
   */
  private Row getPredictionIntervalFields(MojoFrame mojoFrame) {
    Row row = new Row();
    row.addAll(Arrays.asList(mojoFrame.getColumnNames()).subList(1, mojoFrame.getNcols()));
    return row;
  }

  /**
   * True if MOJO response frame has expected structure when prediction interval is
   * supported, false otherwise.
   */
  private boolean isPredictionIntervalAvailable(MojoFrame mojoFrame) {
    return mojoFrame.getColumnNames().length == 3
      && mojoFrame.getColumnType(0).equals(mojoFrame.getColumnType(1))
      && mojoFrame.getColumnType(1).equals(mojoFrame.getColumnType(2))
      && mojoFrame.getColumnType(0).isnumeric
      && Arrays.stream(mojoFrame.getColumnNames()).anyMatch(field -> field.endsWith(LOWER_BOUND))
      && Arrays.stream(mojoFrame.getColumnNames()).anyMatch(field -> field.endsWith(UPPER_BOUND));
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
