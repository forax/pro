package com.github.forax.pro.bootstrap;

import static com.github.forax.pro.Pro.append;
import static com.github.forax.pro.Pro.get;
import static com.github.forax.pro.Pro.list;
import static com.github.forax.pro.Pro.run;
import static com.github.forax.pro.Pro.set;
import static com.github.forax.pro.helper.FileHelper.unchecked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.github.forax.pro.helper.FileHelper;

public class Bootstrap {
  private static void postOperations() throws IOException {
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
  
  public static void main(String[] args) throws IOException {
    set("packager.moduleMetadata", list(
        "com.github.forax.pro@1.0",
        "com.github.forax.pro.api@1.0",
        "com.github.forax.pro.helper@1.0",
        "com.github.forax.pro.main@1.0/com.github.forax.pro.main.Main",
        "com.github.forax.pro.plugin.convention@1.0",
        "com.github.forax.pro.plugin.compiler@1.0",
        "com.github.forax.pro.plugin.packager@1.0",
        "com.github.forax.pro.plugin.linker@1.0"
        ));
    
    set("linker.rootModules", append(
        "com.github.forax.pro",
        "com.github.forax.pro.main",
        "com.github.forax.pro.plugin.convention",
        "com.github.forax.pro.plugin.compiler",
        "com.github.forax.pro.plugin.packager",
        "com.github.forax.pro.plugin.linker",
        "jdk.compiler",
        "jdk.jartool",
        "jdk.jlink",
        "jdk.jshell"));
    
    //set("linker.stripNativeCommands", true);
    //set("linker.serviceNames", list("java.util.spi.ToolProvider"));
    //run("compiler", "packager");
    
    run("compiler", "packager", "linker");
    
    postOperations();
  }
}
