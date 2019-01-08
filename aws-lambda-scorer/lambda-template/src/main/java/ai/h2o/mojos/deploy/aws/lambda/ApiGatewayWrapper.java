package ai.h2o.mojos.deploy.aws.lambda;

import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.deploy.common.transform.ScoreRequestFormatException;
import ai.h2o.mojos.runtime.lic.LicenseException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Wraps {@link MojoScorer} so that it can be called via the AWS API gateway.
 *
 * <p>Note that AWS API Gateway strictly mandates the input and output formats that we need to adhere to here.
 * In addition, this class does error handling.
 *
 * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-create-api-as-simple-proxy-for-lambda.html">
 * Build an API Gateway API with Lambda Proxy Integration</a>
 */
public class ApiGatewayWrapper implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final MojoScorer mojoScorer = new MojoScorer();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            ScoreRequest scoreRequest = gson.fromJson(input.getBody(), ScoreRequest.class);
            ScoreResponse scoreResponse = mojoScorer.score(scoreRequest, context);
            return toSuccessResponse(gson.toJson(scoreResponse));
        } catch (JsonSyntaxException e) {
            return toErrorResponse(HttpStatus.SC_BAD_REQUEST, "Malformed JSON request", e);
        } catch (ScoreRequestFormatException e) {
            return toRequestFormatResponse(e);
        } catch (IOException e) {
            return toErrorResponse(HttpStatus.SC_NOT_FOUND, "Mojo pipeline file not found", e);
        } catch (LicenseException e) {
            return toErrorResponse(HttpStatus.SC_SERVICE_UNAVAILABLE, "Could not find a valid license file", e);
        } catch (Exception e) {
            return toErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unexpected error while scoring", e);
        }
    }

    private static APIGatewayProxyResponseEvent toErrorResponse(int statusCode, String message, Exception exception) {
        String exceptionAsString = getExceptionAsString(exception);
        return new APIGatewayProxyResponseEvent().withStatusCode(statusCode).withBody(
                String.format("%s\n\nError details:\n%s\n", message, exceptionAsString));
    }

    private static String getExceptionAsString(Exception exception) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private APIGatewayProxyResponseEvent toRequestFormatResponse(ScoreRequestFormatException exception) {
        String exceptionAsString = getExceptionAsString(exception);
        String example = gson.toJson(exception.getExampleRequest());
        return new APIGatewayProxyResponseEvent().withStatusCode(HttpStatus.SC_BAD_REQUEST).withBody(
                String.format("Malformed scoring request.\n\nError details:\n%s\n\nExample request:\n%s\n",
                        exceptionAsString, example));
    }

    private static APIGatewayProxyResponseEvent toSuccessResponse(String body) {
        return new APIGatewayProxyResponseEvent().withStatusCode(HttpStatus.SC_OK).withBody(body);
    }
}
