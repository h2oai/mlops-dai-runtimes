package ai.h2o.mojos.deploy.local.rest.config;

import ai.h2o.mojos.deploy.common.transform.MojoFrameToResponseConverter;
import ai.h2o.mojos.deploy.common.transform.RequestToMojoFrameConverter;
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
}
