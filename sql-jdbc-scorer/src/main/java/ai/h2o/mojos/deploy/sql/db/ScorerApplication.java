package ai.h2o.mojos.deploy.sql.db;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScorerApplication {
  public static void main(String[] args) {
    new SpringApplication(ScorerApplication.class).run(args);
  }
}
