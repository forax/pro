package integration.pro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.forax.pro.Pro;
import org.junit.jupiter.api.Test;

class ProTests {

  @Test
  void list() {
    assertEquals(0, Pro.list().size());
  }
}
