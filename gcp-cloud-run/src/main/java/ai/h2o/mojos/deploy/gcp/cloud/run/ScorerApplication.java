package ai.h2o.mojos.deploy.gcp.cloud.run;

import ai.h2o.mojos.deploy.gcp.cloud.run.config.EnvironmentConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication
public class ScorerApplication {
  public static void main(String[] args) {
    new EnvironmentConfiguration().configureScoringEnvironment();
    new SpringApplication(ScorerApplication.class).run(args);
  }
}
