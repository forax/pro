package com.github.forax.pro.plugin.modulefixer;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Modifier;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.FileHelper;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.ModuleHelper;

public class ModuleFixerPlugin implements Plugin {
  @Override
  public String name() {
    return "modulefixer";
  }

  @Override
  public void init(MutableConfig config) {
    ModuleFixerConf moduleFixer = config.getOrUpdate(name(), ModuleFixerConf.class);
    moduleFixer.force(false);
  }
  
  @Override
  public void configure(MutableConfig config) {
    ModuleFixerConf moduleFxier = config.getOrUpdate(name(), ModuleFixerConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);
    
    // inputs
    moduleFxier.moduleDependencyPath(convention.javaModuleDependencyPath());
    
    // outputs
    //TODO
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    ModuleFixerConf moduleFixer = config.getOrThrow(name(), ModuleFixerConf.class);
    List<Path> moduleDependencyPath = moduleFixer.moduleDependencyPath();
    moduleDependencyPath.forEach(registry::watch);
  }
  
  @Override
  public int execute(Config config) throws IOException {
    Log log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    
    ModuleFixerConf moduleFixer = config.getOrThrow(name(), ModuleFixerConf.class);
    List<Path> moduleDependencyPath = moduleFixer.moduleDependencyPath();
    Path moduleDependencyFixerPath = moduleFixer.moduleDependencyFixerPath().orElseGet(() -> Paths.get("target/deps/module-fixer"));  //FIXME
    
    class Info {
      final Set<String> requirePackages;
      final Set<String> exports;
      final Set<String> requireModules;
      
      Info(Set<String> requirePackages, Set<String> exports, Set<String> requireModules) {
        this.requirePackages = requirePackages;
        this.exports = exports;
        this.requireModules = requireModules;
      }
    }
    
    // gather additional requires
    boolean force = moduleFixer.force();
    Map<String, Set<String>> additionalRequireMap =
        moduleFixer.additionalRequires()
                   .orElse(List.of())
                   .stream()
                   .map(line -> line.split("="))
                   .collect(Collectors.groupingBy(tokens -> tokens[0], Collectors.mapping(tokens -> tokens[1], Collectors.toSet())));
    //System.out.println("additionalRequireMap " + additionalRequireMap);
    
    // find dependencies (requires and exports)
    HashMap<ModuleReference, Info> moduleInfoMap = new HashMap<>();
    HashMap<String, List<ModuleReference>> exportMap = new HashMap<>();
    ModuleFinder moduleFinder = ModuleFinder.compose(
        ModuleFinder.of(moduleDependencyPath.toArray(new Path[0])),
        ModuleFinder.ofSystem());
    for(ModuleReference ref: moduleFinder.findAll()) {
      Set<String> exports;
      ModuleDescriptor descriptor = ref.descriptor();
      String name = descriptor.name();
      if (descriptor.isAutomatic() || /*descriptor.isSynthetic() ||*/ (force && additionalRequireMap.containsKey(name))) {
        HashSet<String> requires = new HashSet<>();
        exports = new HashSet<>();
        findRequiresAndExports(ref, requires, exports);
        moduleInfoMap.put(ref,
            new Info(requires, exports,
                Optional.ofNullable(additionalRequireMap.get(name)).orElseGet(HashSet::new)));
      } else {
        exports = descriptor.exports().stream().map(export -> export.source().replace('.', '/')).collect(Collectors.toSet());
      }
      exports.forEach(packageName -> exportMap.computeIfAbsent(packageName, __ -> new ArrayList<>()).add(ref));
    }
    
    if (exportMap.isEmpty()) {
      return 0; // nothing todo
    }
    
    // verify split packages
    int errorCode = 0;
    for(Map.Entry<String, List<ModuleReference>> entry: exportMap.entrySet()) {
      String packageName = entry.getKey();
      List<String> refs = entry.getValue().stream().map(ref -> ref.descriptor().toNameAndVersion()).collect(toList());
      if (refs.size() != 1) {
        System.err.println("[modulefixer] multiple modules " + refs + " contains package " + packageName);
        errorCode = 1;
      }
    }
    if (errorCode != 0) {
      return errorCode;
    }
    
    //System.out.println(moduleInfoMap.entrySet().stream()
    //    .map(entry -> entry.getKey().descriptor().name() + " " + entry.getValue().requirePackages)
    //    .collect(Collectors.joining(", ", "{", "}")));
    
    // calculated module dependencies
    HashMap<String, List<ModuleReference>> unknownRequiredPackages = new HashMap<>();
    moduleInfoMap.forEach((ref, info) -> {
      Set<String> calculatedRequires = info.requirePackages.stream()
          .flatMap(requireName -> {
            List<ModuleReference> refs = exportMap.get(requireName);
            if (refs == null) {
              unknownRequiredPackages.computeIfAbsent(requireName, __ -> new ArrayList<>()).add(ref);
              return Stream.empty();
            }
            return Stream.of(refs.get(0).descriptor().name());
          })
          .collect(Collectors.toSet());
      info.requireModules.addAll(calculatedRequires);
    });
    unknownRequiredPackages.forEach((unknownRequiredPackage, modules) -> {
      String moduleNames = modules.stream().map(ref -> ref.descriptor().name()).collect(joining(", "));
      System.err.println("[modulefixer] package " + unknownRequiredPackage + " required by " + moduleNames + " not found");
    });
    //if (!unknownRequiredPackages.isEmpty()) {
    //  return 1;
    //}
    
    FileHelper.deleteAllFiles(moduleDependencyFixerPath, false);
    Files.createDirectories(moduleDependencyFixerPath);
    
    // patch jars with calculated module-info
    errorCode = 0;
    ToolProvider jarTool = ToolProvider.findFirst("jar")
        .orElseThrow(() -> new IllegalStateException("can not find jar"));
    for(Map.Entry<ModuleReference, Info> entry: moduleInfoMap.entrySet()) {
      ModuleReference ref = entry.getKey();
      Info info = entry.getValue();
      Path generatedModuleInfoPath = generateModuleInfo(ref, info.requireModules, info.exports, moduleDependencyFixerPath);
      
      if (errorCode == 0) { // stop to patch at the first error, but generate all module-info.class
        errorCode = patchModularJar(jarTool, ref, generatedModuleInfoPath);
      }
    }
    return errorCode;
  }

