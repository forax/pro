package com.github.forax.pro.bootstrap;

import static com.github.forax.pro.Pro.get;
import static com.github.forax.pro.helper.FileHelper.unchecked;

import com.github.forax.pro.helper.FileHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

class Vanity {
  private Vanity() {
    throw new AssertionError();
  }

  private static boolean keep(Path path) {
    String fileName = path.getFileName().toString();
    return fileName.equals("pro")
        || fileName.equals("pro.bat")
        || fileName.equals("java")
        || fileName.equals("java.exe")
        || fileName.equals("javac")
        || fileName.equals("javac.exe")
        || fileName.endsWith(".dll")
        || Files.isDirectory(path);
  }

  static void postOperations() throws IOException {
    Path imagePath = get("convention.javaLinkerImagePath", Path.class);

    // remove unnecessary commands
    try (Stream<Path> stream = Files.list(imagePath.resolve("bin"))) {
      stream.filter(path -> !keep(path)).forEach(unchecked(Files::delete));
    }

    // change image directory
    Path proDirectory = imagePath.resolveSibling("pro");
    FileHelper.deleteAllFiles(proDirectory, true);
    Files.move(imagePath, proDirectory);
  }
}
