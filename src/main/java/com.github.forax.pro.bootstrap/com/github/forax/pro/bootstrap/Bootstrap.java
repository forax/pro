package com.github.forax.pro.bootstrap;

import static com.github.forax.pro.Pro.*;
import static com.github.forax.pro.Pro.run;
import static com.github.forax.pro.Pro.set;

import java.io.IOException;

public class Bootstrap {
  public static void main(String[] args) throws IOException {
    set("pro.loglevel", "verbose");
    set("pro.exitOnError", true);
    
    set("packager.moduleMetadata", list(
        "com.github.forax.pro@0.9",
        "com.github.forax.pro.aether@0.9",
        "com.github.forax.pro.ather.fakeguava@0.9",
        "com.github.forax.pro.api@0.9",
        "com.github.forax.pro.helper@0.9",
        "com.github.forax.pro.main@0.9/com.github.forax.pro.main.Main",
        "com.github.forax.pro.plugin.convention@0.9",
        "com.github.forax.pro.plugin.resolver@0.9",
        "com.github.forax.pro.plugin.modulefixer@0.9",
        "com.github.forax.pro.plugin.compiler@0.9",
        "com.github.forax.pro.plugin.packager@0.9",
        "com.github.forax.pro.plugin.linker@0.9",
        "com.github.forax.pro.plugin.runner@0.9",
        "com.github.forax.pro.plugin.uberpackager@0.9", 
        "com.github.forax.pro.plugin.bootstrap@0.9",
        "com.github.forax.pro.ubermain@0.9",
        "com.github.forax.pro.uberbooter@0.9",
        "com.github.forax.pro.daemon@0.9",
        "com.github.forax.pro.daemon.imp@0.9"
        ));
    
    //set("modulefixer.force", true);
    set("modulefixer.additionalRequires", list(
        "maven.aether.provider=commons.lang",
        "maven.aether.provider=com.github.forax.pro.aether.fakeguava",
        "maven.builder.support=commons.lang",
        "maven.modelfat=commons.lang",
        "aether.impl=aether.util",
        "aether.transport.http=aether.util",
        "aether.connector.basic=aether.util"
        ));
    
    set("linker.includeSystemJMODs", true);
    set("linker.rootModules", list(
        "com.github.forax.pro.main",
        "com.github.forax.pro.plugin.convention",
        "com.github.forax.pro.plugin.resolver",
        "com.github.forax.pro.plugin.modulefixer",
        "com.github.forax.pro.plugin.compiler",
        "com.github.forax.pro.plugin.packager",
        "com.github.forax.pro.plugin.linker",
        "com.github.forax.pro.plugin.runner",
        "com.github.forax.pro.plugin.uberpackager",
        "com.github.forax.pro.uberbooter",            // needed by ubermain
        "com.github.forax.pro.daemon.imp",
        "jdk.compiler",
        "jdk.zipfs",      // needed by the compiler to open compressed zip !
        "jdk.jartool",
        "jdk.jlink",
        "jdk.jshell"));
    
    //set("linker.stripNativeCommands", true);
    //set("linker.serviceNames", list("java.util.spi.ToolProvider"));
    
    
    run("modulefixer", "compiler", "packager", "linker", "uberpackager");
    
    Vanity.postOperations();
  }
}
