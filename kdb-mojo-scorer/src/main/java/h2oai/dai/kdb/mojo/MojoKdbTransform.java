package h2oai.dai.kdb.mojo;

import deps.javakdb.c;
import deps.javakdb.c.Flip;
import static deps.javakdb.c.n;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.lic.LicenseException;
import java.io.UnsupportedEncodingException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.io.IOException;

public class MojoKdbTransform {

    private static final Logger log = LoggerFactory.getLogger(MojoKdbTransform.class);

    public static MojoPipeline loadMojo(String pathToMojoFile) throws IOException, LicenseException {
        return MojoPipeline.loadFrom(pathToMojoFile);
    }

    public static MojoFrame createMojoFrameFromKdbFlip(MojoPipeline model, Flip kdbFlipTable) throws UnsupportedEncodingException {
        String[] colNames = kdbFlipTable.x;
        Object[] colData = kdbFlipTable.y;
        MojoFrameBuilder frameBuilder = model.getInputFrameBuilder();
        for (int row = 0; row < n(colData[0]); row++) {
            MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();
            for (int col = 0; col < colNames.length; col++) {
                if (colNames[col].equals("time") || colNames[col].equals("sym")) {
                    log.info("skipping KDB enforced columns that are not part of the mojo");
                } else {
                    rowBuilder.setValue(colNames[col], c.at(colData[col], row).toString());
                }
            }
            frameBuilder.addRow(rowBuilder);
        }
        return frameBuilder.toMojoFrame();
    }

    public static Object[] generateMojoPredictionPublishObject(String qPubTab, MojoFrame oframe, Flip kdbFlipTable) {
        Object[] colData = kdbFlipTable.y;
        Float[] predOut = null;
        Object[] predData = new Object[colData.length + 1];
        System.arraycopy(colData, 0, predData, 0, colData.length);
        for (int r = 0; r < oframe.getNcols(); r++) {
            String result = oframe.getColumnName(r);
            String[] prediction = oframe.getColumn(r).getDataAsStrings();
            Float[] predOutLoc = new Float[prediction.length];
            for (int a=0; a < prediction.length; a++) {
                log.info("Prediction #" + a + " of batch:");
                log.info("Column: " + result + " Prediction: " + prediction[a]);
                predOutLoc[a] = Float.parseFloat(prediction[a]);
            }
            predOut = predOutLoc;
        }
        predData[colData.length] = predOut;
        return new Object[] {".u.upd", qPubTab, predData};
    }
}