package com.github.forax.pro.api.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class ConfigsTests {
  @Test
  void newRootIsNotNull() {
    assertNotNull(new DefaultConfig());
  }
}
