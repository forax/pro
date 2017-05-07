package com.github.forax.pro.plugin.formatter;

import static com.github.forax.pro.api.MutableConfig.derive;
import static com.github.forax.pro.helper.FileHelper.pathFilenameEndsWith;
import static com.github.forax.pro.helper.FileHelper.pathFilenameEquals;
import static com.github.forax.pro.helper.FileHelper.walkIfNecessary;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.Platform;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    derive(formatter, FormatterConf::moduleSourcePath, convention, ConventionFacade::javaModuleSourcePath);
    derive(formatter, FormatterConf::moduleTestPath, convention, ConventionFacade::javaModuleTestPath);
  }

  private List<Path> listAllJavaFiles(FormatterConf formatter) {
    Predicate<Path> filter = pathFilenameEndsWith(".java");
    // TODO include "module-info.java" files, blocked by https://github.com/google/google-java-format/issues/75
    filter = filter.and(pathFilenameEquals("module-info.java").negate());

    List<Path> pathList = new ArrayList<>();
    pathList.addAll(formatter.moduleSourcePath());
    pathList.addAll(formatter.moduleTestPath());
    return walkIfNecessary(pathList, filter);
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    FormatterConf formatter = config.getOrThrow(name(), FormatterConf.class);
    formatter.moduleSourcePath().forEach(registry::watch);
    formatter.moduleTestPath().forEach(registry::watch);
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
    // no (raw) arguments will trigger default "format files to stdout" mode
    formatter.rawArguments().ifPresent(args -> args.forEach(cmdLine::add));
    log.verbose(cmdLine, CmdLine::toString);
    // files
    List<Path> files = formatter.files().orElseGet(() -> listAllJavaFiles(formatter));
    log.verbose(files, fs -> "files\n" + fs.stream().map(Path::toString).collect(Collectors.joining(" ")));
    files.forEach(cmdLine::add);

    Process process = new ProcessBuilder(cmdLine.toArguments()).redirectErrorStream(true).start();
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    process.getInputStream().transferTo(new PrintStream(captured, true, "UTF-8"));
    try {
      int errorCode = process.waitFor();
      dump(captured, System.out::print);
      return errorCode + captured.size();
    } catch (InterruptedException e) {
      return 1; // FIXME
    }
  }

  private static void dump(ByteArrayOutputStream data, Consumer<String> consumer) throws UnsupportedEncodingException {
    Optional.of(data.toString("UTF-8"))
            .filter(text -> !text.trim().isEmpty())
            .ifPresent(consumer);
  }
}
