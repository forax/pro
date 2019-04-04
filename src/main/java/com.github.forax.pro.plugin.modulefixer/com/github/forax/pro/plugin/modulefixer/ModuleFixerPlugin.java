package com.github.forax.pro.plugin.modulefixer;

import static com.github.forax.pro.api.MutableConfig.derive;
import static com.github.forax.pro.helper.util.Unchecked.suppress;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Modifier;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
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
import com.github.forax.pro.plugin.modulefixer.impl.ConstInterpreter;
import com.github.forax.pro.plugin.modulefixer.impl.ConstInterpreter.ConstValue;
import com.github.forax.pro.plugin.modulefixer.impl.EmptyClassVisitor;


public class ModuleFixerPlugin implements Plugin {
  @Override
  public String name() {
    return "modulefixer";
  }

  @Override
  public void init(MutableConfig config) {
    var moduleFixerConf = config.getOrUpdate(name(), ModuleFixerConf.class);
    moduleFixerConf.force(false);
  }
  
  @Override
  public void configure(MutableConfig config) {
    var moduleFixerConf = config.getOrUpdate(name(), ModuleFixerConf.class);
    var convention = config.getOrThrow("convention", ConventionFacade.class);
    
    // inputs
    derive(moduleFixerConf, ModuleFixerConf::moduleDependencyPath, convention, ConventionFacade::javaModuleDependencyPath);
    derive(moduleFixerConf, ModuleFixerConf::moduleDependencyFixerPath, convention, ConventionFacade::javaModuleDependencyFixerPath);
    
    // outputs
    //TODO
  }
  
  @Override
  public void watch(Config config, WatcherRegistry registry) {
    var moduleFixerConf = config.getOrThrow(name(), ModuleFixerConf.class);
    var moduleDependencyPath = moduleFixerConf.moduleDependencyPath();
    moduleDependencyPath.forEach(registry::watch);
  }
  
  enum RequireModifier {
    PLAIN, STATIC;
    
    RequireModifier or(RequireModifier require) {
      return this == STATIC || require == STATIC? STATIC: PLAIN;
    }
  }
  
