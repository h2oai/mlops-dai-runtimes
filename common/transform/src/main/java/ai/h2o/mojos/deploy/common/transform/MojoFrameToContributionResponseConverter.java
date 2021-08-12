package ai.h2o.mojos.deploy.common.transform;

import ai.h2o.mojos.deploy.common.rest.model.ContributionOutputGroup;
import ai.h2o.mojos.deploy.common.rest.model.ContributionResponse;
import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.runtime.frame.MojoFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts the resulting predicted {@link MojoFrame} into the API response object {@link
 * ContributionResponse}.
 */
public class MojoFrameToContributionResponseConverter
        implements Function<MojoFrame, ContributionResponse> {
  @Override
  public ContributionResponse apply(
          MojoFrame shapleyMojoFrame) {
    List<Row> outputRows = Stream.generate(Row::new).limit(shapleyMojoFrame.getNrows())
            .collect(Collectors.toList());
    Utils.copyResultFields(shapleyMojoFrame, outputRows);

    List<String> outputFieldNames = new ArrayList<>(
            Arrays.asList(shapleyMojoFrame.getColumnNames()));

    ContributionOutputGroup contribution = new ContributionOutputGroup();
    contribution.setContributions(outputRows);
    contribution.setFields(outputFieldNames);

    ContributionResponse contributionResponse = new ContributionResponse();
    contributionResponse.add(contribution);

    return contributionResponse;
  }

  /**
   * Converts the resulting predicted {@link MojoFrame} into the API response object {@link
   * ContributionResponse}.
   */
  public ContributionResponse apply(
          MojoFrame shapleyMojoFrame, List<String> outputGroupNames) {
    List<Row> outputRows = Stream.generate(Row::new).limit(shapleyMojoFrame.getNrows())
            .collect(Collectors.toList());
    Utils.copyResultFields(shapleyMojoFrame, outputRows);

    List<String> outputFieldNames = new ArrayList<>(
            Arrays.asList(shapleyMojoFrame.getColumnNames()));

    ContributionResponse contributionResponse = new ContributionResponse();
    for (String outputClassName : outputGroupNames) {
      ContributionOutputGroup contribution = new ContributionOutputGroup();
      contribution.setOutputClass(outputClassName);
      contribution.setContributions(new ArrayList<>());
      contribution.setFields(new ArrayList<>());

      for (int i = 0; i < outputFieldNames.size(); i++) {
        String outputFieldName = outputFieldNames.get(i);
        Row row = new Row();
        for (Row outputRow: outputRows) {
          row.add(outputRow.get(i));
        }

        Matcher m = Pattern.compile("\\." + outputClassName).matcher(outputFieldName);
        if (m.find()) {
          String columnName = outputFieldName.substring(0, m.start());
          contribution.getFields().add(columnName);
          contribution.getContributions().add(row);
        }
      }
      contributionResponse.add(contribution);
    }
    return contributionResponse;
  }
}
