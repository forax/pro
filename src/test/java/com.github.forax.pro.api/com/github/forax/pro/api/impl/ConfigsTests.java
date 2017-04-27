package com.github.forax.pro.api.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("ConfigsTests (in unnamed module) cannot access class Configs (in module com.github.forax.pro.api)")
class ConfigsTests {
  @Test
  void newRootIsNotNull() {
    assertNotNull(Configs.newRoot());
  }
}
