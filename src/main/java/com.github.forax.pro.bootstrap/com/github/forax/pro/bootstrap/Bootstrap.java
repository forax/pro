package com.github.forax.pro.bootstrap;

import static com.github.forax.pro.Pro.list;
import static com.github.forax.pro.Pro.local;
import static com.github.forax.pro.Pro.location;
import static com.github.forax.pro.Pro.path;
import static com.github.forax.pro.Pro.run;
import static com.github.forax.pro.Pro.set;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.util.stream.Collectors;

import com.github.forax.pro.helper.FileHelper;

public class Bootstrap {
  public static void main(String[] args) throws IOException {
    set("pro.loglevel", "verbose");
    set("pro.exitOnError", true);
    
    //set("compiler.lint", "exports,module");
    set("compiler.lint", "all,-varargs,-overloads");
    
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
        "com.github.forax.pro.plugin.bootstrap@0.9/com.github.forax.pro.bootstrap.Bootstrap",
        "com.github.forax.pro.ubermain@0.9",
        "com.github.forax.pro.uberbooter@0.9",
        "com.github.forax.pro.daemon@0.9",
        "com.github.forax.pro.daemon.imp@0.9"
        ));
    
    //set("modulefixer.force", true);
    set("modulefixer.additionalRequires", list(
        "maven.aether.provider=commons.lang",
        "maven.aether.provider=com.github.forax.pro.aether.fakeguava",
        "maven.aether.provider=plexus.utils",
        "maven.builder.support=commons.lang",
        "maven.modelfat=commons.lang",
        "aether.impl=aether.util",
        "aether.transport.http=aether.util",
        "aether.connector.basic=aether.util"
        ));
    
    set("linker.includeSystemJMODs", true);
    set("linker.launchers", list(
        "pro=com.github.forax.pro.main/com.github.forax.pro.main.Main"
        ));
    set("linker.rootModules", list(
        "com.github.forax.pro.main",
        "com.github.forax.pro.plugin.convention",
        "com.github.forax.pro.plugin.resolver",
        "com.github.forax.pro.plugin.modulefixer",
        "com.github.forax.pro.plugin.compiler",
        "com.github.forax.pro.plugin.packager",
        "com.github.forax.pro.plugin.linker",
        "com.github.forax.pro.plugin.uberpackager",
        "com.github.forax.pro.uberbooter",            // needed by ubermain
        "com.github.forax.pro.daemon.imp"
        )                                             // then add all system modules
        .appendAll(ModuleFinder.ofSystem().findAll().stream()
                  .map(ref -> ref.descriptor().name())
                  .filter(name -> !name.startsWith("com.github.forax.pro"))
                  .collect(Collectors.toSet())));
    
    //set("linker.stripNativeCommands", true);
    //set("linker.serviceNames", list("java.util.spi.ToolProvider"));
    
    
    run("modulefixer", "compiler", "packager");
    
    // compile and package plugins
    // FIXME, remove plugins/runner/ in front of the path
    compileAndPackagePlugin("runner");
    
    run("linker", "uberpackager");

    copyPackagedPluginToTargetImage("runner");

    Vanity.postOperations();
  }

  static void compileAndPackagePlugin(String name) throws IOException {
    compileAndPackagePlugin(name, () -> {});
  }

  static void compileAndPackagePlugin(String name, Runnable extras) throws IOException {
    local(location("plugins/" + name), () -> {

      set("modulefixer.moduleDependencyPath", path("plugins/" + name + "/deps"));
      set("modulefixer.moduleDependencyFixerPath", location("plugins/" + name + "/target/deps/module-fixer"));

      set("compiler.moduleSourcePath", path("plugins/" + name + "/src/main/java"));
      set("compiler.moduleExplodedSourcePath", location("plugins/" + name + "/target/main/exploded"));
      set("compiler.moduleDependencyPath", path("plugins/" + name + "/deps", "plugins/" + name + "/../../target/main/artifact/", "plugins/" + name + "/../../deps"));

      set("packager.moduleExplodedSourcePath", path("plugins/" + name + "/target/main/exploded"));
      set("packager.moduleArtifactSourcePath", location("plugins/" + name + "/target/main/artifact"));

      extras.run();

      run("resolver", "modulefixer", "compiler", "packager");
    });
  }

  static void copyPackagedPluginToTargetImage(String name) throws IOException {
    Files.createDirectories(location("target/image/plugins/" + name));
    path("plugins/" + name + "/target/main/artifact", "plugins/" + name + "/deps")
        .filter(Files::exists)
        .forEach(srcPath ->
            FileHelper.walkAndFindCounterpart(
                srcPath,
                location("target/image/plugins/" + name),
                stream -> stream.filter(p -> p.toString().endsWith(".jar")),
                Files::copy));
  }
}
