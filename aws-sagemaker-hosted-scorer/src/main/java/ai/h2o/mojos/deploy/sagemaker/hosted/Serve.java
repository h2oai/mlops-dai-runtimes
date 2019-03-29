package ai.h2o.mojos.deploy.sagemaker.hosted;

import ai.h2o.mojos.runtime.utils.SimpleCSV;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;

import java.io.File;
import java.util.HashMap;

@Component
public class Serve {
    // Used to prevent REST API requests before the server is initialized and ready to handle them.
    private volatile boolean ready = false;

    private void setReady() {
        ready = true;
    }

    boolean isReady() {
        return ready;
    }

    private volatile MojoPipeline model;

    @Autowired
    public Serve() {
    }

    /**
     * Call this immediately after the constructor above.
     * Since this object is used as an Autowired Spring object, the constructor call is not actually visible.
     */
    void init() throws Exception {
        String[] locationsToCheck = {
                "/opt/ml/model/pipeline.mojo",
                "./pipeline.mojo"
        };

        File f = null;
        for (String location : locationsToCheck) {
            File tmp = new File(location);
            if (tmp.exists()) {
                f = tmp;
                break;
            }
        }

        if (f == null) {
            System.out.println("ERROR: pipeline.mojo file not found");
            System.exit(1);
        }

        long startMillis = System.currentTimeMillis();
        model = MojoPipeline.loadFrom(f.getAbsolutePath());
        long endMillis = System.currentTimeMillis();
        long deltaMillis = endMillis - startMillis;
        System.out.println("INFO: MojoPipeline.loadFrom took " + deltaMillis + " millis");
    }

    HashMap<String,String> invocations(HashMap<String,String> inputs) {
        // Put the input variables into the iframe.
        MojoFrameBuilder frameBuilder = model.getInputFrameBuilder();
        MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();
        for (String key : inputs.keySet()) {
            String value = inputs.get(key);
            rowBuilder.setValue(key, value);
        }
        frameBuilder.addRow(rowBuilder);
        MojoFrame iframe = frameBuilder.toMojoFrame();

        // Make the prediction.  Prediction outputs go to the oframe.
        MojoFrame oframe = model.transform(iframe);

        // Move the oframe fields into a more standard Java object for the response.
        @SuppressWarnings("unchecked")
        HashMap<String,String> response = new HashMap();
        SimpleCSV outCsv = SimpleCSV.read(oframe);
        for (int i = 0; i < outCsv.getNCols(); i++) {
            String key = outCsv.getLabels()[i];
            String value = outCsv.getData()[0][i];
            response.put(key, value);
        }

        return response;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    void server() {
        setReady();

        while (true) {
            try {
                Thread.sleep(10000);
            }
            catch (Exception ignore) {}
        }
    }
}
