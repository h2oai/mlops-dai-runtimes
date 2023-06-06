package ai.h2o.mojos.deploy.common.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;


class ShapleyLoadOptionTest {

  @AfterEach
  @ClearEnvironmentVariable(key = "SHAPLEY_ENABLE")
  @ClearEnvironmentVariable(key = "SHAPLEY_TYPES_ENABLED")
  @ClearSystemProperty(key = "shapley.enable")
  @ClearSystemProperty(key = "shapley.types.enabled")
  void clearProperties() {
  }

  @Test
  @ClearEnvironmentVariable(key = "SHAPLEY_ENABLE")
  @ClearEnvironmentVariable(key = "SHAPLEY_TYPES_ENABLED")
  @ClearSystemProperty(key = "shapley.enable")
  @ClearSystemProperty(key = "shapley.types.enabled")
  void loadOptionFromConfiguration_NoConfig_ReturnsDefault() {
    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.NONE, option);
  }

  @Test
  @SetEnvironmentVariable(key = "SHAPLEY_ENABLE", value = "true")
  @ClearSystemProperty(key = "shapley.enable")
  @ClearSystemProperty(key = "shapley.types.enabled")
  void loadOptionFromConfiguration_EnabledEnvironmentVariable_ReturnsAll() {
    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.ALL, option);
  }

  @Test
  @ClearEnvironmentVariable(key = "SHAPLEY_ENABLE")
  @ClearEnvironmentVariable(key = "SHAPLEY_TYPES_ENABLED")
  @ClearSystemProperty(key = "shapley.types.enabled")
  @SetSystemProperty(key = "shapley.enable", value = "true")
  void loadOptionFromConfiguration_EnabledSystemProperty_ReturnsAll() {
    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.ALL, option);
  }

  @Test
  @SetEnvironmentVariable(key = "SHAPLEY_TYPES_ENABLED", value = "ALL")
  @ClearSystemProperty(key = "shapley.enable")
  @ClearSystemProperty(key = "shapley.types.enabled")
  void loadOptionFromConfiguration_TypeInEnvironmentAll_ReturnsAll() {
    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.ALL, option);
  }

  @Test
  @SetEnvironmentVariable(key = "SHAPLEY_TYPES_ENABLED", value = "TRANSFORMED")
  @ClearSystemProperty(key = "shapley.enable")
  @ClearSystemProperty(key = "shapley.types.enabled")
  void loadOptionFromConfiguration_TypeInEnvironmentTranformed_ReturnsTransformed() {
    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.TRANSFORMED, option);
  }

  @Test
  @SetEnvironmentVariable(key = "SHAPLEY_TYPES_ENABLED", value = "ORIGINAL")
  @ClearSystemProperty(key = "shapley.enable")
  @ClearSystemProperty(key = "shapley.types.enabled")
  void loadOptionFromConfiguration_TypeInEnvironmentOriginal_ReturnsOriginal() {
    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.ORIGINAL, option);
  }

  @Test
  @ClearEnvironmentVariable(key = "SHAPLEY_ENABLE")
  @ClearEnvironmentVariable(key = "SHAPLEY_TYPES_ENABLED")
  @ClearSystemProperty(key = "shapley.enable")
  @SetSystemProperty(key = "shapley.types.enabled", value = "ALL")
  void loadOptionFromConfiguration_TypeInPropertiesAll_ReturnsAll() {
    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.ALL, option);
  }

  @Test
  @ClearEnvironmentVariable(key = "SHAPLEY_ENABLE")
  @ClearEnvironmentVariable(key = "SHAPLEY_TYPES_ENABLED")
  @ClearSystemProperty(key = "shapley.enable")
  @SetSystemProperty(key = "shapley.types.enabled", value = "TRANSFORMED")
  void loadOptionFromConfiguration_TypeInPropertiesTransformed_ReturnsTransformed() {
    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.TRANSFORMED, option);
  }

  @Test
  @ClearEnvironmentVariable(key = "SHAPLEY_ENABLE")
  @ClearEnvironmentVariable(key = "SHAPLEY_TYPES_ENABLED")
  @ClearSystemProperty(key = "shapley.enable")
  @SetSystemProperty(key = "shapley.types.enabled", value = "ORIGINAL")
  void loadOptionFromConfiguration_TypeInPropertiesOriginal_ReturnsOriginal() {
    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.ORIGINAL, option);
  }
}
