package com.github.forax.pro.plugin.tester;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class TesterPluginTests {
  @Test
  void nameIsTester() {
    Assertions.assertEquals("tester", new TesterPlugin().name());
  }
}
