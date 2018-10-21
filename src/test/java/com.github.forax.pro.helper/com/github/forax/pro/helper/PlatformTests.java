package com.github.forax.pro.helper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

@SuppressWarnings("static-method")
class PlatformTests {
  @Test
  void currentPlatformIsNotNull() {
    assertNotNull(Platform.current());
  }

  @TestFactory
  Stream<DynamicTest> javaExecutableNamesAreNotBlank() {
    return DynamicTest.stream(
        Arrays.asList(Platform.values()).iterator(),
        Object::toString,
        platform -> {
          assertNotNull(platform.javaExecutableName());
          assertFalse(platform.javaExecutableName().isEmpty());
        }
    );
  }

  @Test
  void javaExecutableNamePointsToExecutableFile() {
    var name = Platform.current().javaExecutableName();
    var path = Path.of(System.getProperty("java.home")).resolve("bin").resolve(name);
    assertTrue(Files.isExecutable(path), "executable? " + path);
  }

}
