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
      ContributionOutputGroup contributionOutputGroup = new ContributionOutputGroup();
      contributionOutputGroup.setOutputClass(outputClassName);
      // make empty arrays = output rows and add them
      contributionOutputGroup.setContributions(new ArrayList<>());
      for (Row outputRow: outputRows) {
        contributionOutputGroup.getContributions().add(new Row());
      }
      contributionOutputGroup.setFields(new ArrayList<>());

      for (int i = 0; i < outputFieldNames.size(); i++) {
        String outputFieldName = outputFieldNames.get(i);

        Matcher m = Pattern.compile("\\." + outputClassName).matcher(outputFieldName);
        if (m.find()) {
          String columnName = outputFieldName.substring(0, m.start());
          contributionOutputGroup.getFields().add(columnName);
          for (int k = 0; k < outputRows.size(); k++) {
            contributionOutputGroup.getContributions().get(k).add(outputRows.get(k).get(i));
          }
        }
      }
      contributionResponse.add(contributionOutputGroup);
    }
    return contributionResponse;
  }
}
