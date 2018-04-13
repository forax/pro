package com.github.forax.pro.plugin.tester;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class TesterPluginTests {
  @Test
  void nameIsTester() {
    assertEquals("tester", new TesterPlugin().name());
  }
}
