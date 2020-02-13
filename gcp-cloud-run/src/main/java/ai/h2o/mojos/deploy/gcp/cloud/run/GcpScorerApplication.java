package ai.h2o.mojos.deploy.gcp.cloud.run;

import ai.h2o.mojos.deploy.gcp.cloud.run.config.EnvironmentConfiguration;
import ai.h2o.mojos.deploy.local.rest.ScorerApplication;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class GcpScorerApplication {
  /**
   * Wrapper application for running local rest scorer in Google Cloud Run. Downloads pipeline.mojo
   * and license.sig files from GCS before starting rest server.
   *
   * @param args N/A, application only requires environment variables
   */
  public static void main(String[] args) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    new EnvironmentConfiguration(storage).configureScoringEnvironment();
    ScorerApplication.main(args);
  }
}
