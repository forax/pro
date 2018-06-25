package integration.pro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.forax.pro.Pro;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class ProTests {
  @Test
  @Disabled(
      "java.lang.LinkageError: loader constraint violation: loader \"<unnamed>\""
          + "(instance of jdk.internal.loader.Loader, child of \"app\" jdk.internal.loader.ClassLoaders$AppClassLoader)"
          + "wants to load class com.github.forax.pro.helper.util.StableList."
          + "A different class with the same name was previously loaded by \"app\""
          + "(instance of jdk.internal.loader.ClassLoaders$AppClassLoader)")
  void list() {
    assertEquals(0, Pro.list().size());
  }
}
