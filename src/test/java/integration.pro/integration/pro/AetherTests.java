package integration.pro;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.forax.pro.aether.Aether;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class AetherTests {
  @Test
  void create() throws Exception {
    assertNotNull(Aether.create(Files.createTempDirectory("AetherTests-create-"), List.of()));
  }
}
