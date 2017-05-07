package com.github.forax.pro.bootstrap;

import static com.github.forax.pro.Pro.*;
import static com.github.forax.pro.helper.FileHelper.deleteAllFiles;
import static com.github.forax.pro.helper.FileHelper.walkAndFindCounterpart;
import static java.nio.file.Files.createDirectories;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

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
        "com.github.forax.pro.plugin.formatter@0.9",
        "com.github.forax.pro.plugin.runner@0.9",
        "com.github.forax.pro.plugin.tester@0.9",
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
    
    compileAndPackagePlugin("runner", () -> { /* empty */});
    compileAndPackagePlugin("tester", () -> {
      set("resolver.remoteRepositories", list(
        uri("https://oss.sonatype.org/content/repositories/snapshots")
      ));
      // <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
      set("resolver.dependencies", list(
          // "API"
          "opentest4j=org.opentest4j:opentest4j:1.0.0-M2",
          "junit.platform.commons=org.junit.platform:junit-platform-commons:1.0.0-M4",
          "junit.jupiter.api=org.junit.jupiter:junit-jupiter-api:5.0.0-M4",
          // "Launcher + Engine"
          "junit.platform.engine=org.junit.platform:junit-platform-engine:1.0.0-M4",
          "junit.platform.launcher=org.junit.platform:junit-platform-launcher:1.0.0-M4",
          "junit.jupiter.engine=org.junit.jupiter:junit-jupiter-engine:5.0.0-M4"
      ));
    });
    compileAndPackagePlugin("formatter", //
      () -> {
        String version = "1.3";
        String base = "https://github.com/google/google-java-format/releases/download/google-java-format";
        download(base + "-" + version+ "/google-java-format-"+version+"-all-deps.jar", "plugins/formatter/libs");
      },
      "compiler", "packager");

    run("linker", "uberpackager");

    copyPackagedPluginToTargetImage("runner");
    copyPackagedPluginToTargetImage("tester");
    copyPackagedPluginToTargetImage("formatter");

    Vanity.postOperations();
  }

  private static void compileAndPackagePlugin(String name, Runnable extras) throws IOException {
    compileAndPackagePlugin(name, extras, "resolver", "modulefixer", "compiler", "packager");
  }

  private static void compileAndPackagePlugin(String name, Runnable extras, String... plugins) throws IOException {
    deleteAllFiles(location("plugins/" + name + "/target"), false);

    local("plugins/" + name, () -> {
      set("resolver.moduleDependencyPath",
          path("plugins/" + name + "/deps", "target/main/artifact/", "deps"));
      set("compiler.moduleDependencyPath",
          path("plugins/" + name + "/deps", "target/main/artifact/", "deps"));
      set("compiler.upgradeModulePath",
          path("target/main/artifact/", "deps"));

      extras.run();

      run(plugins);
    });
  }

  private static void copyPackagedPluginToTargetImage(String name) throws IOException {
    createDirectories(location("target/image/plugins/" + name));
    path("plugins/" + name + "/target/main/artifact", "plugins/" + name + "/deps")
      .filter(Files::exists)
      .forEach(srcPath ->
        walkAndFindCounterpart(
            srcPath,
            location("target/image/plugins/" + name),
            stream -> stream.filter(p -> p.toString().endsWith(".jar")),
            Files::copy));
    if (Files.exists(Paths.get("plugins/" + name + "/libs"))) {
      createDirectories(location("target/image/plugins/" + name + "/libs"));
      path("plugins/" + name + "/libs")
          .filter(Files::exists)
          .forEach(srcPath ->
              walkAndFindCounterpart(
                  srcPath,
                  location("target/image/plugins/" + name + "/libs"),
                  stream -> stream.filter(p -> p.toString().endsWith(".jar")),
                  Files::copy));    }
  }

  private static void download(String urlSpec, String folder) {
    try {
      URL url = new URL(urlSpec);
      String fileName = new File(url.toURI().getPath()).getName();
      Path targetDirectory = Paths.get(folder);
      Files.createDirectories(targetDirectory);
      File targetFile = targetDirectory.resolve(fileName).toFile();
      if (targetFile.exists()) {
        return;
      }
      ReadableByteChannel rbc = Channels.newChannel(url.openStream());
      FileOutputStream fos = new FileOutputStream(targetFile);
      fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    } catch (Exception exception) {
      throw new RuntimeException("download failed: url=" + urlSpec + " folder="+folder, exception);
    }
  }
}
