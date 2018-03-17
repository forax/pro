package com.github.forax.pro.plugin.formatter;

import static com.github.forax.pro.api.MutableConfig.derive;
import static com.github.forax.pro.helper.FileHelper.pathFilenameEndsWith;
import static com.github.forax.pro.helper.FileHelper.walkIfNecessary;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.Platform;
import com.github.forax.pro.helper.util.StableList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FormatterPlugin implements Plugin {
  @Override
  public String name() {
    return "formatter";
  }

  @Override
  public void init(MutableConfig config) {
    FormatterConf formatter = config.getOrUpdate(name(), FormatterConf.class);
    formatter.replace(false);
    formatter.dryRun(false);
    formatter.setExitIfChanged(false);
  }
  
  @Override
  public void configure(MutableConfig config) {
    FormatterConf formatter = config.getOrUpdate(name(), FormatterConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);

    derive(formatter, FormatterConf::moduleSourcePath, convention, ConventionFacade::javaModuleSourcePath);
    derive(formatter, FormatterConf::moduleTestPath, convention, ConventionFacade::javaModuleTestPath);
  }

  private static List<Path> listAllJavaFiles(FormatterConf formatter) {
    StableList<Path> sourcePaths = StableList.<Path>of()
        .appendAll(formatter.moduleSourcePath())
        .appendAll(formatter.moduleTestPath());
    return walkIfNecessary(sourcePaths, pathFilenameEndsWith(".java"));
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
    cmdLine.add("--module").add("google.java.format");
    
    Optional.of("--replace").filter(__ -> formatter.replace()).ifPresent(cmdLine::add);
    Optional.of("--dry-run").filter(__ -> formatter.dryRun()).ifPresent(cmdLine::add);
    Optional.of("--set-exit-if-changed").filter(__ -> formatter.setExitIfChanged()).ifPresent(cmdLine::add);
    
    // no arguments will trigger default "format files to stdout" mode
    formatter.rawArguments().ifPresent(args -> args.forEach(cmdLine::add));
    log.verbose(cmdLine, CmdLine::toString);
    
    // files
    List<Path> files = formatter.files().orElseGet(() -> listAllJavaFiles(formatter));
    log.debug(files, fs -> "files:\n" + fs.stream().map(Path::toString).collect(Collectors.joining(" ")));
    files.forEach(cmdLine::add);

    Process process = new ProcessBuilder(cmdLine.toArguments()).redirectErrorStream(true).start();
    process.getInputStream().transferTo(System.out);
    try {
      return process.waitFor();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }
}
