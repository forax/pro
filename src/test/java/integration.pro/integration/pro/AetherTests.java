package integration.pro;

import com.github.forax.pro.aether.Aether;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("static-method")
class AetherTests {
  @Test
  @Tag("integration")
  void create() throws Exception {
    assertNotNull(Aether.create(Files.createTempDirectory("AetherTests-create-"), List.of()));
  }
}
