package ai.h2o.mojos.deploy.sql.db.config;

import ai.h2o.mojos.deploy.sql.db.controller.MojoScorer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ScorerConfiguration {
  @Bean
  public MojoScorer mojoScorer() {
    return new MojoScorer();
  }
}
