package ai.h2o.mojos.deploy.local.rest.controller;

import ai.h2o.mojos.deploy.common.rest.model.Model;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.deploy.common.transform.CsvToMojoFrameConverter;
import ai.h2o.mojos.deploy.common.transform.MojoFrameToResponseConverter;
import ai.h2o.mojos.deploy.common.transform.MojoPipelineToModelInfoConverter;
import ai.h2o.mojos.deploy.common.transform.RequestToMojoFrameConverter;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.lic.LicenseException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.util.Arrays;

/*
 * AWS lambda request handler that implements scoring using a H2O DAI mojo.
 *
 * <p>The scorer code is shared for all mojo deployments and is only parameterized by environment variables that define,
 * e.g., the location of the mojo file in AWS S3.
 */
@Component
class MojoScorer {
    private static final Logger log = LoggerFactory.getLogger(ModelsApiController.class);
    private static final String MOJO_PIPELINE_PATH_VARIABLE = "MOJO_PATH";
    private static final String MOJO_PIPELINE_PATH = System.getenv(MOJO_PIPELINE_PATH_VARIABLE);
    private static final MojoPipeline pipeline = loadMojoPipelineFromFile();

    private final RequestToMojoFrameConverter requestConverter;
    private final MojoFrameToResponseConverter responseConverter;
    private final MojoPipelineToModelInfoConverter modelInfoConverter;
    private final CsvToMojoFrameConverter csvConverter;

    @Autowired
    public MojoScorer(RequestToMojoFrameConverter requestConverter, MojoFrameToResponseConverter responseConverter,
                      MojoPipelineToModelInfoConverter modelInfoConverter, CsvToMojoFrameConverter csvConverter) {
        this.requestConverter = requestConverter;
        this.responseConverter = responseConverter;
        this.modelInfoConverter = modelInfoConverter;
        this.csvConverter = csvConverter;
    }

    ScoreResponse score(ScoreRequest request) {
        log.info("Got scoring request: {}", request);
        MojoFrame requestFrame = requestConverter.apply(request, pipeline.getInputMeta());
        MojoFrame responseFrame = doScore(requestFrame);
        ScoreResponse response = responseConverter.apply(responseFrame, request);
        response.id(pipeline.getUuid());
        return response;
    }

    ScoreResponse scoreCsv(String csvFilePath) throws IOException {
        log.info("Got scoring request for CSV: {}", csvFilePath);
        MojoFrame requestFrame;
        try (InputStream csvStream = getInputStream(csvFilePath)) {
            requestFrame = csvConverter.apply(csvStream, pipeline.getInputMeta());
        }
        MojoFrame responseFrame = doScore(requestFrame);
        ScoreResponse response = responseConverter.apply(responseFrame, new ScoreRequest());
        response.id(pipeline.getUuid());
        return response;
    }

    private static InputStream getInputStream(String filePath) throws IOException {
        File csvFile = new File(filePath);
        if (!csvFile.isFile()) {
            throw new FileNotFoundException(String.format("Could not find the input CSV file: %s", filePath));
        }
        return new FileInputStream(filePath);
    }

    private static MojoFrame doScore(MojoFrame requestFrame) {
        log.info("Input has {} rows, {} columns: {}", requestFrame.getNrows(), requestFrame.getNcols(),
                Arrays.toString(requestFrame.getColumnNames()));
        MojoFrame responseFrame = pipeline.transform(requestFrame);
        log.info("Response has {} rows, {} columns: {}", responseFrame.getNrows(),
                responseFrame.getNcols(), Arrays.toString(responseFrame.getColumnNames()));
        return responseFrame;
    }

    String getModelId() {
        return pipeline.getUuid();
    }

    Model getModelInfo() {
        return modelInfoConverter.apply(pipeline);
    }

    private static MojoPipeline loadMojoPipelineFromFile() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(MOJO_PIPELINE_PATH),
                "Path to mojo pipeline not specified, set the %s environment variable.",
                MOJO_PIPELINE_PATH_VARIABLE);
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
            log.error("Could not load mojo", e);
            throw new RuntimeException("Unable to load mojo", e);
        } catch (LicenseException e) {
            log.error("No License File", e);
            throw new RuntimeException("License file not found", e);
        }
    }
}
