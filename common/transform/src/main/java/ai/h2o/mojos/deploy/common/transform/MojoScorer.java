package ai.h2o.mojos.deploy.common.transform;

import ai.h2o.mojos.deploy.common.rest.model.ContributionRequest;
import ai.h2o.mojos.deploy.common.rest.model.ContributionResponse;
import ai.h2o.mojos.deploy.common.rest.model.Model;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.deploy.common.rest.model.ScoringType;
import ai.h2o.mojos.deploy.common.rest.model.ShapleyType;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import ai.h2o.mojos.runtime.lic.LicenseException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * H2O DAI mojo scorer.
 *
 * <p>The scorer code is shared for all mojo deployments and is only parameterized by the
 * {@code mojo.path} property to define the mojo to use.
 */
public class MojoScorer {
  private static final String UNIMPLEMENTED_MESSAGE
          = "Shapley values for original features are not implemented yet";

  private static final Logger log = LoggerFactory.getLogger(MojoScorer.class);

  private static final String MOJO_PIPELINE_PATH_PROPERTY = "mojo.path";
  private static final String MOJO_PIPELINE_PATH = System.getProperty(MOJO_PIPELINE_PATH_PROPERTY);
  private static final MojoPipeline pipeline = loadMojoPipelineFromFile();

  // note the mojo pipeline need to be reloaded here as we have a constrain from java mojo
  // both SHAP values and predictions cannot be provided with the same pipeline
  // Link: https://github.com/h2oai/mojo2/blob/7a1ab76b09f056334842a5b442ff89859aabf518/doc/shap.md
  private static final MojoPipeline pipelineShapley = loadMojoPipelineFromFile();


  private final ScoreRequestToMojoFrameConverter scoreRequestConverter;
  private final MojoFrameToScoreResponseConverter scoreResponseConverter;
  private final MojoFrameToContributionResponseConverter contributionResponseConverter;
  private final ContributionRequestToMojoFrameConverter contributionRequestConverter;
  private final MojoPipelineToModelInfoConverter modelInfoConverter;
  private final CsvToMojoFrameConverter csvConverter;

