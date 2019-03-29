package ai.h2o.mojos.deploy.sagemaker.hosted;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonParser;

@SuppressWarnings("unused")
@RestController
@CrossOrigin
public class ServeController {
    @Autowired
    private Serve sv = null;

    /**
     * Performs an inference.
     *
     * The request payload body is a single json object representing one row with string names and string values.
     * Numerical values should still be represented as a string.  e.g.  "1" or "1.234".
     *
     * The response payload body is a single json object with string names and string values.
     *
     * The HTTP response is one of:
     *     SC_OK (200)                    -- A successful prediction was made
     *     SC_BAD_REQUEST (400)           -- Input error
     *     SC_INTERNAL_SERVER_ERROR (500) -- Server-side error
     *
     * See the SageMaker documentation here:
     *     See https://docs.aws.amazon.com/sagemaker/latest/dg/your-algorithms-inference-code.html
     *
     * @param request One input row in json format
     * @param response One output row in json format
     * @return If 200 response, one prediction in json format.
     */
    @SuppressWarnings("unused")
    @RequestMapping("/invocations")
    public HashMap<String,String> invocations(HttpServletRequest request, HttpServletResponse response) {
        @SuppressWarnings("unchecked")
        HashMap<String,String> inputs = new HashMap();

        try {
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(request.getReader());
            JsonObject obj = element.getAsJsonObject();
            Set<Map.Entry<String, JsonElement>> entries = obj.entrySet();
            for (Map.Entry<String, JsonElement> entry : entries) {
                try {
                    String key = entry.getKey();
                    String value = entry.getValue().getAsString();
                    inputs.put(key, value);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return null;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        try {
            return sv.invocations(inputs);
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    /**
     * Perform a health/readiness check.
     *
     * See the SageMaker documentation here:
     *     See https://docs.aws.amazon.com/sagemaker/latest/dg/your-algorithms-inference-code.html
     *
     * @param response 200 if the service is available; 504 otherwise.
     */
    @SuppressWarnings("unused")
    @RequestMapping("/ping")
    public void ping(HttpServletResponse response) {
        if (sv.isReady()) {
            response.setStatus(HttpServletResponse.SC_OK);
        }
        else {
            response.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
        }
    }
}
