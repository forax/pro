package com.github.forax.pro.bootstrap;

import static com.github.forax.pro.Pro.*;
import static com.github.forax.pro.helper.FileHelper.deleteAllFiles;
import static com.github.forax.pro.helper.FileHelper.walkAndFindCounterpart;
import static java.nio.file.Files.createDirectories;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

import com.github.forax.pro.helper.ModuleHelper;

public class Bootstrap {
  @SuppressWarnings("deprecation")
  private static int jdkVersion() {
    return Runtime.version().major();
  }
  
  public static void main(String[] args) throws IOException {
    set("pro.loglevel", "verbose");
    set("pro.exitOnError", true);

    //set("compiler.lint", "exports,module");
    set("compiler.lint", "all,-varargs,-overloads");
    set("compiler.release", jdkVersion());

    String version = "0." + jdkVersion();
    set("packager.modules", list(
        "com.github.forax.pro@" + version,
        "com.github.forax.pro.aether@" + version,
        "com.github.forax.pro.ather.fakeguava@" + version,
        "com.github.forax.pro.api@" + version,
        "com.github.forax.pro.bootstrap@" + version,
        "com.github.forax.pro.builder@" + version,
        "com.github.forax.pro.helper@" + version,
        "com.github.forax.pro.main@" + version + "/com.github.forax.pro.main.Main",
        "com.github.forax.pro.plugin.convention@" + version,
        "com.github.forax.pro.plugin.resolver@" + version,
        "com.github.forax.pro.plugin.modulefixer@" + version,
        "com.github.forax.pro.plugin.compiler@" + version,
        "com.github.forax.pro.plugin.docer@" + version,
        "com.github.forax.pro.plugin.packager@" + version,
        "com.github.forax.pro.plugin.linker@" + version,
        "com.github.forax.pro.plugin.runner@" + version,
        "com.github.forax.pro.plugin.tester@" + version,
        "com.github.forax.pro.plugin.uberpackager@" + version,
        "com.github.forax.pro.plugin.bootstrap@" + version + "/com.github.forax.pro.bootstrap.Bootstrap",
        "com.github.forax.pro.bootstrap.genbuilder@" + version + "/com.github.forax.pro.bootstrap.genbuilder.GenBuilder",
        "com.github.forax.pro.ubermain@" + version,
        "com.github.forax.pro.uberbooter@" + version,
        "com.github.forax.pro.daemon@" + version,
        "com.github.forax.pro.daemon.imp@" + version
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
        "com.github.forax.pro.plugin.docer",
        "com.github.forax.pro.plugin.packager",
        "com.github.forax.pro.plugin.linker",
        "com.github.forax.pro.plugin.uberpackager",
        "com.github.forax.pro.uberbooter",            // needed by ubermain
        "com.github.forax.pro.daemon.imp"
        )                                             // then add all system modules
        .appendAll(ModuleHelper.systemModulesFinder().findAll().stream()
                  .map(ref -> ref.descriptor().name())
                  .collect(Collectors.toSet())));

    run("modulefixer", "compiler", "docer", "packager");

    compileAndPackagePlugin("runner", () -> { /* empty */});
    compileAndPackagePlugin("tester", () -> {
      set("resolver.remoteRepositories", list(
        uri("https://oss.sonatype.org/content/repositories/snapshots")
      ));
      String junitPlatformVersion = "1.0.0";
      String junitJupiterVersion = "5.0.0";
      String opentest4jVersion = "1.0.0";
      String apiGuardianVersion = "1.0.0";
      set("resolver.dependencies", list(
          // "API"
          "org.opentest4j=org.opentest4j:opentest4j:" + opentest4jVersion,
          "org.apiguardian=org.apiguardian:apiguardian-api:" + apiGuardianVersion,
          "org.junit.platform.commons=org.junit.platform:junit-platform-commons:" + junitPlatformVersion,
          "org.junit.jupiter.api=org.junit.jupiter:junit-jupiter-api:" + junitJupiterVersion,
          // "Launcher + Engine"
          "org.junit.platform.engine=org.junit.platform:junit-platform-engine:" + junitPlatformVersion,
          "org.junit.platform.launcher=org.junit.platform:junit-platform-launcher:" + junitPlatformVersion,
          "org.junit.jupiter.engine=org.junit.jupiter:junit-jupiter-engine:" + junitJupiterVersion
      ));
    });

    run("linker"/*, "uberpackager" */);

    copyPackagedPluginToTargetImage("runner");
    copyPackagedPluginToTargetImage("tester");

    // re-generate builders
    //update(java.nio.file.Paths.get("target/image/plugins"));
    //com.github.forax.pro.bootstrap.genbuilder.GenBuilder.generate();

    Vanity.postOperations();
  }

  private static void compileAndPackagePlugin(String name, Runnable extras) throws IOException {
    deleteAllFiles(location("plugins/" + name + "/target"), false);

    local("plugins/" + name, () -> {
      set("resolver.moduleDependencyPath",
          path("plugins/" + name + "/deps", "target/main/artifact/", "deps"));
      set("compiler.moduleDependencyPath",
          path("plugins/" + name + "/deps", "target/main/artifact/", "deps"));

      extras.run();

      run("resolver", "modulefixer", "compiler", "packager");
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
  }
}
