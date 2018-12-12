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
        String[] labels = csv.getLabels();
        if (labels.length != meta.size()) {
            throw new IllegalArgumentException(
                    String.format("Mismatch between column counts of CSV file and the mojo (csv=%d, mojo=%d).",
                            labels.length, meta.size()));
        }

        MojoFrameBuilder frameBuilder = new MojoFrameBuilder(meta);
        MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();
        String[][] data = csv.getData();
        for (String[] row : data) {
            for (int c = 0; c < row.length; c++) {
                rowBuilder.setValue(labels[c], row[c]);
            }
            // Reuse the builder.
            rowBuilder = frameBuilder.addRow(rowBuilder);
        }
        return frameBuilder.toMojoFrame();
    }
}
