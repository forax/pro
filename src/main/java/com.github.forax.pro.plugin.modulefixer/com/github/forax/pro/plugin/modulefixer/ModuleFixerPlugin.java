package com.github.forax.pro.plugin.modulefixer;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
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
import com.github.forax.pro.helper.FileHelper;
import com.github.forax.pro.helper.ModuleHelper;

public class ModuleFixerPlugin implements Plugin {
  @Override
  public String name() {
    return "modulefixer";
  }

  @Override
  public void init(MutableConfig config) {
    config.getOrUpdate(name(), ModuleFixer.class);
  }
  
  @Override
  public void configure(MutableConfig config) {
    ModuleFixer moduleFxier = config.getOrUpdate(name(), ModuleFixer.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);
    moduleFxier.moduleDependencyPath(convention.javaModuleDependencyPath());
  }
  
  @Override
  public int execute(Config config) throws IOException {
    //System.out.println("execute module fixer " + config);
    
    ModuleFixer moduleFixer = config.getOrThrow(name(), ModuleFixer.class);
    List<Path> moduleDependencyPath = moduleFixer.moduleDependencyPath();
    Path moduleDependencyFixerPath = moduleFixer.moduleDependencyFixerPath().orElseGet(() -> Paths.get("target/deps/module-fixer"));
    
    class Info {
      final Set<String> requirePackages;
      final Set<String> exports;
      Set<String> requireModules;
      
      Info(Set<String> requirePackages, Set<String> exports) {
        this.requirePackages = requirePackages;
        this.exports = exports;
      }
    }
    
    // find dependencies (requires and exports)
    HashMap<ModuleReference, Info> moduleInfoMap = new HashMap<>();
    HashMap<String, List<ModuleReference>> exportMap = new HashMap<>();
    ModuleFinder moduleFinder = ModuleFinder.compose(
        ModuleFinder.of(moduleDependencyPath.toArray(new Path[0])),
        ModuleFinder.ofSystem());
    for(ModuleReference ref: moduleFinder.findAll()) {
      Set<String> exports;
      if (ref.descriptor().isAutomatic()) {
        HashSet<String> requires = new HashSet<>();
        exports = new HashSet<>();
        findRequiresAndExports(ref, requires, exports);
        moduleInfoMap.put(ref, new Info(requires, exports));
      } else {
        exports = ref.descriptor().exports().stream().map(export -> export.source().replace('.', '/')).collect(Collectors.toSet());
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
      List<ModuleReference> refs = entry.getValue();
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
      info.requireModules = info.requirePackages.stream()
          .flatMap(requireName -> {
            List<ModuleReference> refs = exportMap.get(requireName);
            if (refs == null) {
              unknownRequiredPackages.computeIfAbsent(requireName, __ -> new ArrayList<>()).add(ref);
              return Stream.empty();
            }
            return Stream.of(refs.get(0).descriptor().name());
          })
          .collect(Collectors.toSet());
    });
    unknownRequiredPackages.forEach((unknownRequiredPackage, modules) -> {
      String moduleNames = modules.stream().map(ref -> ref.descriptor().name()).collect(joining(", "));
      System.err.println("[modulefixer] package " + unknownRequiredPackage + " required by " + moduleNames + " not found");
    });
    //if (!unknownRequiredPackages.isEmpty()) {
    //  return 1;
    //}
    
    FileHelper.deleteAllFiles(moduleDependencyFixerPath);
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
    
    ModuleDescriptor.Builder builder = ModuleDescriptor.openModule(moduleName);
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
            exports.add(packageOf(classReader.getClassName()));
            classReader.accept(new ClassRemapper(null, new Remapper() {
              @Override
              public String map(String typeName) {
                String packageName = packageOf(typeName);
                requires.add(packageName);
                return packageName;
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
      return typeName;
    }
    return typeName.substring(0, index);
  }
}
