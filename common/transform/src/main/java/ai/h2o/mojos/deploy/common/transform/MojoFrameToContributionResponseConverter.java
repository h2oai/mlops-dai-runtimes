package ai.h2o.mojos.deploy.common.transform;

import ai.h2o.mojos.deploy.common.rest.model.ContributionByOutputGroup;
import ai.h2o.mojos.deploy.common.rest.model.ContributionResponse;
import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.runtime.frame.MojoFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MojoFrameToContributionResponseConverter {

  /**
   * Converts the resulting predicted {@link MojoFrame} into the API response object {@link
   * ContributionResponse}.
   */
  public ContributionResponse contributionResponseWithoutOutputGroup(
          MojoFrame shapleyMojoFrame) {
    List<Row> outputRows = Stream.generate(Row::new).limit(shapleyMojoFrame.getNrows())
            .collect(Collectors.toList());
    Utils.copyResultFields(shapleyMojoFrame, outputRows);

    List<String> outputFeatureNames = Arrays.asList(shapleyMojoFrame.getColumnNames());

    ContributionResponse contributionResponse = new ContributionResponse();
    contributionResponse.setFeatures(outputFeatureNames);

    ContributionByOutputGroup contribution = new ContributionByOutputGroup();
    contribution.setContributions(outputRows);
    // for REGRESSION and BINOMIAL models the contribution response
    // contains only one ContributionByOutputGroup object
    contributionResponse.addContributionByOutputGroupItem(contribution);

    return contributionResponse;
  }

  /**
   * Converts the resulting predicted {@link MojoFrame} into the API response object {@link
   * ContributionResponse grouped by the strings called as outputgroupNames}.
   */
  public ContributionResponse contributionResponseWithOutputGroup(
          MojoFrame shapleyMojoFrame, List<String> outputGroupNames) {
    List<Row> outputRows = Stream.generate(Row::new).limit(shapleyMojoFrame.getNrows())
            .collect(Collectors.toList());
    Utils.copyResultFields(shapleyMojoFrame, outputRows);

    List<String> outputFieldNames = new ArrayList<>(
            Arrays.asList(shapleyMojoFrame.getColumnNames()));

    ContributionResponse contributionResponse = new ContributionResponse();
    contributionResponse.setContributionByOutputGroup(new ArrayList<>());
    List<String> featureNames = new ArrayList<>();
    boolean isFirstOutputGroup = true;

    for (String outputGroupName : outputGroupNames) {
      ContributionByOutputGroup contributionByOutputGroup = new ContributionByOutputGroup();
      contributionByOutputGroup.setOutputGroup(outputGroupName);
      contributionByOutputGroup.setContributions(Stream.generate(Row::new)
              .limit(outputRows.size()).collect(Collectors.toList()));
      Pattern pattern = Pattern.compile("\\." + outputGroupName);

      for (int i = 0; i < outputFieldNames.size(); i++) {
        String outputFieldName = outputFieldNames.get(i);
        Matcher m = pattern.matcher(outputFieldName);
        if (m.find()) {
          if (isFirstOutputGroup) {
            String columnName = outputFieldName.substring(0, m.start());
            featureNames.add(columnName);
          }
          for (int k = 0; k < outputRows.size(); k++) {
            Row row = contributionByOutputGroup.getContributions().get(k);
            row.add(outputRows.get(k).get(i));
          }
        }
      }
      contributionResponse.getContributionByOutputGroup().add(contributionByOutputGroup);
      isFirstOutputGroup = false;
    }
    contributionResponse.setFeatures(featureNames);
    return contributionResponse;
  }
}
