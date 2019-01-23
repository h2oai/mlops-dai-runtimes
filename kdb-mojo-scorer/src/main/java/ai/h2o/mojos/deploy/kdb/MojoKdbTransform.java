package ai.h2o.mojos.deploy.kdb;

import kx.c;
import kx.c.Flip;
import static kx.c.n;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import java.io.UnsupportedEncodingException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.Arrays;
import java.util.List;


class MojoKdbTransform {

    private static final Logger log = LoggerFactory.getLogger(MojoKdbTransform.class);

    static MojoFrame createMojoFrameFromKdbFlip(MojoPipeline model, Flip kdbFlipTable, String dropCols) throws UnsupportedEncodingException {
        String[] colNames = kdbFlipTable.x;
        Object[] colData = kdbFlipTable.y;
        String[] colsToDrop = dropCols.split(",");
        List<String> dropColsList = Arrays.asList(colsToDrop);
        MojoFrameBuilder frameBuilder = model.getInputFrameBuilder();
        for (int row = 0; row < n(colData[0]); row++) {
            MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();
            for (int col = 0; col < colNames.length; col++) {
                if (dropColsList.contains(colNames[col])) {
                    log.info("skipping user specified column to drop: {}", colNames[col]);
                } else {
                    rowBuilder.setValue(colNames[col], c.at(colData[col], row).toString());
                }
            }
            frameBuilder.addRow(rowBuilder);
        }
        return frameBuilder.toMojoFrame();
    }

    static Object[] generateMojoPredictionPublishObject(String qPubTab, MojoFrame oframe, Flip kdbFlipTable) {
        Object[] colData = kdbFlipTable.y;
        Float[] predOut = null;
        Object[] predData = new Object[colData.length + 1];
        System.arraycopy(colData, 0, predData, 0, colData.length);
        for (int r = 0; r < oframe.getNcols(); r++) {
            String result = oframe.getColumnName(r);
            String[] prediction = oframe.getColumn(r).getDataAsStrings();
            Float[] predOutLoc = new Float[prediction.length];
            for (int a=0; a < prediction.length; a++) {
                log.info("Prediction #{} of batch:", a);
                log.info("Column: {}, Prediction: {}", result, prediction[a]);
                predOutLoc[a] = Float.parseFloat(prediction[a]);
            }
            predOut = predOutLoc;
        }
        predData[colData.length] = predOut;
        return new Object[] {".u.upd", qPubTab, predData};
    }
}