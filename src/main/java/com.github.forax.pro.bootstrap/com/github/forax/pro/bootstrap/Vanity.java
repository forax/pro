package com.github.forax.pro.bootstrap;

import static com.github.forax.pro.Pro.getOrElseThrow;
import static com.github.forax.pro.helper.FileHelper.unchecked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.forax.pro.helper.FileHelper;

class Vanity {
  private Vanity() {
    throw new AssertionError();
  }

  private static boolean keep(Path path) {
    var fileName = path.getFileName().toString();
    return fileName.equals("pro") || fileName.equals("pro.bat") ||
           fileName.equals("java") || fileName.equals("java.exe") ||
           fileName.equals("javaw") || fileName.equals("javaw.exe") ||
           fileName.equals("javap") || fileName.equals("javap.exe") ||
           fileName.equals("javac") || fileName.equals("javac.exe") ||
           fileName.endsWith(".dll") ||
           Files.isDirectory(path);
  }
  
  static void postOperations() throws IOException {
    var imagePath = getOrElseThrow("convention.javaLinkerImagePath", Path.class);
    
    // remove unnecessary commands
    try(var stream = Files.list(imagePath.resolve("bin"))) {
      stream.filter(path -> !keep(path))
            .forEach(unchecked(Files::delete));
    }
    
    // change image directory
    var proDirectory = imagePath.resolveSibling("pro");
    FileHelper.deleteAllFiles(proDirectory, true);
    Files.move(imagePath, proDirectory);
  }
}
