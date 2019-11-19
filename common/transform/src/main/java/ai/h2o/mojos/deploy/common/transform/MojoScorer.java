package ai.h2o.mojos.deploy.common.transform;

import ai.h2o.mojos.deploy.common.rest.model.Model;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.lic.LicenseException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * H2O DAI mojo scorer.
 *
 * <p>The scorer code is shared for all mojo deployments and is only parameterized by the
 * {@code mojo.path} property to define the mojo to use.
 */
public class MojoScorer {

  private static final Logger log = LoggerFactory.getLogger(MojoScorer.class);

  private static final String MOJO_PIPELINE_PATH_PROPERTY = "mojo.path";
  private static final String MOJO_PIPELINE_PATH = System.getProperty(MOJO_PIPELINE_PATH_PROPERTY);
  private static final MojoPipeline pipeline = loadMojoPipelineFromFile();

  private final RequestToMojoFrameConverter requestConverter;
  private final MojoFrameToResponseConverter responseConverter;
  private final MojoPipelineToModelInfoConverter modelInfoConverter;
  private final CsvToMojoFrameConverter csvConverter;

  /**
   * MojoScorer class initializer, requires below parameters.
   *
   * @param requestConverter {@link RequestToMojoFrameConverter}
   * @param responseConverter {@link MojoFrameToResponseConverter}
   * @param modelInfoConverter {@link MojoPipelineToModelInfoConverter}
   * @param csvConverter {@link CsvToMojoFrameConverter}
   */
  public MojoScorer(
      RequestToMojoFrameConverter requestConverter,
      MojoFrameToResponseConverter responseConverter,
      MojoPipelineToModelInfoConverter modelInfoConverter,
      CsvToMojoFrameConverter csvConverter) {
    this.requestConverter = requestConverter;
    this.responseConverter = responseConverter;
    this.modelInfoConverter = modelInfoConverter;
    this.csvConverter = csvConverter;
  }

  /**
   * Method to score an incoming request of type {@link ScoreRequest}.
   *
   * @param request {@link ScoreRequest}
   * @return response {@link ScoreResponse}
   */
  public ScoreResponse score(ScoreRequest request) {
    MojoFrame requestFrame = requestConverter.apply(request, pipeline.getInputFrameBuilder());
    MojoFrame responseFrame = doScore(requestFrame);
    ScoreResponse response = responseConverter.apply(responseFrame, request);
    response.id(pipeline.getUuid());
    return response;
  }

  /**
   * Method to score a csv file on path provided as part of request {@link ScoreRequest} payload.
   *
   * @param csvFilePath {@link String} path to csv to score. MUST exist locally.
   * @return response {@link ScoreResponse}
   * @throws IOException if csv file does not exist on local path, IOException will be thrown
   */
  public ScoreResponse scoreCsv(String csvFilePath) throws IOException {
    MojoFrame requestFrame;
    try (InputStream csvStream = getInputStream(csvFilePath)) {
      requestFrame = csvConverter.apply(csvStream, pipeline.getInputFrameBuilder());
    }
    MojoFrame responseFrame = doScore(requestFrame);
    ScoreResponse response = responseConverter.apply(responseFrame, new ScoreRequest());
    response.id(pipeline.getUuid());
    return response;
  }

  private static InputStream getInputStream(String filePath) throws IOException {
    File csvFile = new File(filePath);
    if (!csvFile.isFile()) {
      throw new FileNotFoundException(
          String.format("Could not find the input CSV file: %s", filePath));
    }
    return new FileInputStream(filePath);
  }

  private static MojoFrame doScore(MojoFrame requestFrame) {
    log.debug(
        "Input has {} rows, {} columns: {}",
        requestFrame.getNrows(),
        requestFrame.getNcols(),
        Arrays.toString(requestFrame.getColumnNames()));
    MojoFrame responseFrame = pipeline.transform(requestFrame);
    log.debug(
        "Response has {} rows, {} columns: {}",
        responseFrame.getNrows(),
        responseFrame.getNcols(),
        Arrays.toString(responseFrame.getColumnNames()));
    return responseFrame;
  }

  public String getModelId() {
    return pipeline.getUuid();
  }

  public MojoPipeline getPipeline() {
    return pipeline;
  }

  public Model getModelInfo() {
    return modelInfoConverter.apply(pipeline);
  }

  private static MojoPipeline loadMojoPipelineFromFile() {
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(MOJO_PIPELINE_PATH),
        "Path to mojo pipeline not specified, set the %s property.",
        MOJO_PIPELINE_PATH_PROPERTY);
    log.info("Loading Mojo pipeline from path {}", MOJO_PIPELINE_PATH);
    File mojoFile = new File(MOJO_PIPELINE_PATH);
    if (!mojoFile.isFile()) {
      ClassLoader classLoader = MojoScorer.class.getClassLoader();
      URL resourcePath = classLoader.getResource(MOJO_PIPELINE_PATH);
      if (resourcePath != null) {
        mojoFile = new File(resourcePath.getFile());
      }
    }
    if (!mojoFile.isFile()) {
      throw new RuntimeException("Could not load mojo");
    }
    try {
      MojoPipeline mojoPipeline = MojoPipeline.loadFrom(mojoFile.getPath());
      log.info("Mojo pipeline successfully loaded ({}).", mojoPipeline.getUuid());
      return mojoPipeline;
    } catch (IOException e) {
      throw new RuntimeException("Unable to load mojo", e);
    } catch (LicenseException e) {
      throw new RuntimeException("License file not found", e);
    }
  }
}
