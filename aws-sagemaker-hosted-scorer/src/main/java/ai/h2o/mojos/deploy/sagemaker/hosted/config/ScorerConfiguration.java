package ai.h2o.mojos.deploy.sagemaker.hosted.config;

import ai.h2o.mojos.deploy.common.transform.CsvToMojoFrameConverter;
import ai.h2o.mojos.deploy.common.transform.MojoFrameToResponseConverter;
import ai.h2o.mojos.deploy.common.transform.MojoPipelineToModelInfoConverter;
import ai.h2o.mojos.deploy.common.transform.MojoScorer;
import ai.h2o.mojos.deploy.common.transform.RequestToMojoFrameConverter;
import ai.h2o.mojos.deploy.common.transform.SampleRequestBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ScorerConfiguration {
  @Bean
  public MojoFrameToResponseConverter responseConverter() {
    return new MojoFrameToResponseConverter();
  }

  @Bean
  public RequestToMojoFrameConverter requestConverter() {
    return new RequestToMojoFrameConverter();
  }

  @Bean
  public MojoPipelineToModelInfoConverter modelConverter() {
    return new MojoPipelineToModelInfoConverter();
  }

  @Bean
  public CsvToMojoFrameConverter csvConverter() {
    return new CsvToMojoFrameConverter();
  }

  @Bean
  public SampleRequestBuilder sampleRequestBuilder() {
    return new SampleRequestBuilder();
  }

  @Bean
  public MojoScorer mojoScorer(
      RequestToMojoFrameConverter requestConverter,
      MojoFrameToResponseConverter responseConverter,
      MojoPipelineToModelInfoConverter modelInfoConverter,
      CsvToMojoFrameConverter csvConverter) {
    return new MojoScorer(requestConverter, responseConverter, modelInfoConverter, csvConverter);
  }
}