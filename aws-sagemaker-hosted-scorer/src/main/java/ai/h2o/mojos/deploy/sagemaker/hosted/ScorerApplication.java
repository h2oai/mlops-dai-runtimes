package ai.h2o.mojos.deploy.sagemaker.hosted;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication
public class ScorerApplication {
  public static void main(String[] args) {
    new SpringApplication(ScorerApplication.class).run(args);
  }
}
