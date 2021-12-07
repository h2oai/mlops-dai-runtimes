package ai.h2o.mojos.deploy.common.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class ShapleyLoadOptionTest {

  @SystemStub
  private EnvironmentVariables environmentVariables;

  @BeforeEach
  void verifyEnvironment() {
    assertNull(System.getenv("SHAPLEY_ENABLE"));
    assertNull(System.getenv("SHAPLEY_TYPES_ENABLED"));
    assertNull(System.getProperty("shapley.enable"));
    assertNull(System.getProperty("shapley.types.enabled"));
  }

  @AfterEach
  void clearProperties() {
    System.clearProperty("shapley.enable");
    System.clearProperty("shapley.types.enabled");

    environmentVariables.set("SHAPLEY_ENABLE", null);
    environmentVariables.set("SHAPLEY_TYPES_ENABLED", null);
  }

  @Test
  void loadOptionFromConfiguration_NoConfig_ReturnsDefault() {
    // Given

    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.NONE, option);
  }

  @Test
  void loadOptionFromConfiguration_EnabledEnvironmentVariable_ReturnsAll() {
    // Given
    environmentVariables.set("SHAPLEY_ENABLE", "true");

    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.ALL, option);
  }

  @Test
  void loadOptionFromConfiguration_EnabledSystemProperty_ReturnsAll() {
    // Given
    System.setProperty("shapley.enable", "true");

    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.ALL, option);
  }

  @Test
  void loadOptionFromConfiguration_TypeInEnvironmentAll_ReturnsAll() {
    // Given
    environmentVariables.set("SHAPLEY_TYPES_ENABLED", "ALL");

    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.ALL, option);
  }

  @Test
  void loadOptionFromConfiguration_TypeInEnvironmentTranformed_ReturnsTransformed() {
    // Given
    environmentVariables.set("SHAPLEY_TYPES_ENABLED", "TRANSFORMED");

    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.TRANSFORMED, option);
  }

  @Test
  void loadOptionFromConfiguration_TypeInEnvironmentOriginal_ReturnsOriginal() {
    // Given
    environmentVariables.set("SHAPLEY_TYPES_ENABLED", "ORIGINAL");

    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.ORIGINAL, option);
  }

  @Test
  void loadOptionFromConfiguration_TypeInPropertiesAll_ReturnsAll() {
    // Given
    System.setProperty("shapley.types.enabled", "ALL");

    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.ALL, option);
  }

  @Test
  void loadOptionFromConfiguration_TypeInPropertiesTransformed_ReturnsTransformed() {
    // Given
    System.setProperty("shapley.types.enabled", "TRANSFORMED");

    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.TRANSFORMED, option);
  }

  @Test
  void loadOptionFromConfiguration_TypeInPropertiesOriginal_ReturnsOriginal() {
    // Given
    System.setProperty("shapley.types.enabled", "TRANSFORMED");

    // When
    ShapleyLoadOption option = ShapleyLoadOption.fromEnvironment();

    // Then
    assertEquals(ShapleyLoadOption.ORIGINAL, option);
  }
}