package ai.h2o.mojos.deploy.common.transform;

/** Enum defining options for loading the mojo to enable Shapley predictions. */
public enum ShapleyLoadOption {
  ALL,
  NONE,
  ORIGINAL,
  TRANSFORMED;

  private static final String SHAPLEY_ENABLE_PROPERTY = "shapley.enable";
  private static final String SHAPLEY_ENABLE_ENV_VAR = "SHAPLEY_ENABLE";
  private static final String SHAPLEY_ENABLED_TYPES_PROPERTY = "shapley.types.enabled";
  private static final String SHAPLEY_ENABLED_TYPES_ENV_VAR = "SHAPLEY_TYPES_ENABLED";

  /**
   * Checks whether Shapley scoring is permitted.
   *
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
   *
   * @param requested {@link String}
   * @return {@link Boolean}
   */
  public static boolean requestedTypeEnabled(ShapleyLoadOption option, String requested) {
    if (option == ALL) {
      return true;
    }
    return option.name().equals(requested);
  }

  /**
   * Extracts configuration from system properties or environment variables.
   *
   * @return {@link ShapleyLoadOption}
   */
  public static ShapleyLoadOption fromEnvironment() {
    return shapleyEnabledFromEnvironment() ? ALL : shapleyTypeFromEnvironment();
  }

  private static boolean shapleyEnabledFromEnvironment() {
    boolean enabledProperty = Boolean.getBoolean(SHAPLEY_ENABLE_PROPERTY);
    boolean enabledEnvironment = Boolean.parseBoolean(System.getenv(SHAPLEY_ENABLE_ENV_VAR));
    return enabledEnvironment || enabledProperty;
  }

  private static ShapleyLoadOption shapleyTypeFromEnvironment() {
    String enabledTypeProperty = System.getProperty(SHAPLEY_ENABLED_TYPES_PROPERTY);
    if (enabledTypeProperty != null) {
      return ShapleyLoadOption.valueOf(enabledTypeProperty);
    }
    String enabledTypeEnvironment = System.getenv(SHAPLEY_ENABLED_TYPES_ENV_VAR);
    if (enabledTypeEnvironment != null) {
      return ShapleyLoadOption.valueOf(enabledTypeEnvironment);
    }
    return NONE;
  }
}
