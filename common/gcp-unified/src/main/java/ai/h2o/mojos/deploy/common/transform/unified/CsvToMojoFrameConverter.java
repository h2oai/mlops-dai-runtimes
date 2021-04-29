package ai.h2o.mojos.deploy.common.transform.unified;

import static java.nio.charset.StandardCharsets.UTF_8;

import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;
import ai.h2o.mojos.runtime.utils.BatchedCsvMojoProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;

/**
 * Converts the CSV from {@link InputStream} to the {@link MojoFrame} for scoring.
 *
 * @deprecated use {@link BatchedCsvMojoProcessor} instead, it is better for large datasets as it
 *     can process it by batches of configurable size.
 */
@Deprecated
public class CsvToMojoFrameConverter {
  /**
   * Converts the CSV data from the given {@link InputStream} to a {@link MojoFrame}.
   *
   * @deprecated use {@link BatchedCsvMojoProcessor} instead, it is better for large datasets as it
   *     can process it by batches of configurable size. An example usage is here:
   *     https://github.com/h2oai/mojo2/blob/master/java/mojo2-runtime-impl/src-examples/main/java/ai/h2o/mojos/ExecuteMojo.java#L67-L77
   *     .
   */
  @Deprecated
  public MojoFrame apply(InputStream inputStream, MojoFrameBuilder frameBuilder)
      throws IOException {
    MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();

    Reader reader = new InputStreamReader(inputStream, UTF_8);
    // Use default CSV parser settings.
    final Iterator<String[]> csvReaderIter =
        BatchedCsvMojoProcessor.readerToCsvReader(reader).iterator();
    // Read first row as column labels
    final String[] labels = csvReaderIter.next();

    // It looks like empty file, so follow original contract and throw IOException
    if (labels == null) {
      throw new IOException("Empty csv file!");
    }

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
