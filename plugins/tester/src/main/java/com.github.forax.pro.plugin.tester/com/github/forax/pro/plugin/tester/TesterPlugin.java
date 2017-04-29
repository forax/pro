package com.github.forax.pro.plugin.tester;

import static com.github.forax.pro.api.MutableConfig.derive;
import static com.github.forax.pro.api.helper.OptionAction.actionLoop;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.platform.console.ConsoleLauncher;
import org.junit.platform.engine.TestEngine;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.OptionAction;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.Log;

public class TesterPlugin implements Plugin {
  @Override
  public String name() {
    return "tester";
  }

  @Override
  public void init(MutableConfig config) {
    config.getOrUpdate(name(), TesterConf.class);
  }
  
  @Override
  public void configure(MutableConfig config) {
    TesterConf tester = config.getOrUpdate(name(), TesterConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);
    
    // inputs
    derive(tester, TesterConf::moduleExplodedTestPath, convention, ConventionFacade::javaModuleExplodedTestPath);
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    TesterConf testerConf = config.getOrThrow(name(), TesterConf.class);
    testerConf.moduleExplodedTestPath().forEach(registry::watch);
  }

  static List<Path> directories(TesterConf tester) {
    return ModuleFinder.of(tester.moduleExplodedTestPath().toArray(new Path[0]))
      .findAll()
      .stream()
      .flatMap(ref -> ref.location().stream())
      .map(Paths::get)
      .collect(Collectors.toList());
  }
  
  enum ConsoleLauncherOption {
    // DISABLE_ANSI_COLORS(config -> Optional.of(line -> line.add("--disable-ansi-colors"))),
    SCAN_CLASSPATH(actionLoop("--scan-classpath", TesterPlugin::directories)),
    CLASSPATH(actionLoop("--classpath", TesterPlugin::directories))
    ;
    
    final OptionAction<TesterConf> action;
    
    private ConsoleLauncherOption(OptionAction<TesterConf> action) {
      this.action = action;
    }
  }
  
  @Override
  public int execute(Config config) throws IOException {
    Log log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    TesterConf tester = config.getOrThrow(name(), TesterConf.class);
    log.debug(tester, _tester -> "config " + _tester);

    String[] arguments = tester.overrideArguments()
      .map(overrideArguments -> {
        log.verbose(overrideArguments, _arguments -> String.join("\n", _arguments));
        return overrideArguments.toArray(new String[0]);
      })
      .orElseGet(() -> {
        log.verbose(null, __ -> OptionAction.toPrettyString(ConsoleLauncherOption.class, option -> option.action).apply(tester, "tester"));
        return OptionAction.gatherAll(ConsoleLauncherOption.class, option -> option.action).apply(tester, new CmdLine()).toArguments();
      });

    Thread currentThread = Thread.currentThread();
    ClassLoader oldContext = currentThread.getContextClassLoader();
    try {
      currentThread.setContextClassLoader(TestEngine.class.getClassLoader());
      return executeConsoleLauncher(arguments);
    } finally {
      currentThread.setContextClassLoader(oldContext); // restore the context
    }
  }

  private static void dump(ByteArrayOutputStream data, Consumer<String> consumer) throws UnsupportedEncodingException {
    Optional.of(data.toString("UTF-8"))
      .filter(text -> !text.trim().isEmpty())
      .ifPresent(consumer);
  }
  
  private static int executeConsoleLauncher(String[] arguments) throws IOException {
    ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
    ByteArrayOutputStream dataErr = new ByteArrayOutputStream();
    
    int exitCode = ConsoleLauncher.execute(
        new PrintStream(dataOut, true, "UTF-8"),
        new PrintStream(dataErr, true, "UTF-8"),
        arguments
        ).getExitCode();
    
    dump(dataOut, System.out::print);
    dump(dataErr, System.err::print);
    
    return exitCode;
  }
}
