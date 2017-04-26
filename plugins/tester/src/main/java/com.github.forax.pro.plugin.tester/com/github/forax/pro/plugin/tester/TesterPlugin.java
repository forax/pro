package com.github.forax.pro.plugin.tester;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.platform.console.ConsoleLauncher;
import org.junit.platform.engine.TestEngine;

public class TesterPlugin implements Plugin {
  @Override
  public String name() {
    return "tester";
  }

  @Override
  public void init(MutableConfig config) {
  }
  
  @Override
  public void configure(MutableConfig config) {
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
  }

  @Override
  public int execute(Config config) throws IOException {
    String[] options = buildDefaultConsoleLauncherOptions(config);
    Arrays.stream(options).forEach(System.out::println);

    Thread currentThread = Thread.currentThread();
    ClassLoader oldContext = currentThread.getContextClassLoader();
    try {
      currentThread.setContextClassLoader(TestEngine.class.getClassLoader());
      return executeConsoleLauncher(options);
    } finally {
      currentThread.setContextClassLoader(oldContext); // restore the context
    }
  }

  int executeConsoleLauncher(String[] options) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    int exitCode = ConsoleLauncher.execute(
        new PrintStream(out, true, "UTF-8"),
        new PrintStream(err, true, "UTF-8"),
        options
    ).getExitCode();
    toOptionalString(out).ifPresent(System.out::print);
    toOptionalString(err).ifPresent(System.err::print);
    return exitCode;
  }

  Optional<String> toOptionalString(Object object) {
    if (object == null) {
      return Optional.of("null");
    }
    String string = object.toString();
    if (string == null || string.trim().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(string);
  }

  String[] buildDefaultConsoleLauncherOptions(Config config) {
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);

    List<String> options = new ArrayList<>();
    // scan available exploded test directories
    options.add("--scan-classpath");
    Consumer<Path> addClassPath = path -> {
      options.add("--classpath");
      options.add(path.toString());
    };
    List<Path> javaModuleExplodedTestPaths = convention.javaModuleExplodedTestPath();
    javaModuleExplodedTestPaths.forEach(path -> findDirectoriesAt(path).forEach(addClassPath));
    // internal pro-convention: plugins are in "plugins" folder and obey default target convention paths
    findDirectoriesAt(Paths.get("plugins"))
        .forEach(plugin -> findDirectoriesAt(plugin.resolve("target/test/exploded"))
            .forEach(addClassPath));
    return options.toArray(new String[options.size()]);
  }

  List<Path> findDirectoriesAt(Path base) {
    List<Path> directories = new ArrayList<>();
    DirectoryStream.Filter<Path> filter = path -> path.toFile().isDirectory();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(base, filter)) {
      for (Path path : directoryStream) {
        directories.add(path);
      }
    } catch (IOException ex) {
      // ignore
    }
    return directories;
  }
}