  private static int patchModularJar(ToolProvider jarTool, ModuleReference ref, Path generatedModuleInfo) {
    System.out.println("[modulefixer] fix " + ref.descriptor().name());
    return jarTool.run(System.out, System.err,
        "--update",
        "--file", Paths.get(ref.location().get()).toString(),
        "-C", generatedModuleInfo.getParent().toString(),
        generatedModuleInfo.getFileName().toString());
  }
  
  private static Path generateModuleInfo(ModuleReference ref, Set<String> requires, Set<String> exports, Path moduleDependencyFixerPath) throws IOException {
    String moduleName = ref.descriptor().name();
    
    //System.out.println(moduleName);
    //System.out.println("requires: " + requires);
    //System.out.println("exports: " + exports);
    
    Path modulePatchPath = moduleDependencyFixerPath.resolve(moduleName);
    Files.createDirectories(modulePatchPath);
    
    ModuleDescriptor.Builder builder = ModuleDescriptor.newModule(moduleName, Set.of(Modifier.OPEN));
    ref.descriptor().version().ifPresent(version -> builder.version(version.toString()));
    requires.forEach(builder::requires);
    exports.forEach(export -> builder.exports(export.replace('/', '.')));
    
    Path generatedModuleInfoPath = modulePatchPath.resolve("module-info.class");
    Files.write(generatedModuleInfoPath, ModuleHelper.moduleDescriptorToBinary(builder.build()));
    return generatedModuleInfoPath;
  }

  private static void findRequiresAndExports(ModuleReference ref, HashSet<String> requires, Set<String> exports) throws IOException {
    try(ModuleReader reader = ref.open()) {
      try(Stream<String> resources = reader.list()) {
        resources.filter(res -> res.endsWith(".class"))
                 .forEach(resource -> {
          try(InputStream input = reader.open(resource).orElseThrow(() -> new IOException("resource unavailable " + resource))) {
            ClassReader classReader = new ClassReader(input);
            String className = classReader.getClassName();
            if (className.equals("module-info")) {
              return;  // skip module-info
            }
            String packageName = packageOf(className);
            exports.add(packageName);
            classReader.accept(new ClassRemapper(null, new Remapper() {
              @Override
              public String map(String typeName) {
                String packageName = packageOf(typeName);
                requires.add(packageName);
                return typeName;
              }
            }) {
              // consider that annotations are not real dependencies
              @Override
              protected AnnotationVisitor createAnnotationRemapper(AnnotationVisitor av) {
                return null;
              }
            }, 0);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }  
        }); 
      }
    }
    requires.removeAll(exports);
  }
  
  static String packageOf(String typeName) {
    int index = typeName.lastIndexOf('/');
    if (index == -1) {
      return "";
    }
    return typeName.substring(0, index);
  }
}
