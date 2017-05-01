package com.github.forax.pro.helper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

@SuppressWarnings("static-method")
class PlatformTests {
  @Test
  void currentPlatformIsNotNull() {
    Assertions.assertNotNull(Platform.current());
  }

  @TestFactory
  Stream<DynamicTest> javaExecutableNamesAreNotBlank() {
    return DynamicTest.stream(
        Arrays.asList(Platform.values()).iterator(),
        Object::toString,
        platform -> {
          Assertions.assertNotNull(platform.javaExecutableName());
          Assertions.assertFalse(platform.javaExecutableName().isEmpty());
        }
    );
  }

  @Test
  void javaExecutableNamePointsToExecutableFile() {
    String name = Platform.current().javaExecutableName();
    Path path = Paths.get(System.getProperty("java.home")).resolve("bin").resolve(name);
    Assertions.assertTrue(Files.isExecutable(path), "executable? " + path);
  }

}
