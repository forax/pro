package integration.pro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.forax.pro.Pro;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class ProTests {
  @Test
  @Disabled(
      "java.util.ServiceConfigurationError: com.github.forax.pro.api.Plugin: com.github.forax.pro.plugin.modulefixer.ModuleFixerPlugin not a subtype")
  void list() {
    assertEquals(0, Pro.list().size());
  }
}
