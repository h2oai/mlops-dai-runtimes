package ai.h2o.mojos.deploy.common.transform;

import ai.h2o.mojos.deploy.common.rest.model.ContributionResponse;
import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.runtime.frame.MojoFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
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

    ContributionResponse contributionResponse = new ContributionResponse();
    contributionResponse.setContributions(outputRows);

    List<String> outputFieldNames = new ArrayList<>(
            Arrays.asList(shapleyMojoFrame.getColumnNames()));
    contributionResponse.setFields(outputFieldNames);

    return contributionResponse;
  }
}
