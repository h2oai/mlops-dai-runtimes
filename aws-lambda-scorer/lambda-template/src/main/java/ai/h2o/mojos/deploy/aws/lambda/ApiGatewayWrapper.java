package ai.h2o.mojos.deploy.aws.lambda;

import ai.h2o.mojos.deploy.aws.lambda.model.ScoreRequest;
import ai.h2o.mojos.deploy.aws.lambda.model.ScoreResponse;
import ai.h2o.mojos.runtime.lic.LicenseException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpStatus;

import java.io.IOException;

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
    private final Gson gson = new Gson();
    private final MojoScorer mojoScorer = new MojoScorer();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            ScoreRequest scoreRequest = gson.fromJson(input.getBody(), ScoreRequest.class);
            ScoreResponse scoreResponse = mojoScorer.score(scoreRequest, context);
            return toSuccessResponse(gson.toJson(scoreResponse));
        } catch (JsonSyntaxException e) {
            return toErrorResponse(HttpStatus.SC_BAD_REQUEST, e);
        } catch (IOException e) {
            return toErrorResponse(HttpStatus.SC_NOT_FOUND, e);
        } catch (LicenseException e) {
            return toErrorResponse(HttpStatus.SC_SERVICE_UNAVAILABLE, e);
        } catch (Exception e) {
            return toErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    private static APIGatewayProxyResponseEvent toErrorResponse(int statusCode, Exception exception) {
        return new APIGatewayProxyResponseEvent().withStatusCode(statusCode).withBody(exception.toString());
    }

    private static APIGatewayProxyResponseEvent toSuccessResponse(String body) {
        return new APIGatewayProxyResponseEvent().withStatusCode(HttpStatus.SC_OK).withBody(body);
    }
}
