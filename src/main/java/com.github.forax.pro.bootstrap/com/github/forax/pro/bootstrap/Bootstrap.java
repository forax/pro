package com.github.forax.pro.bootstrap;

import static com.github.forax.pro.Pro.append;
import static com.github.forax.pro.Pro.list;
import static com.github.forax.pro.Pro.run;
import static com.github.forax.pro.Pro.set;

import java.io.IOException;

public class Bootstrap {
  public static void main(String[] args) throws IOException {
    set("packager.moduleMetadata", list(
        "com.github.forax.pro@1.0",
        "com.github.forax.pro.api@1.0",
        "com.github.forax.pro.helper@1.0",
        "com.github.forax.pro.main@1.0/com.github.forax.pro.main.Main",
        "com.github.forax.pro.plugin.convention@1.0",
        "com.github.forax.pro.plugin.compiler@1.0",
        "com.github.forax.pro.plugin.packager@1.0",
        "com.github.forax.pro.plugin.linker@1.0",
        "com.github.forax.pro.plugin.bootstrap@1.0"
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
    
    Vanity.postOperations();
  }
}
