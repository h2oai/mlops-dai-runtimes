
package ai.h2o.dia.deploy.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.InputStream;
import java.io.OutputStream;

/*
 * AWS lambda request handler that implements scoring using a H2O DAI mojo.
 *
 * <p>The scorer code is shared for all mojo deployments and is only parameterized by environment variables that define,
 * e.g., the location of the mojo file in AWS S3.
 */
public class MojoScorer implements RequestStreamHandler {

    // TODO(osery): Load mojo path, from env variable so that the same lambda code can be deployed without being
    // specific to any particular mojo.

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        LambdaLogger logger = context.getLogger();
        // TODO(osery): Implement scoring.
        logger.log("Lambda called, but not implemented yet.");
        throw new AssertionError("Scoring not implemented yet.");
    }
}
