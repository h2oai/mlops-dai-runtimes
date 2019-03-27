package ai.h2o.mojos.deploy.sagemaker.hosted;

import ai.h2o.mojos.runtime.utils.SimpleCSV;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;

import java.util.HashMap;

@Component
public class Serve {
    // Used to prevent REST API requests before the server is initialized and ready to handle them.
    volatile boolean ready = false;

    private volatile MojoPipeline model;

    private void setReady() {
        ready = true;
    }

    @Autowired
    public Serve() {
    }

    /**
     * Call this immediately after the constructor above.
     * Since this object is used as an Autowired Spring object, the constructor call is not actually visible.
     */
    void init() throws Exception {
        model = MojoPipeline.loadFrom("pipeline.mojo");
    }

    HashMap<String,String> invocations(HashMap<String,String> inputs) {
        MojoFrameBuilder frameBuilder = model.getInputFrameBuilder();
        MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();
        for (String key : inputs.keySet()) {
            String value = inputs.get(key);
            rowBuilder.setValue(key, value);
        }
        frameBuilder.addRow(rowBuilder);
        MojoFrame iframe = frameBuilder.toMojoFrame();

        MojoFrame oframe = model.transform(iframe);

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
