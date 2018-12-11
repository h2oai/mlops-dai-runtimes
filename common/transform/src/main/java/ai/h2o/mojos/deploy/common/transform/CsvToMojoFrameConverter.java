package ai.h2o.mojos.deploy.common.transform;

import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;
import ai.h2o.mojos.runtime.utils.SimpleCSV;

import java.io.IOException;
import java.io.InputStream;

/**
 * Converts the CSV from {@link InputStream} to the {@link MojoFrame} for scoring.
 */
public class CsvToMojoFrameConverter {
    public MojoFrame apply(InputStream inputStream, MojoFrameMeta meta) throws IOException {
        SimpleCSV csv = SimpleCSV.read(inputStream);
        MojoFrameBuilder frameBuilder = new MojoFrameBuilder(meta);

        String[] labels = csv.getLabels();
        String[][] data = csv.getData();
        MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();
        for (String[] row : data) {
            for (int c = 0; c < row.length; c += 1) {
                rowBuilder.setValue(labels[c], row[c]);
            }
            // Reuse the builder.
            rowBuilder = frameBuilder.addRow(rowBuilder);
        }
        return frameBuilder.toMojoFrame();
    }
}
