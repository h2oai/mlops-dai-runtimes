package ai.h2o.mojos.deploy.aws.lambda;

import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.deploy.common.transform.MojoFrameToScoreResponseConverter;
import ai.h2o.mojos.deploy.common.transform.RequestChecker;
import ai.h2o.mojos.deploy.common.transform.SampleRequestBuilder;
import ai.h2o.mojos.deploy.common.transform.ScoreRequestFormatException;
import ai.h2o.mojos.deploy.common.transform.ScoreRequestToMojoFrameConverter;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.lic.LicenseException;
import ai.h2o.mojos.runtime.readers.MojoPipelineReaderBackendFactory;
import ai.h2o.mojos.runtime.readers.MojoReaderBackend;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/*
 * AWS lambda request handler that implements scoring using a H2O DAI mojo.
 *
 * <p>The scorer code is shared for all mojo deployments and is only parameterized by environment
 * variables that define, e.g., the location of the mojo file in AWS S3.
 */
public final class MojoScorer {
  private static final String DEPLOYMENT_S3_BUCKET_NAME =
      System.getenv("DEPLOYMENT_S3_BUCKET_NAME");
  private static final String MOJO_S3_OBJECT_KEY = System.getenv("MOJO_S3_OBJECT_KEY");
  private static final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
  private static final Object pipelineLock = new Object();
  private static MojoPipeline pipeline;

  private final ScoreRequestToMojoFrameConverter requestConverter =
      new ScoreRequestToMojoFrameConverter();
  private final MojoFrameToScoreResponseConverter responseConverter =
      new MojoFrameToScoreResponseConverter();
  private final RequestChecker requestChecker = new RequestChecker(new SampleRequestBuilder());

  /** Processes a single {@link ScoreRequest} in the given AWS Lambda {@link Context}. */
  public ScoreResponse score(ScoreRequest request, Context context)
      throws IOException, LicenseException, ScoreRequestFormatException {
    LambdaLogger logger = context.getLogger();
    logger.log("Got scoring request");
    MojoPipeline mojoPipeline = getMojoPipeline(logger);
    requestChecker.verify(request, mojoPipeline.getInputMeta());
    logger.log("Scoring request verified");
    MojoFrame requestFrame = requestConverter.apply(request, mojoPipeline.getInputFrameBuilder());
    logger.log(
        String.format(
            "Input has %d rows, %d columns: %s",
            requestFrame.getNrows(),
            requestFrame.getNcols(),
            Arrays.toString(requestFrame.getColumnNames())));
    MojoFrame responseFrame = mojoPipeline.transform(requestFrame);
    logger.log(
        String.format(
            "Response has %d rows, %d columns: %s",
            responseFrame.getNrows(),
            responseFrame.getNcols(),
            Arrays.toString(responseFrame.getColumnNames())));

    ScoreResponse response = responseConverter.apply(responseFrame, request);
    response.id(mojoPipeline.getUuid());
    return response;
  }

  private static MojoPipeline getMojoPipeline(LambdaLogger logger)
      throws IOException, LicenseException {
    synchronized (pipelineLock) {
      if (pipeline == null) {
        pipeline = loadMojoPipelineFromS3(logger);
      }
      return pipeline;
    }
  }

  private static MojoPipeline loadMojoPipelineFromS3(LambdaLogger logger)
      throws IOException, LicenseException {
    try (S3Object s3Object = s3Client.getObject(DEPLOYMENT_S3_BUCKET_NAME, MOJO_S3_OBJECT_KEY);
        InputStream mojoInput = s3Object.getObjectContent()) {
      logger.log(
          String.format(
              "Loading Mojo pipeline from S3 object %s/%s",
              DEPLOYMENT_S3_BUCKET_NAME, MOJO_S3_OBJECT_KEY));
      MojoReaderBackend mojoReaderBackend =
          MojoPipelineReaderBackendFactory.createReaderBackend(mojoInput);
      MojoPipeline mojoPipeline = MojoPipeline.loadFrom(mojoReaderBackend);
      logger.log(String.format("Mojo pipeline successfully loaded (%s).", mojoPipeline.getUuid()));
      return mojoPipeline;
    }
  }
}
