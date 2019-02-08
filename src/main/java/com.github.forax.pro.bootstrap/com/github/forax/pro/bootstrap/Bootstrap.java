package com.github.forax.pro.bootstrap;

import static com.github.forax.pro.Pro.list;
import static com.github.forax.pro.Pro.local;
import static com.github.forax.pro.Pro.location;
import static com.github.forax.pro.Pro.path;
import static com.github.forax.pro.Pro.run;
import static com.github.forax.pro.Pro.set;
import static com.github.forax.pro.Pro.uri;
import static com.github.forax.pro.helper.FileHelper.deleteAllFiles;
import static com.github.forax.pro.helper.FileHelper.download;
import static com.github.forax.pro.helper.FileHelper.walkAndFindCounterpart;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.github.forax.pro.helper.ModuleHelper;

public class Bootstrap {

  // jdk 12 will see similar issues: https://bugs.openjdk.java.net/browse/JDK-8193784
  private static int jdkVersion() {
    var major = Runtime.version().feature();
    return /*Math.min(major, 11);*/ major; // weirdly jdk12 javac --release 11 doesn't currently work
  }

  public static void main(String[] args) throws IOException {
    set("pro.loglevel", "verbose");
    set("pro.exitOnError", true);

    set("compiler.lint", "all,-varargs,-overloads");
    set("compiler.sourceRelease", jdkVersion());

    var version = "0." + jdkVersion();
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
        "com.github.forax.pro.plugin.jmher@" + version,
        "com.github.forax.pro.plugin.bootstrap@" + version + "/com.github.forax.pro.bootstrap.Bootstrap",
        "com.github.forax.pro.bootstrap.genbuilder@" + version + "/com.github.forax.pro.bootstrap.genbuilder.GenBuilder",
        "com.github.forax.pro.daemon@" + version,
        "com.github.forax.pro.daemon.imp@" + version
        ));

    // ask the resolver to find new versions of the dependencies
    set("resolver.checkForUpdate", true);
    
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

    set("docer.quiet", true);
    set("docer.link", uri("https://docs.oracle.com/en/java/javase/11/docs/api/"));

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
        "com.github.forax.pro.daemon.imp"
        )                                             // then add all system modules
        .appendAll(ModuleHelper.systemModulesFinder().findAll().stream()
                  .map(ref -> ref.descriptor().name())
                  .collect(Collectors.toSet())));
    
    run("modulefixer", "compiler", "docer", "packager");

    compileAndPackagePlugin("runner", list("resolver", "modulefixer", "compiler", "packager"), () -> { /* empty */});
    compileAndPackagePlugin("tester", list("resolver", "modulefixer", "compiler", "packager"), () -> {
      // set("resolver.remoteRepositories", list(uri("https://oss.sonatype.org/content/repositories/snapshots")));
      var junitPlatformVersion = "1.4.0-RC1";
      var junitJupiterVersion = "5.4.0-RC1";
      var opentest4jVersion = "1.1.1";
      var apiGuardianVersion = "1.0.0";
      set("resolver.dependencies", list(
          // "API"
          "org.opentest4j=org.opentest4j:opentest4j:" + opentest4jVersion,
          "org.apiguardian=org.apiguardian:apiguardian-api:" + apiGuardianVersion,
          "org.junit.platform.commons=org.junit.platform:junit-platform-commons:" + junitPlatformVersion,
          "org.junit.jupiter.api=org.junit.jupiter:junit-jupiter-api:" + junitJupiterVersion,
          // "Launcher + Engine"
          "org.junit.platform.engine=org.junit.platform:junit-platform-engine:" + junitPlatformVersion,
          "org.junit.platform.launcher=org.junit.platform:junit-platform-launcher:" + junitPlatformVersion,
          "org.junit.platform.reporting=org.junit.platform:junit-platform-reporting:" + junitPlatformVersion,
          "org.junit.jupiter.engine=org.junit.jupiter:junit-jupiter-engine:" + junitJupiterVersion
      ));
    });
    compileAndPackagePlugin("perfer", list("resolver", "modulefixer", "compiler", "packager"), () -> {
      var jmhVersion = "1.21";
      var commonMath3Version = "3.6.1";
      var joptSimpleVersion = "5.0.4";
      set("resolver.dependencies", list(
          // "JMH Core"
          "org.openjdk.jmh=org.openjdk.jmh:jmh-core:" + jmhVersion,
          "org.apache.commons.math3=org.apache.commons:commons-math3:" + commonMath3Version,
          "net.sf.jopt-simple=net.sf.jopt-simple:jopt-simple:" + joptSimpleVersion
      ));
    });

    // https://github.com/google/google-java-format/issues/266
    //    compileAndPackagePlugin("formatter", () -> {
    //      set(
    //        "resolver.remoteRepositories",
    //        list(uri("https://oss.sonatype.org/content/repositories/snapshots")));
    //      String gjfVersion = "1.6";
    //      String guavaVersion = "24.1";
    //      String javacShadedVersion = "9+181-r4173-1";
    //      set("resolver.dependencies", list(
    //          // "Google Java Format"
    //          "com.google.googlejavaformat=com.google.googlejavaformat:google-java-format:" +
    // gjfVersion,
    //          "com.google.guava=com.google.guava:guava:" + guavaVersion,
    //          "com.google.errorprone=com.google.errorprone:javac-shaded:" + javacShadedVersion,
    //          "com.google.j2objc=com.google.j2objc:j2objc-annotations:1.1",
    // "org.codehaus.animal.sniffer.annotations=org.codehaus.mojo:animal-sniffer-annotations:1.14"
    //      ));
    //    });
    compileAndPackagePlugin("formatter", list("compiler", "packager"), () -> {
      var gjfVersion = "1.7";
      var base = "https://github.com/google/google-java-format/releases/download/google-java-format";
      download(
          uri(base + "-" + gjfVersion + "/google-java-format-" + gjfVersion + "-all-deps.jar"),
          location("plugins/formatter/libs"));
    });

    compileAndPackagePlugin("frozer", list("resolver", "modulefixer", "compiler", "packager"), () -> { /* empty */});
    
    run("linker" /*, "uberpackager" */);

    copyPackagedPluginToTargetImage("runner");
    copyPackagedPluginToTargetImage("tester");
    copyPackagedPluginToTargetImage("perfer");
    copyPackagedPluginToTargetImage("formatter");
    copyPackagedPluginToTargetImage("frozer");

    // re-generate builders
    //com.github.forax.pro.Pro.loadPlugins(java.nio.file.Path.of("target/image/plugins"));
    //com.github.forax.pro.bootstrap.genbuilder.GenBuilder.generate();
    
    // update module name to maven coordinates list
    //updateModuleNameList();
    
    Vanity.postOperations();
  }

  
  private static void updateModuleNameList() {
    var moduleNameListURI = uri("https://raw.githubusercontent.com/jodastephen/jpms-module-names/master/generated/module-maven.properties");
    var resolverResourceFile = location("src/main/resources/com.github.forax.pro.plugin.resolver/com/github/forax/pro/plugin/resolver/module-maven.properties");
    
    try {
      var tmpDir = createTempDirectory("pro");
      download(moduleNameListURI, tmpDir); 
      System.out.println("download " + moduleNameListURI);
      createDirectories(resolverResourceFile.getParent());
      move(tmpDir.resolve("module-maven.properties"),
          resolverResourceFile,
          REPLACE_EXISTING);
    } catch(IOException | UncheckedIOException e) {
      System.err.println("can not update module name list " + e.getMessage());
    }
  }
  
  private static void compileAndPackagePlugin(String name, List<String> plugins, Runnable extras) throws IOException {
    deleteAllFiles(location("plugins/" + name + "/target"), false);

    local("plugins/" + name, () -> {
      set("resolver.moduleDependencyPath",
          path("plugins/" + name + "/deps", "target/main/artifact/", "deps"));
      set("compiler.moduleDependencyPath",
          path("plugins/" + name + "/deps", "target/main/artifact/", "deps"));

      extras.run();

      run(plugins);
    });
  }

  private static void copyPackagedPluginToTargetImage(String name) throws IOException {
    createDirectories(location("target/image/plugins/" + name));
    path("plugins/" + name + "/target/main/artifact", "plugins/" + name + "/deps")
        .filter(Files::exists)
        .forEach(
            srcPath ->
                walkAndFindCounterpart(
                    srcPath,
                    location("target/image/plugins/" + name),
                    stream -> stream.filter(p -> p.toString().endsWith(".jar")),
                    Files::copy));
    if (Files.exists(Path.of("plugins/" + name + "/libs"))) {
      createDirectories(location("target/image/plugins/" + name + "/libs"));
      path("plugins/" + name + "/libs")
          .filter(Files::exists)
          .forEach(
              srcPath ->
                  walkAndFindCounterpart(
                      srcPath,
                      location("target/image/plugins/" + name + "/libs"),
                      stream -> stream.filter(p -> p.toString().endsWith(".jar")),
                      Files::copy));
    }
  }
}
