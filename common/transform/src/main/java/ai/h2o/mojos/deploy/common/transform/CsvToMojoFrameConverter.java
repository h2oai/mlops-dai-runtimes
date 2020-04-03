package ai.h2o.mojos.deploy.common.transform;

import static java.nio.charset.StandardCharsets.UTF_8;

import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;
import ai.h2o.mojos.runtime.utils.BatchedCsvMojoProcessor;
import com.opencsv.CSVReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;

/** Converts the CSV from {@link InputStream} to the {@link MojoFrame} for scoring. */
public class CsvToMojoFrameConverter {
  /** Converts the CSV data from the given {@link InputStream} to a {@link MojoFrame}. */
  public MojoFrame apply(InputStream inputStream, MojoFrameBuilder frameBuilder) {
    MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();

    Reader reader = new InputStreamReader(inputStream, UTF_8);

    final CSVReader csvReader = BatchedCsvMojoProcessor.readerToCsvReader(reader);

    final Iterator<String[]> csvReaderIter = csvReader.iterator();

    final String[] labels = csvReaderIter.next();

    if (labels.length != rowBuilder.size()) {
      throw new IllegalArgumentException(
          String.format(
              "Mismatch between column counts of CSV file and the mojo (csv=%d, mojo=%d).",
              labels.length, rowBuilder.size()));
    }

    for (; csvReaderIter.hasNext(); ) {
      final String[] row = csvReaderIter.next();
      for (int c = 0; c < row.length; c++) {
        rowBuilder.setValue(labels[c], row[c]);
      }
      // Reuse the builder.
      rowBuilder = frameBuilder.addRow(rowBuilder);
    }

    return frameBuilder.toMojoFrame();
  }
}
