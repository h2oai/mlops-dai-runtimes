package ai.h2o.mojos.deploy.gcp.unified.config;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentConfiguration {
  private static final Logger log = LoggerFactory.getLogger(EnvironmentConfiguration.class);
  private static final String MOJO_DOWNLOAD_PATH = "/tmp/pipeline.mojo";
  private static final String LICENSE_DOWNLOAD_PATH = "/tmp/license.sig";
  private final Storage storage;

  public EnvironmentConfiguration(Storage storage) {
    this.storage = storage;
  }

  /**
   * Method for ensuring scoring environment is correct and has expected files: pipeline.mojo and
   * license.sig.
   */
  public void configureScoringEnvironment() {
    Map<String, String> env = System.getenv();
    String errMsg = "Environment Variable: 'DRIVERLESS_AI_LICENSE_FILE' must be set and be: %s";
    Preconditions.checkArgument(
        env.getOrDefault("DRIVERLESS_AI_LICENSE_FILE", "").equals(LICENSE_DOWNLOAD_PATH),
        errMsg,
        LICENSE_DOWNLOAD_PATH);
    downloadFromGcs(env);
    log.info("Successfully downloaded files from GCS. Starting rest scoring service");
  }

  private void downloadFromGcs(Map<String, String> env) {
    downloadFileFromGcs(getFromEnv(env, "MOJO_GCS_PATH"), Paths.get(MOJO_DOWNLOAD_PATH));
    downloadFileFromGcs(getFromEnv(env, "LICENSE_GCS_PATH"), Paths.get(LICENSE_DOWNLOAD_PATH));
  }

  private void downloadFileFromGcs(String gcsPath, Path outputPath) {
    Properties fileProps = parseGcsPath(gcsPath);
    log.info(
        String.format(
            "Parsed GCS Path to - Bucket: %s, Path: %s",
            fileProps.getProperty("bucket"), fileProps.getProperty("filepath")));
    Blob blob =
        storage.get(BlobId.of(fileProps.getProperty("bucket"), fileProps.getProperty("filepath")));
    log.info(String.format("Downloading file to: %s", outputPath.toString()));
    blob.downloadTo(outputPath);
  }

  private Properties parseGcsPath(String gcsPath) {
    Properties properties = new Properties();
    String gcsPathNoPrefix = gcsPath.replace("gs://", "");
    List<String> gcsPathArray = Splitter.on('/').splitToList(gcsPathNoPrefix);
    properties.setProperty("bucket", gcsPathArray.get(0));
    properties.setProperty("filepath", gcsPathNoPrefix.replace(gcsPathArray.get(0) + "/", ""));
    return properties;
  }

  private String getFromEnv(Map<String, String> env, String envVar) {
    String envValue = env.getOrDefault(envVar, "");
    if (envValue.isEmpty()) {
      throw new RuntimeException(
          String.format("Error: required environment variable: %s, is not set", envVar));
    }
    return envValue;
  }
}
