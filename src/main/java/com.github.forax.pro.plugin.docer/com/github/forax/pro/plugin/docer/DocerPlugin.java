package com.github.forax.pro.plugin.docer;


import static com.github.forax.pro.api.MutableConfig.derive;
import static com.github.forax.pro.api.helper.OptionAction.action;
import static com.github.forax.pro.api.helper.OptionAction.actionMaybe;
import static com.github.forax.pro.api.helper.OptionAction.exists;
import static com.github.forax.pro.api.helper.OptionAction.gatherAll;
import static com.github.forax.pro.api.helper.OptionAction.rawValues;
import static com.github.forax.pro.helper.FileHelper.pathFilenameEndsWith;
import static com.github.forax.pro.helper.FileHelper.walkIfNecessary;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.CmdLine;
import com.github.forax.pro.api.helper.OptionAction;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.FileHelper;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.ModuleHelper;
import com.github.forax.pro.helper.util.StableList;

public class DocerPlugin implements Plugin {
  @Override
  public String name() {
    return "docer";
  }

  @Override
  public void init(MutableConfig config) {
    var docerConf = config.getOrUpdate(name(), DocerConf.class);
    docerConf.generateTestDoc(false);
    docerConf.quiet(false);
    docerConf.html5(true);
  }
  
  @Override
  public void configure(MutableConfig config) {
    var docerConf = config.getOrUpdate(name(), DocerConf.class);
    var convention = config.getOrThrow("convention", ConventionFacade.class);
    
    // inputs
    derive(docerConf, DocerConf::moduleDependencyPath, convention, ConventionFacade::javaModuleDependencyPath);
    derive(docerConf, DocerConf::moduleSourcePath, convention, ConventionFacade::javaModuleSourcePath);
    derive(docerConf, DocerConf::moduleMergedTestPath, convention, ConventionFacade::javaModuleMergedTestPath);
    
    // outputs
    derive(docerConf, DocerConf::moduleDocSourcePath, convention, ConventionFacade::javaModuleDocSourcePath);
    derive(docerConf, DocerConf::moduleDocTestPath, convention, ConventionFacade::javaModuleDocTestPath);
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    var docerConf = config.getOrThrow(name(), DocerConf.class);
    docerConf.moduleDependencyPath().forEach(registry::watch);
    docerConf.moduleSourcePath().forEach(registry::watch);
    docerConf.moduleMergedTestPath().forEach(registry::watch);
  }
  
  static Optional<List<Path>> modulePathOrDependencyPath(Optional<List<Path>> modulePath, List<Path> moduleDependencyPath, List<Path> additionnalPath) {
    return modulePath
             .or(() -> Optional.of(
                    StableList.from(moduleDependencyPath).appendAll(additionnalPath)))
             .map(FileHelper.unchecked(FileHelper::pathFromFilesThatExist))
             .filter(list -> !list.isEmpty());
  }
  
  enum JavadocOption {
    RAW_ARGUMENTS(rawValues(Javadoc::rawArguments)),
    DESTINATION(action("-d", Javadoc::destination)),
    MODULE_SOURCE_PATH(action("--module-source-path", Javadoc::moduleSourcePath, File.pathSeparator)),
    ROOT_MODULES(actionMaybe("--add-modules", Javadoc::rootModules, ",")),
    UPGRADE_MODULE_PATH(actionMaybe("--upgrade-module-path", Javadoc::upgradeModulePath, File.pathSeparator)),
    MODULE_PATH(actionMaybe("--module-path", Javadoc::modulePath, File.pathSeparator)),
    QUIET(exists("-quiet", Javadoc::quiet)),
    HTML5(exists("-html5", Javadoc::html5)),
    LINK(actionMaybe("-link", Javadoc::link))
    ;
    
    final OptionAction<Javadoc> action;
    
    private JavadocOption(OptionAction<Javadoc> action) {
      this.action = action;
    }
  }
  
  @Override
  public int execute(Config config) throws IOException {
    var log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    
    var javadocTool = ToolProvider.findFirst("javadoc")
        .orElseThrow(() -> new IllegalStateException("can not find javadoc"));
    var docerConf = config.getOrThrow(name(), DocerConf.class);
    
    var moduleSourcePath = FileHelper.pathFromFilesThatExist(docerConf.moduleSourcePath());
    var moduleSourceFinder = ModuleHelper.sourceModuleFinders(moduleSourcePath);
    var errorCode = generateAll(moduleSourceFinder, docerConf.moduleDocSourcePath(),
        (input, output) -> generateDoc(log, javadocTool, docerConf, input, output));
    if (errorCode != 0) {
      return errorCode;
    }
    var moduleTestPath = FileHelper.pathFromFilesThatExist(docerConf.moduleMergedTestPath());
    if (!docerConf.generateTestDoc() || moduleTestPath.isEmpty()) {
      return 0;
    }
    
    var moduleTestFinder = ModuleHelper.sourceModuleFinders(moduleTestPath);
    return generateAll(moduleTestFinder, docerConf.moduleDocTestPath(),
        (input, output) -> generateDoc(log, javadocTool, docerConf, input, output));
  }

  interface Action {
    int apply(Path input, Path output);
  }
  
  private static int generateAll(ModuleFinder finder, Path output, Action action) throws IOException {
    FileHelper.deleteAllFiles(output, false);
    Files.createDirectories(output);
    
    var modules = finder.findAll().stream().flatMap(module -> module.location().stream()).collect(toList());
    return modules.parallelStream()
        .mapToInt(location -> action.apply(Paths.get(location), output))
        .reduce(0, (exitCode1, exitCode2) -> exitCode1 | exitCode2);
  }
  
  private static int generateDoc(Log log, ToolProvider javadocTool, DocerConf docerConf, Path input, Path output) {
    var javadoc = new Javadoc(output.resolve(input.getFileName().toString()), docerConf.moduleSourcePath());
    docerConf.rawArguments().ifPresent(javadoc::rawArguments);
    javadoc.modulePath(docerConf.moduleDependencyPath());
    docerConf.upgradeModulePath().ifPresent(javadoc::upgradeModulePath);
    docerConf.rootModules().ifPresent(javadoc::rootModules);
    javadoc.quiet(docerConf.quiet());
    javadoc.html5(docerConf.html5());
    docerConf.link().filter(url -> isLinkHostOnline(log, url)).ifPresent(javadoc::link);
    
    var cmdLine = gatherAll(JavadocOption.class, option -> option.action).apply(javadoc, new CmdLine());
    var files = docerConf.files().orElseGet(
        () -> walkIfNecessary(List.of(input), pathFilenameEndsWith(".java")));  //FIXME, use rootNames ??
    files.forEach(cmdLine::add);
    var arguments = cmdLine.toArguments();
    log.verbose(files, fs -> OptionAction.toPrettyString(JavadocOption.class, option -> option.action).apply(javadoc, "javadoc") + "\n" + fs.stream().map(Path::toString).collect(Collectors.joining(" ")));
    
    return javadocTool.run(System.out, System.err, arguments);
  }
  
  private static boolean isLinkHostOnline(Log log, URI uri) {
    var host = uri.getHost();
    try {  
      InetAddress.getByName(host);
      return true;
    } catch (@SuppressWarnings("unused") IOException e) {
      log.info(host, t -> "link: could not reach host " + host);
      return false;
    }
  }
}