  /**
   * MojoScorer class initializer, requires below parameters.
   *
   * @param scoreRequestConverter {@link ScoreRequestToMojoFrameConverter}
   * @param scoreResponseConverter {@link MojoFrameToScoreResponseConverter}
   * @param modelInfoConverter {@link MojoPipelineToModelInfoConverter}
   * @param csvConverter {@link CsvToMojoFrameConverter}
   */
  public MojoScorer(
      ScoreRequestToMojoFrameConverter scoreRequestConverter,
      MojoFrameToScoreResponseConverter scoreResponseConverter,
      ContributionRequestToMojoFrameConverter contributionRequestConverter,
      MojoFrameToContributionResponseConverter contributionResponseConverter,
      MojoPipelineToModelInfoConverter modelInfoConverter,
      CsvToMojoFrameConverter csvConverter) {
    this.scoreRequestConverter = scoreRequestConverter;
    this.scoreResponseConverter = scoreResponseConverter;
    this.contributionRequestConverter = contributionRequestConverter;
    this.contributionResponseConverter = contributionResponseConverter;
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
  public ScoreResponse scoreResponse(ScoreRequest request) {
    ScoreResponse response = score(request);
    ShapleyType requestedShapleyType = shapleyType(request.getShapleyResults());
    switch (requestedShapleyType) {
      case TRANSFORMED:
        response.setFeatureShapleyContributions(contributionResponse(request));
        return response;
      case ORIGINAL:
        throw new UnsupportedOperationException(UNIMPLEMENTED_MESSAGE);
      default:
        return response;
    }
  }

  private ScoreResponse score(ScoreRequest request) {
    MojoFrame requestFrame = scoreRequestConverter
            .apply(request, pipeline.getInputFrameBuilder());
    MojoFrame responseFrame = doScore(requestFrame);
    ScoreResponse response = scoreResponseConverter.apply(responseFrame, request);
    response.id(pipeline.getUuid());
    return response;
  }

  /**
   * Method to get shapley values for an incoming request of type {@link ScoreRequest}.
   *
   * @param request {@link ScoreRequest}
   * @return response {@link ContributionResponse}
   */
  private ContributionResponse contributionResponse(ScoreRequest request) {
    MojoFrame requestFrame = scoreRequestConverter
            .apply(request, pipelineShapley.getInputFrameBuilder());
    return contribution(requestFrame);
  }

  /**
   * Method to get shapley values for an incoming request of type {@link ContributionRequest}.
   *
   * @param request {@link ContributionRequest}
   * @return response {@link ContributionResponse}
   */
  public ContributionResponse contributionResponse(ContributionRequest request) {
    ShapleyType requestedShapleyType = shapleyType(request.getShapleyResults());
    switch (requestedShapleyType) {
      case TRANSFORMED:
        MojoFrame requestFrame = contributionRequestConverter
                .apply(request, pipelineShapley.getInputFrameBuilder());
        return contribution(requestFrame);
      case ORIGINAL:
        throw new UnsupportedOperationException(UNIMPLEMENTED_MESSAGE);
      default:
        throw new IllegalArgumentException(
                "Only ORIGINAL or TRANSFORMED are accepted enums values "
                        + "for Shapley results property");
    }
  }

  private ContributionResponse contribution(MojoFrame requestFrame) {
    MojoFrame contributionFrame = doShapleyContrib(requestFrame);

    MojoFrameMeta outputMeta = pipeline.getOutputMeta();
    ScoringType scoringType = scoringType(outputMeta.getColumns().size());
    List<String> outputGroupNames = getOutputGroups(outputMeta);

    if (ScoringType.CLASSIFICATION.equals(scoringType)) {
      return contributionResponseConverter.apply(contributionFrame, outputGroupNames);
    } else {
      return contributionResponseConverter.apply(contributionFrame);
    }
  }

  private List<String> getOutputGroups(MojoFrameMeta outputMeta) {
    int numberOutputColumns = outputMeta.getColumns().size();
    List<String> outputClass =  new ArrayList<>();
    for (int i = 0; i < numberOutputColumns; i++) {
      String outputClassName = outputMeta.getColumnName(i);
      String[] outputClassNameSplit = outputClassName.split("\\.");
      String refinedOutputClass = outputClassNameSplit[outputClassNameSplit.length - 1 ];
      outputClass.add(refinedOutputClass);
    }
    return outputClass;
  }

  // note this will be provided by mojo pipeline in the future
  private ScoringType scoringType(int outputColumnSize) {
    ScoringType scoringType = null;
    if (outputColumnSize > 2) {
      scoringType = ScoringType.CLASSIFICATION;
    } else if (outputColumnSize == 2) {
      scoringType = ScoringType.BINOMIAL;
    } else if (outputColumnSize == 1) {
      scoringType = ScoringType.REGRESSION;
    }
    return scoringType;
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
    ScoreResponse response = scoreResponseConverter.apply(responseFrame, new ScoreRequest());
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

  private static MojoFrame doShapleyContrib(MojoFrame requestFrame) {
    log.debug(
            "Input has {} rows, {} columns: {}",
            requestFrame.getNrows(),
            requestFrame.getNcols(),
            Arrays.toString(requestFrame.getColumnNames()));
    MojoFrame shapleyResponseFrame = pipelineShapley.transform(requestFrame);
    log.debug(
            "Response has {} rows, {} columns: {}",
            shapleyResponseFrame.getNrows(),
            shapleyResponseFrame.getNcols(),
            Arrays.toString(shapleyResponseFrame.getColumnNames()));
    return shapleyResponseFrame;
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

  private ShapleyType shapleyType(String requestedType) {
    if (ShapleyType.TRANSFORMED.toString().equalsIgnoreCase(requestedType)) {
      return ShapleyType.TRANSFORMED;
    } else if (ShapleyType.ORIGINAL.toString().equalsIgnoreCase(requestedType)) {
      return ShapleyType.ORIGINAL;
    }
    return ShapleyType.NONE;
  }
}
