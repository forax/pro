package com.github.forax.pro.plugin.formatter;

import static com.github.forax.pro.helper.FileHelper.pathFilenameEndsWith;
import static com.github.forax.pro.helper.FileHelper.pathFilenameEquals;
import static com.github.forax.pro.helper.FileHelper.walkIfNecessary;

import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.helper.Platform;
import java.io.IOException;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.Log;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class FormatterPlugin implements Plugin {
  @Override
  public String name() {
    return "formatter";
  }

  @Override
  public void init(MutableConfig config) {
    config.getOrUpdate(name(), FormatterConf.class);
  }
  
  @Override
  public void configure(MutableConfig config) {
    FormatterConf formatter = config.getOrUpdate(name(), FormatterConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);
  }

  private List<Path> listAllJavaFiles(ConventionFacade convention) {
    Predicate<Path> filter = pathFilenameEndsWith(".java");
    // TODO include "module-info.java" files, blocked by https://github.com/google/google-java-format/issues/75
    filter = filter.and(pathFilenameEquals("module-info.java").negate());

    List<Path> pathList = new ArrayList<>();
    pathList.addAll(convention.javaModuleSourcePath());
    pathList.addAll(convention.javaModuleTestPath());
    return walkIfNecessary(pathList, filter);
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    FormatterConf formatter = config.getOrThrow(name(), FormatterConf.class);
  }

  @Override
  public int execute(Config config) throws IOException {
    Log log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);
    FormatterConf formatter = config.getOrThrow(name(), FormatterConf.class);
    log.debug(formatter, _formatter -> "config " + _formatter);

    CmdLine cmdLine = new CmdLine();
    cmdLine.add(convention.javaHome().resolve("bin").resolve(Platform.current().javaExecutableName()));
    cmdLine.add("--module-path");
    cmdLine.add(convention.javaHome().resolve("plugins").resolve(name()).resolve("libs"));
    cmdLine.add("--module");
    cmdLine.add("google.java.format");
    // TODO add more options, see details at https://github.com/google/google-java-format/blob/master/core/src/main/java/com/google/googlejavaformat/java/UsageException.java#L30
    // cmdLine.add("--replace");
    // TODO make javaFiles() configurable
    listAllJavaFiles(convention).forEach(cmdLine::add);

    Process process = new ProcessBuilder(cmdLine.toArguments()).redirectErrorStream(true).start();
    process.getInputStream().transferTo(System.out);
    try {
      return process.waitFor();
    } catch (InterruptedException e) {
      return 1; // FIXME
    }
  }

}
