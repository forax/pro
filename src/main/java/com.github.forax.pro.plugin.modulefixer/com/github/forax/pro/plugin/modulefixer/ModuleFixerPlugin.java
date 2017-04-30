package com.github.forax.pro.plugin.modulefixer;

import static com.github.forax.pro.api.MutableConfig.derive;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Modifier;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.nio.charset.StandardCharsets;
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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.MutableConfig;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.WatcherRegistry;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.helper.FileHelper;
import com.github.forax.pro.helper.Log;
import com.github.forax.pro.helper.ModuleHelper;

import com.github.forax.pro.plugin.modulefixer.ConstInterpreter.ConstValue;

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
    ModuleFixerConf moduleFixer = config.getOrUpdate(name(), ModuleFixerConf.class);
    ConventionFacade convention = config.getOrThrow("convention", ConventionFacade.class);
    
    // inputs
    derive(moduleFixer, ModuleFixerConf::moduleDependencyPath, convention, ConventionFacade::javaModuleDependencyPath);
    derive(moduleFixer, ModuleFixerConf::moduleDependencyFixerPath, convention, ConventionFacade::javaModuleDependencyFixerPath);
    
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
    Path moduleDependencyFixerPath = moduleFixer.moduleDependencyFixerPath();
    
    class Info {
      final Set<String> requirePackages;
      final Set<String> exports;
      final Set<String> uses;
      final Map<String, Set<String>> provides;
      final Set<String> requireModules;
      
      Info(Set<String> requirePackages, Set<String> exports, Set<String> uses, Map<String, Set<String>> provides, Set<String> requireModules) {
        this.requirePackages = requirePackages;
        this.exports = exports;
        this.uses = uses;
        this.provides = provides;
        this.requireModules = requireModules;
      }
    }
    
    // gather additional requires, uses and provides
    boolean force = moduleFixer.force();
    Map<String, Set<String>> additionalRequireMap = parseAdditionals(moduleFixer.additionalRequires());
    Map<String, Set<String>> additionalUses = parseAdditionals(moduleFixer.additionalUses());
    
    //System.out.println("additionalRequireMap " + additionalRequireMap);
    //System.out.println("additionalUses " + additionalUses);
    
    // find dependencies (requires, exports and uses)
    HashMap<ModuleReference, Info> moduleInfoMap = new HashMap<>();
    HashMap<String, List<ModuleReference>> exportMap = new HashMap<>();
    ModuleFinder moduleFinder = ModuleFinder.compose(
        ModuleFinder.of(moduleDependencyPath.toArray(new Path[0])),
        ModuleFinder.ofSystem());
    for(ModuleReference ref: moduleFinder.findAll()) {
      Set<String> exports;
      ModuleDescriptor descriptor = ref.descriptor();
      String name = descriptor.name();
      if (descriptor.isAutomatic() || /*descriptor.isSynthetic() ||*/
          (force && (additionalRequireMap.containsKey(name) || additionalUses.containsKey(name)))) {
        HashSet<String> requires = new HashSet<>();
        exports = new HashSet<>();
        Set<String> uses = Optional.ofNullable(additionalUses.get(name)).orElseGet(HashSet::new);
        Map<String, Set<String>> provides = new HashMap<>();
        findRequiresExportsUsesAndProvides(ref, requires, exports, uses, provides);
        
        Set<String> additionalRequireModules = Optional.ofNullable(additionalRequireMap.get(name)).orElseGet(HashSet::new);
        moduleInfoMap.put(ref,
            new Info(requires, exports, uses, provides, additionalRequireModules));
      } else {
        exports = descriptor.exports().stream().map(export -> export.source().replace('.', '/')).collect(Collectors.toCollection(HashSet::new));
      }
      exports.forEach(packageName -> exportMap.computeIfAbsent(packageName, __ -> new ArrayList<>()).add(ref));
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
      Path generatedModuleInfoPath = generateModuleInfo(ref, info.requireModules, info.exports, info.uses, info.provides, moduleDependencyFixerPath);
      
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
  
  private static Path generateModuleInfo(ModuleReference ref,
                                         Set<String> requires, Set<String> exports, Set<String> uses, Map<String, Set<String>> provides,
                                         Path moduleDependencyFixerPath) throws IOException {
    String moduleName = ref.descriptor().name();
    
    //System.out.println(moduleName);
    //System.out.println("requires: " + requires);
    //System.out.println("exports: " + exports);
    //System.out.println("uses: " + uses);
    //System.out.println("provides: " + provides);
    
    Path modulePatchPath = moduleDependencyFixerPath.resolve(moduleName);
    Files.createDirectories(modulePatchPath);
    
    ModuleDescriptor.Builder builder = ModuleDescriptor.newModule(moduleName, Set.of(Modifier.OPEN));
    ref.descriptor().version().ifPresent(version -> builder.version(version.toString()));
    requires.forEach(builder::requires);
    exports.forEach(export -> builder.exports(export.replace('/', '.')));
    uses.forEach(use -> builder.uses(use.replace('/', '.')));
    provides.forEach((service, providers) -> builder.provides(service, new ArrayList<>(providers)));
    
    Path generatedModuleInfoPath = modulePatchPath.resolve("module-info.class");
    Files.write(generatedModuleInfoPath, ModuleHelper.moduleDescriptorToBinary(builder.build()));
    return generatedModuleInfoPath;
  }

  private static void findRequiresExportsUsesAndProvides(ModuleReference ref, Set<String> requirePackages,
                                                         Set<String> exports, Set<String> uses, Map<String, Set<String>> provides) throws IOException {
    try(ModuleReader reader = ref.open()) {
      try(Stream<String> resources = reader.list()) {
        resources.forEach(resource -> {
          if (resource.endsWith(".class")) {
            scanJavaClass(resource, reader, requirePackages, exports, uses);  
          } else {
            if (resource.startsWith("META-INF/services/")) {
              scanServiceFile(resource, reader, provides);
            }
          }
        }); 
      }
    }
    requirePackages.removeAll(exports);
  }

  // see ServiceLoader javadoc for the full format
  private static void scanServiceFile(String resource, ModuleReader reader, Map<String, Set<String>> provides) {
    try(InputStream input = reader.open(resource).orElseThrow(() -> new IOException("resource unavailable " + resource));
        InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
        BufferedReader lineReader = new BufferedReader(isr)) {

      String service = resource.substring("META-INF/services/".length());
      Set<String> providers = lineReader.lines()
        .map(ModuleFixerPlugin::parseProviderLine)
        .collect(Collectors.toSet());
      provides.put(service, providers);

    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  
  private static String parseProviderLine(String line) {
    String serviceName = line;
    int commentIndex = serviceName.indexOf('#');
    if (commentIndex != -1) {
      serviceName = line.substring(0, commentIndex);
    }
    serviceName = serviceName.trim();
    return serviceName;
  }
  
  private static void scanJavaClass(String resource, ModuleReader reader, Set<String> requirePackages, Set<String> exports, Set<String> uses) {
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
          requirePackages.add(packageName);
          return typeName;
        }
      }) {
        String owner;
        
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
          owner = name;
        }
        
        // consider that annotations are not real dependencies
        @Override
        protected AnnotationVisitor createAnnotationRemapper(AnnotationVisitor av) {
          return null;
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
          super.visitMethod(access, packageName, desc, signature, exceptions);
          return new MethodNode(Opcodes.ASM6, access, packageName, desc, signature, exceptions) {
            final ArrayList<MethodInsnNode> nodes = new ArrayList<>();
            
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
              super.visitMethodInsn(opcode, owner, packageName, desc, itf);
              if (opcode == Opcodes.INVOKESTATIC &&
                  owner.equals("java/util/ServiceLoader") &&
                  name.startsWith("load")) {
                // record ServideLoader.load* instruction
                nodes.add((MethodInsnNode)instructions.getLast());
              }
            }
            
            @Override
            public void visitEnd() {
              super.visitEnd();
              
              if (!nodes.isEmpty()) {
                // do the static analysis to find constant classes
                Analyzer<ConstValue> analyzer = new Analyzer<>(new ConstInterpreter());
                Frame<ConstValue>[] frames;
                try {
                  frames = analyzer.analyze(owner, this);
                } catch (AnalyzerException e) {
                  throw new UncheckedIOException(new IOException("error while analyzing " + owner, e));
                }
                
                for(MethodInsnNode node: nodes) {
                  // try to recognize
                  //   <S> java.util.ServiceLoader<S> load(java.lang.Class<S>, java.lang.ClassLoader);
                  //   <S> java.util.ServiceLoader<S> load(java.lang.Class<S>);
                  //   <S> java.util.ServiceLoader<S> loadInstalled(java.lang.Class<S>);
                  //   <S> java.util.ServiceLoader<S> load(java.lang.reflect.Layer, java.lang.Class<S>);

                  int index = instructions.indexOf(node);
                  Frame<ConstValue> frame = frames[index];
                  ConstValue value;
                  switch(node.desc) {
                  case "(Ljava/lang/Class;)Ljava/util/ServiceLoader;":
                  case "(Ljava/lang/reflect/Layer;Ljava/lang/Class;)Ljava/util/ServiceLoader;": //FIXME when Layer will be java.lang.Layer
                    // extract the top of the stack
                    value = frame.getStack(frame.getStackSize() - 1);
                    break;

                  case "(Ljava/lang/Class;Ljava/lang/ClassLoader;)Ljava/util/ServiceLoader;":
                    // extract the second item from the top of the stack
                    value = frame.getStack(frame.getStackSize() - 2);
                    break;

                  default:
                    throw new IllegalStateException("unknown signature in java.util.ServiceLoader " + node.name + node.desc);
                  }
                  
                  String constString = value.constString;
                  if (constString != "") {
                    uses.add(constString);
                  }  
                }
              }
            }
          };
        }
      }, 0);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  
  
  private static Map<String, Set<String>> parseAdditionals(Optional<List<String>> additionals) {
    return additionals.orElse(List.of())
        .stream()
        .map(line -> line.split("="))
        .collect(Collectors.groupingBy(tokens -> tokens[0], Collectors.mapping(tokens -> tokens[1], Collectors.toSet())));
  }
  
  static String packageOf(String typeName) {
    int index = typeName.lastIndexOf('/');
    if (index == -1) {
      return "";
    }
    return typeName.substring(0, index);
  }
}
