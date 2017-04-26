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
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    String[] options = buildDefaultConsoleLauncherOptions();
    Arrays.stream(options).forEach(System.out::println);

    Thread.currentThread().setContextClassLoader(TestEngine.class.getClassLoader());
    int exitCode = ConsoleLauncher.execute(
        new PrintStream(out, true, "UTF-8"),
        new PrintStream(err, true, "UTF-8"),
        options
    ).getExitCode();

    System.out.println(out);
    System.err.println(err);

    return exitCode;
  }

  String[] buildDefaultConsoleLauncherOptions() {
    List<String> options = new ArrayList<>();
    // scan available exploded test directories
    options.add("--scan-classpath");
    Consumer<String> addClassPath = path -> {
      options.add("--classpath");
      options.add(path);
    };
    findDirectoriesAt("target/test/exploded").forEach(addClassPath);
    findDirectoriesAt("plugins")
        .forEach(plugin -> findDirectoriesAt(plugin + "/target/test/exploded")
            .forEach(addClassPath));
    return options.toArray(new String[options.size()]);
  }

  List<String> findDirectoriesAt(String base) {
    List<String> directories = new ArrayList<>();
    Path parent = Paths.get(base);
    DirectoryStream.Filter<Path> filter = path -> path.toFile().isDirectory();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(parent, filter)) {
      for (Path path : directoryStream) {
        directories.add(path.toString());
      }
    } catch (IOException ex) {
      // ignore
    }
    return directories;
  }
}
