package ai.h2o.mojos.deploy.common.transform;

import ai.h2o.mojos.deploy.common.rest.model.Model;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.deploy.common.rest.model.ShapleyResponse;
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

  // note the mojo pipeline need to be reloaded here as we have a constrain from java mojo
  // both SHAP values and predictions cannot be provided with the same pipeline
  // Link: https://github.com/h2oai/mojo2/blob/7a1ab76b09f056334842a5b442ff89859aabf518/doc/shap.md
  private static final MojoPipeline pipelineShapley = loadMojoPipelineFromFile();


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
    pipelineShapley.setShapPredictContrib(true);
  }

  /**
   * Method to score an incoming request of type {@link ScoreRequest}.
   *
   * @param request {@link ScoreRequest}
   * @return response {@link ScoreResponse}
   */
  public ScoreResponse score(ScoreRequest request) {
    return getScoreResponse(request, false);
  }

  /**
   * Method to score an incoming request of type {@link ScoreRequest}
   * with shapley contributions if needed.
   *
   * @param request {@link ScoreRequest}
   * @return response {@link ScoreResponse}
   */
  public ScoreResponse getScoreResponse(ScoreRequest request, Boolean shapleyResults) {
    MojoFrame requestFrame = requestConverter.apply(request, pipeline.getInputFrameBuilder());
    MojoFrame responseFrame = doScore(requestFrame, false);
    ScoreResponse response = responseConverter.apply(responseFrame, request);
    response.id(pipeline.getUuid());

    // set shapley contributions if requested
    if (Boolean.TRUE.equals(shapleyResults)) {
      response.setInputShapleyContributions(getShapleyResponse(request));
    }
    return response;
  }

  /**
   * Method to get shapley values for an incoming request of type {@link ScoreRequest}.
   *
   * @param request {@link ScoreRequest}
   * @return response {@link ShapleyResponse}
   */
  private ShapleyResponse getShapleyResponse(ScoreRequest request) {
    MojoFrame requestFrame = requestConverter
            .apply(request, pipelineShapley.getInputFrameBuilder());
    MojoFrame shapleyResponseFrame = doScore(requestFrame, true);
    return responseConverter.getShapleyResponse(shapleyResponseFrame);
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
    MojoFrame responseFrame = doScore(requestFrame, false);
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

  private static MojoFrame doScore(MojoFrame requestFrame, boolean isShapleyContribution) {
    log.debug(
            "Input has {} rows, {} columns: {}",
            requestFrame.getNrows(),
            requestFrame.getNcols(),
            Arrays.toString(requestFrame.getColumnNames()));
    MojoFrame responseFrame;
    if (isShapleyContribution) {
      responseFrame = pipelineShapley.transform(requestFrame);
    } else {
      responseFrame = pipeline.transform(requestFrame);
    }
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
