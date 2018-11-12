
package ai.h2o.dia.deploy.aws.lambda;

import ai.h2o.dai.deploy.aws.lambda.model.ScoreRequest;
import ai.h2o.dai.deploy.aws.lambda.model.ScoreResponse;
import ai.h2o.mojos.runtime.MojoPipeline;
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

/*
 * AWS lambda request handler that implements scoring using a H2O DAI mojo.
 *
 * <p>The scorer code is shared for all mojo deployments and is only parameterized by environment variables that define,
 * e.g., the location of the mojo file in AWS S3.
 */
public final class MojoScorer {
    private static final String DEPLOYMENT_S3_BUCKET_NAME = System.getenv("DEPLOYMENT_S3_BUCKET_NAME");
    private static final String MOJO_S3_OBJECT_KEY = System.getenv("MOJO_S3_OBJECT_KEY");
    private static final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

    public ScoreResponse score(ScoreRequest request, Context context) throws IOException, LicenseException {
        LambdaLogger logger = context.getLogger();
        logger.log(String.format("Loading Mojo pipeline from S3 object %s/%s", DEPLOYMENT_S3_BUCKET_NAME,
                MOJO_S3_OBJECT_KEY));
        MojoPipeline mojoPipeline = loadMojoPipelineFromS3();
        logger.log(String.format("Mojo pipeline successfully loaded (%s).", mojoPipeline));

        // TODO(osery):
        //  - Score the input and return the result.
        //  - Map errors to HTTP error codes.
        throw new AssertionError("Mojo pipeline loaded but scoring is not implemented yet.");
    }

    private static MojoPipeline loadMojoPipelineFromS3() throws IOException, LicenseException {
        try (
                S3Object s3Object = s3Client.getObject(DEPLOYMENT_S3_BUCKET_NAME, MOJO_S3_OBJECT_KEY);
                InputStream mojoInput = s3Object.getObjectContent()
        ) {
            MojoReaderBackend mojoReaderBackend = MojoPipelineReaderBackendFactory.createReaderBackend(mojoInput);
            return MojoPipeline.loadFrom(mojoReaderBackend);
        }
    }
}
