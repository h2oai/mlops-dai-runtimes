package ai.h2o.mojos.deploy.gcp.cloud.run.config;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Splitter;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentConfiguration {

  private static final Logger log = LoggerFactory.getLogger(EnvironmentConfiguration.class);
  private final String mojoDownloadPath = "/mojos";
  private final String licenseDownloadPath = "/secrets";
  private final Storage storage = initiateStorageClient();

  /**
   * Method for ensuring scoring environment is correct and has expected files: pipeline.mojo and
   * license.sig.
   */
  public void configureScoringEnvironment() {
    try {
      assert !System.getenv("DRIVERLESS_AI_LICENSE_FILE").isEmpty();
      downloadFromGcs();
      File mojoFile = Paths.get(mojoDownloadPath, "pipeline.mojo").toFile();
      assert mojoFile.exists();
      File licenseFile = Paths.get(licenseDownloadPath, "license.sig").toFile();
      assert licenseFile.exists();
    } catch (Exception e) {
      throw new RuntimeException("Exception during startup", e);
    }
  }

  private void downloadFromGcs() {
    ensureDirectoryExists(mojoDownloadPath);
    downloadFileFromGcs(getFromEnv("MOJO_GCS_PATH"));
    ensureDirectoryExists(licenseDownloadPath);
    downloadFileFromGcs(getFromEnv("LICENSE_GCS_PATH"));
  }

  private void downloadFileFromGcs(String filePath) {
    Properties fileProps = parseGcsPath(filePath);
    log.info(
        String.format(
            "Parsed GCS Path to - Bucket: %s, Path: %s",
            fileProps.getProperty("bucket"), fileProps.getProperty("filepath")));
    Blob blob =
        storage.get(BlobId.of(fileProps.getProperty("bucket"), fileProps.getProperty("filepath")));
    if (filePath.endsWith(".mojo")) {
      log.info(String.format("Downloading Mojo to: %s/pipeline.mojo", mojoDownloadPath));
      blob.downloadTo(Paths.get(mojoDownloadPath, "pipeline.mojo"));
    } else if (filePath.endsWith(".sig")) {
      log.info(String.format("Downloading license to: %s/license.sig", licenseDownloadPath));
      blob.downloadTo(Paths.get(licenseDownloadPath, "license.sig"));
    } else {
      String errMsg = "Path provided: %s, is not used for this scorer. Not downloading";
      log.warn(String.format(errMsg, filePath));
    }
  }

  private Storage initiateStorageClient() {
    return StorageOptions.getDefaultInstance().getService();
  }

  private Properties parseGcsPath(String gcsPath) {
    Properties properties = new Properties();
    String gcsPathNoPrefix = gcsPath.replace("gs://", "");
    List<String> gcsPathArray = Splitter.on('/').splitToList(gcsPathNoPrefix);
    properties.setProperty("bucket", gcsPathArray.get(0));
    properties.setProperty("filepath", gcsPathNoPrefix.replace(gcsPathArray.get(0) + "/", ""));
    return properties;
  }

  private String getFromEnv(String envVar) {
    if (System.getenv(envVar) == null) {
      throw new RuntimeException(
          String.format("Error: required environment variable: %s, is not set", envVar));
    }
    return System.getenv(envVar);
  }

  private void ensureDirectoryExists(String dirPath) {
    File dir = new File(dirPath);
    dir.mkdir();
  }
}
