package ai.h2o.mojos.deploy.common.transform;

import ai.h2o.mojos.deploy.common.rest.model.DataField;
import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.runtime.frame.MojoFrame;

import java.util.List;

public class Utils {

  private static final String PREDICTION_INTERVAL_ENABLE_ENV_VAR = "PREDICTION_INTERVAL";

  private static final String PREDICTION_INTERVAL_ENABLE_PROPERTY = "prediction.interval";

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

  /**
   * Sanitize boolean string literal values true / false (case insensitive) into 1 / 0 respectively.
   * @return sanitized string.
   */
  public static String sanitizeBoolean(String value, DataField.DataTypeEnum dataType) {
    if (
        dataType.equals(DataField.DataTypeEnum.FLOAT32)
            || dataType.equals(DataField.DataTypeEnum.FLOAT64)
    ) {
      if ("true".equalsIgnoreCase(value)) {
        return "1";
      } else if ("false".equalsIgnoreCase(value)) {
        return "0";
      }
    }
    return value;
  }

  /**
   * Check if prediction interval is enabled or not.
   */
  public static boolean isPredictionIntervalEnabled() {
    return isPredictionIntervalEnabledFromEnv() || isPredictionIntervalEnabledFromProperty();
  }

  private static boolean isPredictionIntervalEnabledFromEnv() {
    try {
      return Boolean.getBoolean(PREDICTION_INTERVAL_ENABLE_PROPERTY);
    } catch (Exception ignored) {
      return false;
    }
  }

  private static boolean isPredictionIntervalEnabledFromProperty() {
    try {
      return Boolean.parseBoolean(System.getenv(PREDICTION_INTERVAL_ENABLE_ENV_VAR));
    } catch (Exception ignored) {
      return false;
    }
  }
}
