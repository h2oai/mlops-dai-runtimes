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

    List<String> outputFeatureNames = new ArrayList<>(
            Arrays.asList(shapleyMojoFrame.getColumnNames()));

    ContributionResponse contributionResponse = new ContributionResponse();
    contributionResponse.setFeatures(outputFeatureNames);
    contributionResponse.setContributionOutputGroup(new ArrayList<>());

    ContributionOutputGroup contribution = new ContributionOutputGroup();
    contribution.setContributions(outputRows);
    // for REGRESSION and BINOMIAL models the contribution response
    // contains only one ContributionOutputGroup object
    contributionResponse.getContributionOutputGroup().add(contribution);

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
    contributionResponse.setContributionOutputGroup(new ArrayList<>());
    List<String> featureNames = new ArrayList<>();
    boolean isFirstOutputGroup = true;

    for (String outputGroupName : outputGroupNames) {
      ContributionOutputGroup contributionOutputGroup = new ContributionOutputGroup();
      contributionOutputGroup.setOutputGroup(outputGroupName);
      contributionOutputGroup.setContributions(Stream.generate(Row::new)
              .limit(outputRows.size()).collect(Collectors.toList()));

      for (int i = 0; i < outputFieldNames.size(); i++) {
        String outputFieldName = outputFieldNames.get(i);
        Matcher m = Pattern.compile("\\." + outputGroupName).matcher(outputFieldName);
        if (m.find()) {
          if (isFirstOutputGroup) {
            String columnName = outputFieldName.substring(0, m.start());
            featureNames.add(columnName);
          }
          for (int k = 0; k < outputRows.size(); k++) {
            Row row = contributionOutputGroup.getContributions().get(k);
            row.add(outputRows.get(k).get(i));
          }
        }
      }
      contributionResponse.getContributionOutputGroup().add(contributionOutputGroup);
      isFirstOutputGroup = false;
    }
    contributionResponse.setFeatures(featureNames);
    return contributionResponse;
  }
}
