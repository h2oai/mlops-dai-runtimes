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

    @SuppressWarnings("unused")
    @RequestMapping("/invocations")
    public HashMap<String,String> invocations(HttpServletRequest request) {
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
                catch (Exception ignore) {
                }
            }
        }
        catch (Exception ignore) {
        }

        return sv.invocations(inputs);
    }

    @SuppressWarnings("unused")
    @RequestMapping("/ping")
    public void ping(HttpServletRequest request, HttpServletResponse response) {
        if (sv.ready) {
            response.setStatus(HttpServletResponse.SC_OK);
        }
        else {
            response.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
        }
    }
}
