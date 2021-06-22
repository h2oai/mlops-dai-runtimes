package ai.h2o.mojos.deploy.gcp.vertex.ai.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
class JsonConfiguration {

  @Bean
  public ObjectMapper objectMapper() {
    return new Jackson2ObjectMapperBuilder()
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .featuresToEnable(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS)
        .build();
  }
}
