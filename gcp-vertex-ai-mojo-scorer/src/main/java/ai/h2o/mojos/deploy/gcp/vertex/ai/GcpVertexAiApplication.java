package ai.h2o.mojos.deploy.gcp.vertex.ai;

import ai.h2o.mojos.deploy.gcp.vertex.ai.config.EnvironmentConfiguration;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GcpVertexAiApplication {
  /**
   * Wrapper application for running local rest scorer in Google Vertex AI. Downloads pipeline.mojo
   * and license.sig files from GCS before starting rest server.
   *
   * @param args N/A, application only requires environment variables
   */
  public static void main(String[] args) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    new EnvironmentConfiguration(storage).configureScoringEnvironment();
    new SpringApplication(GcpVertexAiApplication.class).run(args);
  }
}
