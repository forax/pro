package com.github.forax.pro.helper;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("slow")
@SuppressWarnings("static-method")
class SlowNoopTests {
  @Test
  void sleep100() throws Exception {
    Thread.sleep(100);
  }

  @Test
  void sleep101() throws Exception {
    Thread.sleep(101);
  }

  @Test
  void sleep102() throws Exception {
    Thread.sleep(102);
  }
}
