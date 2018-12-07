package ai.h2o.mojos.deploy.local.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
public class ScorerApplication {
    public static void main(String[] args) {
        new SpringApplication(ScorerApplication.class).run(args);
    }
}