  @Override
  public int execute(Config config) throws IOException {
    var log = Log.create(name(), config.getOrThrow("pro", ProConf.class).loglevel());
    log.debug(config, conf -> "config " + config);
    
    var moduleFixerConf = config.getOrThrow(name(), ModuleFixerConf.class);
    var moduleDependencyPath = moduleFixerConf.moduleDependencyPath();
    var moduleDependencyFixerPath = moduleFixerConf.moduleDependencyFixerPath();
    
    class Info {
      final Set<String> requirePackages;
      final Set<String> exports;
      final Set<String> uses;
      final Map<String, Set<String>> provides;
      final Optional<Version> versionOpt;
      final Map<String, RequireModifier> requireModuleMap;
      
      Info(Set<String> requirePackages, Set<String> exports, Set<String> uses, Map<String, Set<String>> provides, Optional<Version> versionOpt,
           Map<String, RequireModifier> requireModuleMap) {
        this.requirePackages = requirePackages;
        this.exports = exports;
        this.uses = uses;
        this.provides = provides;
        this.versionOpt = versionOpt;
        this.requireModuleMap = requireModuleMap;
      }
    }
    
    // gather additional requires, uses and provides
    var force = moduleFixerConf.force();
    var additionalRequireMap = parseAdditionalRequireMap(moduleFixerConf.additionalRequires());
    var additionalUses = parseAdditionals(moduleFixerConf.additionalUses());
    
    //System.out.println("additionalRequireMap " + additionalRequireMap);
    //System.out.println("additionalUses " + additionalUses);
    
    // find dependencies (requires, exports and uses)
    var moduleInfoMap = new HashMap<ModuleReference, Info>();
    var exportMap = new HashMap<String, List<ModuleReference>>();
    var moduleFinder = ModuleFinder.compose(
        ModuleFinder.of(moduleDependencyPath.toArray(Path[]::new)),
        ModuleHelper.systemModulesFinder());
    for(var moduleRef: moduleFinder.findAll()) {
      Set<String> exports;
      var descriptor = moduleRef.descriptor();
      var name = descriptor.name();
      if (descriptor.isAutomatic() || /*descriptor.isSynthetic() ||*/
          (force && (additionalRequireMap.containsKey(name) || additionalUses.containsKey(name)))) {
        var requires = new HashSet<String>();
        exports = new HashSet<>();
        var uses = Optional.ofNullable(additionalUses.get(name)).orElseGet(HashSet::new);
        var provides = new HashMap<String, Set<String>>();
        var propertyMap = new HashMap<String, Object>();
        findModuleData(moduleRef, requires, exports, uses, provides, propertyMap);
        var versionOpt = Optional.ofNullable((Version)propertyMap.get("Implementation-Version"));
        
        var additionalRequireModuleMap = Optional.ofNullable(additionalRequireMap.get(name)).orElseGet(HashMap::new);
        moduleInfoMap.put(moduleRef,
            new Info(requires, exports, uses, provides, versionOpt, additionalRequireModuleMap));
      } else {
        exports = descriptor.exports().stream().map(export -> export.source().replace('.', '/')).collect(Collectors.toCollection(HashSet::new));
      }
      exports.forEach(packageName -> exportMap.computeIfAbsent(packageName, __ -> new ArrayList<>()).add(moduleRef));
    }
    
    // verify split packages
    var errorCode = 0;
    for(var entry: exportMap.entrySet()) {
      var packageName = entry.getKey();
      var refs = entry.getValue().stream().map(ref -> ref.descriptor().toNameAndVersion()).collect(toUnmodifiableList());
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
    var unknownRequiredPackages = new HashMap<String, List<ModuleReference>>();
    moduleInfoMap.forEach((ref, info) -> {
      // merge calculated requires with explicitly declared requires
      info.requirePackages.stream()
          .flatMap(requireName -> {
            var refs = exportMap.get(requireName);
            if (refs == null) {
              unknownRequiredPackages.computeIfAbsent(requireName, __ -> new ArrayList<>()).add(ref);
              return Stream.empty();
            }
            return Stream.of(refs.get(0).descriptor().name());
          })
          .distinct()
          .forEach(require -> {
            info.requireModuleMap.merge(require, RequireModifier.PLAIN, RequireModifier::or);
          });
    });
    unknownRequiredPackages.forEach((unknownRequiredPackage, modules) -> {
      var moduleNames = modules.stream().map(ref -> ref.descriptor().name()).collect(joining(", "));
      System.err.println("[modulefixer] package " + unknownRequiredPackage + " required by " + moduleNames + " not found");
    });
    //if (!unknownRequiredPackages.isEmpty()) {
    //  return 1;
    //}
    
    FileHelper.deleteAllFiles(moduleDependencyFixerPath, false);
    Files.createDirectories(moduleDependencyFixerPath);
    
    // patch jars with calculated module-info
    errorCode = 0;
    var jarTool = ToolProvider.findFirst("jar").orElseThrow(() -> new IllegalStateException("can not find jar"));
    for(Map.Entry<ModuleReference, Info> entry: moduleInfoMap.entrySet()) {
      var moduleRef = entry.getKey();
      var info = entry.getValue();
      var generatedModuleInfoPath = generateModuleInfo(moduleRef, info.requireModuleMap, info.exports, info.uses, info.provides, info.versionOpt, moduleDependencyFixerPath);
      
      if (errorCode == 0) { // stop to patch at the first error, but generate all module-info.class
        errorCode = patchModularJar(jarTool, moduleRef, generatedModuleInfoPath);
      }
    }
    return errorCode;
  }

  private static int patchModularJar(ToolProvider jarTool, ModuleReference ref, Path generatedModuleInfo) {
    System.out.println("[modulefixer] fix " + ref.descriptor().name());
    return jarTool.run(System.out, System.err,
        "--update",
        "--file", Path.of(ref.location().orElseThrow()).toString(),
        "-C", generatedModuleInfo.getParent().toString(),
        generatedModuleInfo.getFileName().toString());
  }
  
  private static Path generateModuleInfo(ModuleReference ref,
                                         Map<String, RequireModifier> requires,
                                         Set<String> exports, Set<String> uses,
                                         Map<String, Set<String>> provides,
                                         Optional<Version> versionOpt,
                                         Path moduleDependencyFixerPath) throws IOException {
    var moduleName = ref.descriptor().name();
    
    //System.out.println(moduleName);
    //System.out.println("requires: " + requires);
    //System.out.println("exports: " + exports);
    //System.out.println("uses: " + uses);
    //System.out.println("provides: " + provides);
    
    var modulePatchPath = moduleDependencyFixerPath.resolve(moduleName);
    Files.createDirectories(modulePatchPath);
    
    var builder = ModuleDescriptor.newModule(moduleName, Set.of(Modifier.OPEN));
    ref.descriptor().version().ifPresent(version -> builder.version(version.toString()));
    requires.forEach((require, requireModifier) -> builder.requires(requireModifier == RequireModifier.STATIC? EnumSet.of(Requires.Modifier.STATIC): Set.of(), require));
    exports.forEach(export -> builder.exports(export.replace('/', '.')));
    uses.forEach(use -> builder.uses(use.replace('/', '.')));
    provides.forEach((service, providers) -> builder.provides(service, new ArrayList<>(providers)));
    versionOpt.ifPresent(version -> builder.version(version));
    
    var generatedModuleInfoPath = modulePatchPath.resolve("module-info.class");
    Files.write(generatedModuleInfoPath, ModuleHelper.moduleDescriptorToBinary(builder.build()));
    return generatedModuleInfoPath;
  }

  private static void findModuleData(ModuleReference ref, Set<String> requirePackages,
                                     Set<String> exports, Set<String> uses, Map<String, Set<String>> provides,
                                     Map<String, Object> propertyMap) throws IOException {
    try(var reader = ref.open()) {
      try(var resources = reader.list()) {
        resources.forEach(suppress(resource -> {
          if (resource.endsWith(".class")) {
            scanJavaClass(resource, reader, requirePackages, exports, uses);  
          } else {
            if (resource.startsWith("META-INF/services/")) {
              // skip "META-INF/services/" directory resource entry
              if (resource.length() == "META-INF/services/".length()) {
                return;
              }
              scanServiceFile(resource, reader, provides);
            } else {
              if (resource.equals("META-INF/MANIFEST.MF")) {
                scanManifest(resource, reader, propertyMap);
              }
            }
          }
        })); 
      }
    }
    requirePackages.removeAll(exports);
  }

  // see ServiceLoader javadoc for the full format
  private static void scanServiceFile(String resource, ModuleReader reader, Map<String, Set<String>> provides) throws IOException {
    var service = resource.substring("META-INF/services/".length());
    try(var input = reader.open(resource).orElseThrow(() -> new IOException("resource unavailable " + resource));
        var isr = new InputStreamReader(input, StandardCharsets.UTF_8);
        var lineReader = new BufferedReader(isr)) {

      var providers = lineReader.lines()
          .map(ModuleFixerPlugin::parseProviderLine)
          .filter(provider -> !provider.isEmpty())  // skip empty line
          .collect(Collectors.toSet());
      provides.put(service, providers);
    }
  }
  
  private static String parseProviderLine(String line) {
    var serviceName = line;
    int commentIndex = serviceName.indexOf('#');
    if (commentIndex != -1) {
      serviceName = line.substring(0, commentIndex);
    }
    serviceName = serviceName.trim();
    return serviceName;
  }
  
  // see Jar Manifest for the full documentation
  private static void scanManifest(String resource, ModuleReader reader, Map<String, Object> propertyMap) throws IOException {
    try(var input = reader.open(resource).orElseThrow(() -> new IOException("resource unavailable " + resource));
        var isr = new InputStreamReader(input, StandardCharsets.UTF_8);
        var lineReader = new BufferedReader(isr)) {

      lineReader.lines()
      .flatMap(line -> {
        int index = line.indexOf(':');
        if (index == -1) {
          return Stream.empty();
        }
        String key = line.substring(0, index);
        String value = line.substring(index + 1).trim();
        return extractManifestValue(key, value).map(object -> Map.entry(key, object)).stream();
      })
      .forEach(entry -> propertyMap.put(entry.getKey(), entry.getValue()));

    }
  }
  
  private static Optional<Object> extractManifestValue(String key, String value) {
    switch(key) {
    case "Implementation-Version":
      try {
        return Optional.of(Version.parse(value));
      } catch(@SuppressWarnings("unused") IllegalArgumentException e) { // not a valid version
        return Optional.empty();
      }
    default:
      return Optional.empty();
    }
  }
  
  private static void scanJavaClass(String resource, ModuleReader reader, Set<String> requirePackages, Set<String> exports, Set<String> uses) throws IOException {
    try(var input = reader.open(resource).orElseThrow(() -> new IOException("resource unavailable " + resource))) {
      var classReader = new ClassReader(input);
      var className = classReader.getClassName();
      if (className.equals("module-info")) {
        return;  // skip module-info
      }
      var packageName = packageOf(className);
      exports.add(packageName);

      classReader.accept(new ClassRemapper(EmptyClassVisitor.getInstance(), new Remapper() {
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
          super.visit(version, access, name, signature, superName, interfaces);
        }

        // consider that annotations are not real dependencies
        @Override
        protected AnnotationVisitor createAnnotationRemapper(AnnotationVisitor av) {
          return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
          var delegate = super.visitMethod(access, name, desc, signature, exceptions);
          if (delegate == null) {
            return null;
          }
          return new MethodNode(Opcodes.ASM7, access, packageName, desc, signature, exceptions) {
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

              // replay instructions to find dependencies
              accept(delegate);

              if (!nodes.isEmpty()) {
                // do the static analysis to find constant classes
                var analyzer = new Analyzer<>(new ConstInterpreter());
                Frame<ConstValue>[] frames;
                try {
                  frames = analyzer.analyze(owner, this);
                } catch (AnalyzerException e) {
                  throw new IllegalStateException("error while analyzing " + owner, e);
                }

                for(var node: nodes) {
                  // try to recognize
                  //   <S> java.util.ServiceLoader<S> load(java.lang.Class<S>, java.lang.ClassLoader);
                  //   <S> java.util.ServiceLoader<S> load(java.lang.Class<S>);
                  //   <S> java.util.ServiceLoader<S> loadInstalled(java.lang.Class<S>);
                  //   <S> java.util.ServiceLoader<S> load(java.lang.ModuleLayer, java.lang.Class<S>);

                  var index = instructions.indexOf(node);
                  var frame = frames[index];
                  ConstValue value;
                  switch(node.desc) {
                  case "(Ljava/lang/Class;)Ljava/util/ServiceLoader;":
                  case "(Ljava/lang/ModuleLayer;Ljava/lang/Class;)Ljava/util/ServiceLoader;":
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

                  var constString = value.getConstString();
                  if (constString != "") {
                    uses.add(constString);
                  }  
                }
              }
            }
          };
        }
      }, ClassReader.SKIP_FRAMES  /* there is maybe a bug in ASM ? */);
    }
  }
  
  private static Map<String, Set<String>> parseAdditionals(Optional<List<String>> additionals) {
    return additionals.orElse(List.of())
        .stream()
        .map(line -> line.split("="))
        .collect(Collectors.groupingBy(tokens -> tokens[0], Collectors.mapping(tokens -> tokens[1], Collectors.toSet())));
  }
  
  private static Map<String, Map<String, RequireModifier>> parseAdditionalRequireMap(Optional<List<String>> additionals) {
    return additionals.orElse(List.of())
        .stream()
        .map(line -> line.split("="))
        .collect(groupingBy(tokens -> tokens[0],
                   mapping(tokens -> tokens[1].split("/"),
                     toMap(values -> values[0], values -> values.length == 0? RequireModifier.PLAIN: RequireModifier.STATIC))));
  }
  
  static String packageOf(String typeName) {
    var index = typeName.lastIndexOf('/');
    if (index == -1) {
      return "";
    }
    return typeName.substring(0, index);
  }
}
