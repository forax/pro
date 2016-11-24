package com.github.forax.pro.bootstrap;

import static com.github.forax.pro.Pro.get;
import static com.github.forax.pro.helper.FileHelper.unchecked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.github.forax.pro.helper.FileHelper;

class Vanity {
  private Vanity() {
    throw new AssertionError();
  }

  static void postOperations() throws IOException {
    Path imagePath = get("convention.javaLinkerImagePath", Path.class);
    
    // rename to command to pro
    Files.move(imagePath.resolve("bin/com.github.forax.pro.main"),
               imagePath.resolve("bin/pro"));
    
    // remove other commands
    try(Stream<Path> stream = Files.list(imagePath.resolve("bin"))) {
      stream.filter(p -> !p.getFileName().toString().equals("pro") &&
                         !p.getFileName().toString().equals("java"))
            .forEach(unchecked(Files::delete));
    }
    
    // change image directory
    FileHelper.deleteAllFiles(imagePath.resolveSibling("pro"));
    Files.move(imagePath, imagePath.resolveSibling("pro"));
  }
}
