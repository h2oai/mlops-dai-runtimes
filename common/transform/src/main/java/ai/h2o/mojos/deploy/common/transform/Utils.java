package ai.h2o.mojos.deploy.common.transform;

import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.runtime.frame.MojoFrame;

import java.util.List;

public class Utils {
  /**
   * Method to copy rows from mojoFrame to a list of Rows.
   *
   * @param mojoFrame {@link MojoFrame}
   */
  public static void copyResultFields(MojoFrame mojoFrame, List<Row> outputRows) {
    String[][] outputColumns = new String[mojoFrame.getNcols()][];
    for (int col = 0; col < mojoFrame.getNcols(); col++) {
      outputColumns[col] = mojoFrame.getColumn(col).getDataAsStrings();
    }
    for (int row = 0; row < mojoFrame.getNrows(); row++) {
      Row outputRow = outputRows.get(row);
      for (String[] resultColumn : outputColumns) {
        outputRow.add(resultColumn[row]);
      }
    }
  }
}
