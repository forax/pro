package com.github.forax.pro.bootstrap;

import static com.github.forax.pro.Pro.append;
import static com.github.forax.pro.Pro.list;
import static com.github.forax.pro.Pro.run;
import static com.github.forax.pro.Pro.set;

import java.io.IOException;

public class Bootstrap {
  public static void main(String[] args) throws IOException {
    set("packager.moduleMetadata", list(
        "com.github.forax.pro@9.0",
        "com.github.forax.pro.api@9.0",
        "com.github.forax.pro.helper@9.0",
        "com.github.forax.pro.main@9.0/com.github.forax.pro.main.Main",
        "com.github.forax.pro.plugin.convention@9.0",
        "com.github.forax.pro.plugin.resolver@9.0",
        "com.github.forax.pro.plugin.compiler@9.0",
        "com.github.forax.pro.plugin.packager@9.0",
        "com.github.forax.pro.plugin.linker@9.0",
        "com.github.forax.pro.plugin.bootstrap@9.0"
        ));
    
    set("linker.rootModules", append(
        "com.github.forax.pro",
        "com.github.forax.pro.main",
        "com.github.forax.pro.plugin.convention",
        "com.github.forax.pro.plugin.resolver",
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
    
    run("compiler", "packager" /*, "linker" FIXME*/ );
    
    //Vanity.postOperations(); FIXME
  }
}
