package com.github.forax.pro.helper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class FileHelperTests {
  @Test
  void pathFilenameEndsWith() {
    Predicate<Path> predicate = FileHelper.pathFilenameEndsWith("foo");
    Assertions.assertTrue(predicate.test(Paths.get("bar.foo")));
  }
}
