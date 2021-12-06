package ai.h2o.mojos.deploy.common.transform;

/**
 * Enum defining options for loading the mojo to enable Shapley predictions.
 */
public enum ShapleyLoadOption {
  ALL,
  NONE,
  ORIGINAL,
  TRANSFORMED;


  /**
   * Checks whether Shapley scoring is permitted.
   * @param option {@link ShapleyLoadOption}
   * @return {@link Boolean}
   */
  public static boolean isEnabled(ShapleyLoadOption option) {
    switch (option) {
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
  public static boolean requestedTypeEnabled(ShapleyLoadOption option, String requested) {
    if (option.equals(ALL)) {
      return true;
    }
    return option.name().equals(requested);
  }
}
