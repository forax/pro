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
    
    // remove other commands
    // FIXME temporary fix to enable windows build
    /*
    try(Stream<Path> stream = Files.list(imagePath.resolve("bin"))) {
      stream.filter(p -> !p.getFileName().toString().equals("pro") &&
                         !p.getFileName().toString().equals("java"))
            .forEach(unchecked(Files::delete));
    }*/
    
    // change image directory
    Path proDirectory = imagePath.resolveSibling("pro");
    FileHelper.deleteAllFiles(proDirectory, true);
    Files.move(imagePath, proDirectory);
  }
}
