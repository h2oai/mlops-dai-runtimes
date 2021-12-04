package ai.h2o.mojos.deploy.common.transform;

/**
 * Enum defining options for loading the mojo to enable Shapley predictions.
 */
public enum ShapleyLoadOptions {
  ALL("ALL"),
  NONE("NONE"),
  ORIGINAL("ORIGINAL"),
  TRANSFORMED("TRANSFORMED");

  private final String value;

  ShapleyLoadOptions(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  /**
   * Checks whether Shapley scoring is permitted.
   * @return {@link Boolean}
   */
  public boolean isEnabled() {
    ShapleyLoadOptions options = ShapleyLoadOptions.fromValue(value);
    switch (options) {
      case ALL:
      case ORIGINAL:
      case TRANSFORMED:
        return true;
      case NONE:
      default:
        return false;
    }
  }

  /**
   * Checks whether requested type of Shapley value scoring is permitted.
   * @param requested {@link String}
   * @return {@link Boolean}
   */
  public boolean requestedTypeEnabled(String requested) {
    if (value.equals(ALL.getValue())) {
      return true;
    }
    return value.equals(requested);
  }

  /**
   * Obtain ShaplelyLoadOptions value from input string.
   * @param text {@link String} one of [ALL, NONE, ORIGINAL, TRANSFORMED]
   * @return {@link ShapleyLoadOptions} ShapleyLoadOptions representation of input string
   */
  public static ShapleyLoadOptions fromValue(String text) {
    for (ShapleyLoadOptions b : ShapleyLoadOptions.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return NONE;
  }
}
