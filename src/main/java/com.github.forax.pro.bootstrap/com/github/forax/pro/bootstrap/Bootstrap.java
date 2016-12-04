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
        "com.github.forax.pro.plugin.modulefixer@9.0",
        "com.github.forax.pro.plugin.compiler@9.0",
        "com.github.forax.pro.plugin.packager@9.0",
        "com.github.forax.pro.plugin.linker@9.0",
        "com.github.forax.pro.plugin.bootstrap@9.0"
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
    
    set("linker.rootModules", append(
        "com.github.forax.pro",
        "com.github.forax.pro.main",
        "com.github.forax.pro.plugin.convention",
        "com.github.forax.pro.plugin.resolver",
        "com.github.forax.pro.plugin.modulefixer",
        "com.github.forax.pro.plugin.compiler",
        "com.github.forax.pro.plugin.packager",
        "com.github.forax.pro.plugin.linker",
        "jdk.compiler",
        "jdk.zipfs",      // need by the compiler to open compressed zip !
        "jdk.jartool",
        "jdk.jlink",
        "jdk.jshell"));
    
    //set("linker.stripNativeCommands", true);
    //set("linker.serviceNames", list("java.util.spi.ToolProvider"));
    //run("compiler", "packager");
    
    run("modulefixer", "compiler", "packager", "linker");
    
    Vanity.postOperations();
  }
}
