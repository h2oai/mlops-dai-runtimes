package ai.h2o.mojos.deploy.gcp.unified;

import ai.h2o.mojos.deploy.gcp.unified.config.EnvironmentConfiguration;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication
public class GcpUnifiedApplication {
  /**
   * Wrapper application for running local rest scorer in Google AI Platform Unified.
   * Downloads pipeline.mojo and license.sig files from GCS before starting rest server.
   *
   * @param args N/A, application only requires environment variables
   */
  public static void main(String[] args) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    new EnvironmentConfiguration(storage).configureScoringEnvironment();
    new SpringApplication(GcpUnifiedApplication.class).run(args);
  }
}