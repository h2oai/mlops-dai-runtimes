package ai.h2o.mojos.deploy.local.rest.controller;

import ai.h2o.mojos.deploy.common.rest.model.Model;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.deploy.common.transform.MojoFrameToResponseConverter;
import ai.h2o.mojos.deploy.common.transform.MojoPipelineToModelInfoConverter;
import ai.h2o.mojos.deploy.common.transform.RequestToMojoFrameConverter;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.lic.LicenseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
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

    private static final String MOJO_PIPELINE_PATH = System.getenv("MOJO_PATH");
    private static final Object pipelineLock = new Object();
    private static MojoPipeline pipeline;

    private final RequestToMojoFrameConverter requestConverter;
    private final MojoFrameToResponseConverter responseConverter;
    private final MojoPipelineToModelInfoConverter modelInfoConverter;

    @Autowired
    public MojoScorer(RequestToMojoFrameConverter requestConverter, MojoFrameToResponseConverter responseConverter,
                      MojoPipelineToModelInfoConverter modelInfoConverter) {
        this.requestConverter = requestConverter;
        this.responseConverter = responseConverter;
        this.modelInfoConverter = modelInfoConverter;
    }

    ScoreResponse score(ScoreRequest request) {
        log.info("Got scoring request: {}", request);
        MojoPipeline mojoPipeline = getMojoPipeline();
        MojoFrame requestFrame = requestConverter.apply(request, mojoPipeline.getInputMeta());
        log.info("Input has {} rows, {} columns: {}", requestFrame.getNrows(), requestFrame.getNcols(),
                Arrays.toString(requestFrame.getColumnNames()));
        MojoFrame responseFrame = mojoPipeline.transform(requestFrame);
        log.info("Response has {} rows, {} columns: {}", responseFrame.getNrows(),
                responseFrame.getNcols(), Arrays.toString(responseFrame.getColumnNames()));

        ScoreResponse response = responseConverter.apply(responseFrame, request);
        response.id(mojoPipeline.getUuid());
        return response;
    }

    String getModelId() {
        return getMojoPipeline().getUuid();
    }

    Model getModelInfo() {
        return modelInfoConverter.apply(getMojoPipeline());
    }

    private static MojoPipeline getMojoPipeline() {
        synchronized (pipelineLock) {
            if (pipeline == null) {
                pipeline = loadMojoPipelineFromFile();
            }
            return pipeline;
        }
    }

    private static MojoPipeline loadMojoPipelineFromFile() {
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
