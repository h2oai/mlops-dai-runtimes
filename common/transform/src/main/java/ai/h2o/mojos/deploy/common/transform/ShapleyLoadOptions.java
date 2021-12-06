package ai.h2o.mojos.deploy.common.transform;

/**
 * Enum defining options for loading the mojo to enable Shapley predictions.
 */
public enum ShapleyLoadOptions {
  ALL,
  NONE,
  ORIGINAL,
  TRANSFORMED;


  /**
   * Checks whether Shapley scoring is permitted.
   * @param options {@link ShapleyLoadOptions}
   * @return {@link Boolean}
   */
  public static boolean isEnabled(ShapleyLoadOptions options) {
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
  public static boolean requestedTypeEnabled(ShapleyLoadOptions options, String requested) {
    if (options.equals(ALL)) {
      return true;
    }
    return options.name().equals(requested);
  }
}
