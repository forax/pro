package com.github.forax.pro.plugin.formatter;

import static com.github.forax.pro.api.MutableConfig.derive;
import static com.github.forax.pro.helper.FileHelper.pathFilenameEndsWith;
import static com.github.forax.pro.helper.FileHelper.walkIfNecessary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.Platform;
import com.github.forax.pro.helper.util.StableList;

public class FormatterPlugin implements Plugin {
  @Override
  public String name() {
    return "formatter";
  }

  @Override
  public void init(MutableConfig config) {
    var formatterConf = config.getOrUpdate(name(), FormatterConf.class);
    formatterConf.replace(false);
    formatterConf.dryRun(false);
    formatterConf.setExitIfChanged(false);
  }
  
  @Override
  public void configure(MutableConfig config) {
    var formatterConf = config.getOrUpdate(name(), FormatterConf.class);
    var convention = config.getOrThrow("convention", ConventionFacade.class);

    derive(formatterConf, FormatterConf::moduleSourcePath, convention, ConventionFacade::javaModuleSourcePath);
    derive(formatterConf, FormatterConf::moduleTestPath, convention, ConventionFacade::javaModuleTestPath);
  }

  private static List<Path> listAllJavaFiles(FormatterConf formatter) {
    var sourcePaths = StableList.<Path>of()
        .appendAll(formatter.moduleSourcePath())
        .appendAll(formatter.moduleTestPath());
    return walkIfNecessary(sourcePaths, pathFilenameEndsWith(".java"));
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    var formatterConf = config.getOrThrow(name(), FormatterConf.class);
    formatterConf.moduleSourcePath().forEach(registry::watch);
    formatterConf.moduleTestPath().forEach(registry::watch);
  }

  @Override
  public int execute(Config config) throws IOException {
    var log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    var convention = config.getOrThrow("convention", ConventionFacade.class);
    var formatterConf = config.getOrThrow(name(), FormatterConf.class);
    log.debug(formatterConf, _formatterConf -> "config " + _formatterConf);

    var cmdLine = new CmdLine();
    cmdLine.add(convention.javaHome().resolve("bin").resolve(Platform.current().javaExecutableName()));
    cmdLine.add("--module-path");
    cmdLine.add(convention.javaHome().resolve("plugins").resolve(name()).resolve("libs"));
    cmdLine.add("--module").add("google.java.format");
    
    Optional.of("--replace").filter(__ -> formatterConf.replace()).ifPresent(cmdLine::add);
    Optional.of("--dry-run").filter(__ -> formatterConf.dryRun()).ifPresent(cmdLine::add);
    Optional.of("--set-exit-if-changed").filter(__ -> formatterConf.setExitIfChanged()).ifPresent(cmdLine::add);
    
    // no arguments will trigger default "format files to stdout" mode
    formatterConf.rawArguments().ifPresent(args -> args.forEach(cmdLine::add));
    log.verbose(cmdLine, CmdLine::toString);
    
    // files
    var files = formatterConf.files().orElseGet(() -> listAllJavaFiles(formatterConf));
    log.debug(files, fs -> "files:\n" + fs.stream().map(Path::toString).collect(Collectors.joining(" ")));
    files.forEach(cmdLine::add);

    var process = new ProcessBuilder(cmdLine.toArguments()).redirectErrorStream(true).start();
    process.getInputStream().transferTo(System.out);
    try {
      return process.waitFor();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }
}
