package com.github.forax.pro.helper.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import com.github.forax.pro.helper.util.Unchecked.IORunnable;
import com.github.forax.pro.helper.util.Unchecked.IOSupplier;

@SuppressWarnings("static-method")
class UncheckedTests {
  void rethrow() {
    Unchecked.getUnchecked(() -> 2);
    Unchecked.runUnchecked(() -> { /* empty */});
    assertThrows(IOException.class, () -> Unchecked.getUnchecked((IOSupplier<Void>)() -> { throw new IOException(); }));
    assertThrows(IOException.class, () -> Unchecked.runUnchecked((IORunnable)() -> { throw new IOException(); }));
  }
}
