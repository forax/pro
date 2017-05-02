package com.github.forax.pro.helper;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.ACC_OPEN;
import static org.objectweb.asm.Opcodes.V1_9;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Builder;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Modifier;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.ModuleRequireNode;

import com.github.forax.pro.helper.parser.JavacModuleParser;
import com.github.forax.pro.helper.parser.ModuleClassVisitor;

public class ModuleHelper {
  private ModuleHelper() {
    throw new AssertionError(); 
  }

  private static boolean containsAtLeastAService(ModuleReference ref, List<String> serviceNames) {
     Set<String> provides = ref.descriptor().provides().stream().map(Provides::service).collect(Collectors.toSet());
     return serviceNames.stream().anyMatch(provides::contains);
  } 
  
  public static Stream<ModuleReference> findAllModulesWhichProvideAService(List<String> serviceNames, ModuleFinder finder) {
    return finder.findAll().stream()
        .filter(ref -> containsAtLeastAService(ref, serviceNames));
  }
  
  private static Set<Requires.Modifier> requireModifiers(int modifiers) {
    return Map.of(
        ACC_MANDATED, Requires.Modifier.MANDATED,
        ACC_SYNTHETIC, Requires.Modifier.SYNTHETIC,
        ACC_TRANSITIVE, Requires.Modifier.TRANSITIVE,
        ACC_STATIC_PHASE, Requires.Modifier.STATIC)
      .entrySet()
      .stream()
      .map(entry -> (modifiers & entry.getKey()) != 0? entry.getValue(): null)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }
  
  private static void parseModule(Path moduleInfoPath, ModuleClassVisitor visitor) {
    try {
      JavacModuleParser.parse(moduleInfoPath, visitor);
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  
  private static Optional<ModuleNode> sourceModuleInfo(Path moduleInfoPath) {
    class Visitor implements  ModuleClassVisitor {
      ModuleNode moduleNode;
      
      @Override
      public ModuleVisitor visitModule(String name, int flags, String version) {
        return moduleNode = new ModuleNode(name, flags, version);
      }
    }
    
    Visitor visitor = new Visitor();
    parseModule(moduleInfoPath, visitor);
    ModuleNode moduleNode = visitor.moduleNode;
    if (moduleNode == null) {
      return Optional.empty();
    }
    moduleNode.requires = fixNull(moduleNode.requires);
    if (moduleNode.requires.stream().noneMatch(require -> require.module.equals("java.base"))) {
      moduleNode.requires.add(new ModuleRequireNode("java.base", ACC_MANDATED, null));
    }
    moduleNode.exports = fixNull(moduleNode.exports);
    moduleNode.opens = fixNull(moduleNode.opens);
    moduleNode.uses = fixNull(moduleNode.uses);
    moduleNode.provides = fixNull(moduleNode.provides);
    return Optional.of(moduleNode);
  }
  
  private static <T> List<T> fixNull(List<T> list) {
    return list == null? new ArrayList<>(): list;
  }
  
  private static Set<String> findJavaPackages(Path moduleDirectory) {
    try(Stream<Path> stream = Files.walk(moduleDirectory)) {
      return stream
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .map(path -> moduleDirectory.relativize(path))
          .filter(path -> path.getParent() != null)
          .map(path -> path.getParent().toString().replace('/', '.').replace('\\', '.'))
          .collect(Collectors.toSet());
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  
  public static Optional<ModuleDescriptor> sourceModuleDescriptor(Path moduleInfoPath) {
    return sourceModuleInfo(moduleInfoPath).map(moduleNode -> createModuleDescriptor(moduleNode, moduleInfoPath));
  }
  
  private static ModuleDescriptor createModuleDescriptor(ModuleNode moduleNode, Path moduleInfoPath) {
    Set<Modifier> modifiers = (moduleNode.access & ACC_OPEN) != 0? Set.of(Modifier.OPEN): Set.of();
    ModuleDescriptor.Builder builder = ModuleDescriptor.newModule(moduleNode.name, modifiers);
    
    moduleNode.requires.forEach(require -> builder.requires(requireModifiers(require.access), require.module));
    moduleNode.exports.forEach(export -> {
      if (export.modules.isEmpty()) {
        builder.exports(export.packaze);
      } else {
        builder.exports(export.packaze, export.modules.stream().collect(toSet()));
      }
    });
    moduleNode.opens.forEach(open -> {
      if (open.modules.isEmpty()) {
        builder.opens(open.packaze);
      } else {
        builder.opens(open.packaze, open.modules.stream().collect(toSet()));
      }
    });
    moduleNode.uses.forEach(builder::uses);
    moduleNode.provides.forEach(provide -> builder.provides(provide.service, provide.providers));

    Path moduleDirectory = moduleInfoPath.getParent();
    Set<String> javaPackages = findJavaPackages(moduleDirectory);
    javaPackages.removeAll(moduleNode.exports.stream().map(export -> export.packaze).collect(Collectors.toList()));
    javaPackages.removeAll(moduleNode.opens.stream().map(export -> export.packaze).collect(Collectors.toList()));
    builder.packages(javaPackages);

    ModuleDescriptor descriptor = builder.build();
    //System.out.println(descriptor.name() + " " + descriptor.packages());

    return descriptor;
  }
  
  public static ModuleFinder sourceModuleFinder(Path directory) {
    return new ModuleFinder() {
      @Override
      public Set<ModuleReference> findAll() {
        try(Stream<Path> stream = Files.list(directory)) {
          return stream.flatMap(path -> find(path.getFileName().toString()).stream())
              .collect(Collectors.toSet());
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
      
      @Override
      public Optional<ModuleReference> find(String name) {
        return Optional.of(directory.resolve(name))
          .filter(Files::isDirectory)
          .flatMap(path -> Optional.of(path.resolve("module-info.java"))
                                   .filter(Files::exists)
                                   .flatMap(ModuleHelper::sourceModuleDescriptor)
                                   .map(descriptor -> moduleReference(descriptor, path.toUri(), null)));
      }
    };
  }
  
  static ModuleReference moduleReference(ModuleDescriptor descriptor, URI uri, ModuleReader moduleReader) {
    return new ModuleReference(descriptor, uri) {
      @Override
      public ModuleReader open() throws IOException {
        return moduleReader;
      }
    };
  }
  
  public static ModuleFinder sourceModuleFinders(List<Path> directories) {
    return ModuleFinder.compose(
        directories.stream()
                   .map(ModuleHelper::sourceModuleFinder)
                   .toArray(ModuleFinder[]::new));
  }
  
  public static ModuleFinder filter(ModuleFinder finder, Predicate<? super ModuleReference> predicate) {
    return new ModuleFinder() {
      private Set<ModuleReference> filtered;
      
      @Override
      public Set<ModuleReference> findAll() {
        if (filtered != null) {
          return filtered;
        }
        return filtered = Collections.unmodifiableSet(
            finder.findAll().stream().filter(predicate).collect(Collectors.toSet()));
      }
      
      @Override
      public Optional<ModuleReference> find(String name) {
        return finder.find(name).filter(predicate);
      }
    };
  }
  
  public interface ResolverFailureListener {
    public void dependencyNotFound(String moduleName, String dependencyChain);
  }
  
  public static boolean resolveOnlyRequires(ModuleFinder finder, List<String> rootNames, ResolverFailureListener listener) {
    class Work {
      final Supplier<String> chain;
      final String moduleName;
      
      Work(Supplier<String> chain, String moduleName) {
        this.chain = chain;
        this.moduleName = moduleName;
      }
    }
    
    HashSet<String> moduleFounds = new HashSet<>(rootNames);
    ArrayDeque<Work> works = new ArrayDeque<>();
    rootNames.forEach(root -> works.offer(new Work(() -> root, root)));
    
    boolean resolved = true;
    for(;;) {
      Work work = works.poll();
      if (work == null) {
        return resolved;
      }
      String name = work.moduleName;
      Supplier<String> chain = work.chain;
      Optional<ModuleReference> optRef = finder.find(name);
      if (optRef.isPresent()) {
        optRef.get().descriptor().requires()
          .stream()
          .map(Requires::name)
          .filter(moduleFounds::add)  // side effect !, it filters out require already presents in moduleFound
          .forEach(require -> works.offer(new Work(() -> chain.get() + " -> " + require, require)));
      } else {
        resolved = false;
        listener.dependencyNotFound(name, chain.get());
      }
    }
  }

  
  public static ModuleDescriptor mergeModuleDescriptor(ModuleDescriptor sourceModule, ModuleDescriptor testModule) {
    boolean open = sourceModule.isOpen() || testModule.isOpen();
    
    System.out.println("merge " + sourceModule + " " + testModule + " " + sourceModule.isOpen() + " " + testModule.isOpen());
    
    Set<Modifier> moduleModifiers = open? Set.of(Modifier.OPEN): Set.of();
    Builder builder = ModuleDescriptor.newModule(testModule.name(), moduleModifiers);
    
    HashMap<String, Set<Requires.Modifier>> requires = merge(ModuleDescriptor::requires,
        Requires::name, Requires::modifiers, ModuleHelper::mergeRequiresModifiers, sourceModule, testModule);
    HashMap<String, Set<String>> exports = merge(ModuleDescriptor::exports,
        Exports::source, Exports::targets, ModuleHelper::mergeRestrictions, sourceModule, testModule);
    HashMap<String, Boolean> packages = merge(ModuleDescriptor::packages,
        x -> x, x -> true, (_1, _2) -> true, sourceModule, testModule);
    HashMap<String, Set<String>> opens = merge(ModuleDescriptor::opens,
        Opens::source, Opens::targets, ModuleHelper::mergeRestrictions, sourceModule, testModule);
    HashMap<String, Boolean> uses = merge(ModuleDescriptor::uses,
        x -> x, x -> true, (_1, _2) -> true, sourceModule, testModule);
    HashMap<String, Set<String>> provides = merge(ModuleDescriptor::provides,
        Provides::service, p -> new HashSet<>(p.providers()), ModuleHelper::mergeAll, sourceModule, testModule);
    
    requires.forEach((name, modifiers) -> builder.requires(modifiers, name));
    exports.forEach((source, target) -> {
      if (target.isEmpty()) {
        builder.exports(Set.of(), source);
      } else {
        builder.exports(source, target);
      }
    });
    packages.keySet().removeAll(exports.keySet());
    builder.packages(packages.keySet());
    opens.forEach((source, target) -> {
      if (target.isEmpty()) {
        builder.opens(Set.of(), source);
      } else {
        builder.opens(source, target);
      }
    });
    uses.keySet().forEach(builder::uses);
    provides.forEach((service, providers) -> builder.provides(service, providers.stream().collect(toList())));
    
    return builder.build();
  }
  
  private static <T, V> HashMap<String, V> merge(Function<ModuleDescriptor, Set<? extends T>> propertyExtractor, Function<? super T, String> keyMapper, Function<? super T, ? extends V> valueMapper, BiFunction<? super V, ? super V, ? extends V> valueMerger, ModuleDescriptor sourceDescriptor, ModuleDescriptor testDescriptor) {
    LinkedHashMap<String, V> map = new LinkedHashMap<>();
    Consumer<T> consumer = element -> map.merge(keyMapper.apply(element), valueMapper.apply(element), valueMerger);
    propertyExtractor.apply(sourceDescriptor).forEach(consumer);
    propertyExtractor.apply(testDescriptor).forEach(consumer);
    return map;
  }
  
  private static Set<String> mergeAll(Set<String> s1, Set<String> s2) {
    LinkedHashSet<String> set = new LinkedHashSet<>();
    set.addAll(s1);
    set.addAll(s2);
    return set;
  }
  
  private static Set<String> mergeRestrictions(Set<String> s1, Set<String> s2) {
    if (s1.isEmpty()) {
      return s1;
    }
    if (s2.isEmpty()) {
      return s2;
    }
    return mergeAll(s1, s2);
  }
  
  private static Set<Requires.Modifier> mergeRequiresModifiers(Set<Requires.Modifier> s1, Set<Requires.Modifier> s2) {
    boolean transitive = s1.contains(Requires.Modifier.TRANSITIVE) || s2.contains(Requires.Modifier.TRANSITIVE);
    boolean staticz = s1.contains(Requires.Modifier.STATIC) && s2.contains(Requires.Modifier.STATIC);
    return Stream.of(
          Optional.of(Requires.Modifier.TRANSITIVE).filter(__ -> transitive),
          Optional.of(Requires.Modifier.STATIC).filter(__ -> staticz)
        ).flatMap(Optional::stream).collect(Collectors.toSet());
  }
  
  
  public static String moduleDescriptorToSource(ModuleDescriptor descriptor) {
    class Generator {
      private final ArrayList<Stream<String>> streams = new ArrayList<>();
      
      Generator $(String text) {
        streams.add(Stream.of(text)); return this;
      }
      Generator $(String text, Function<ModuleDescriptor, ? extends String> mapper) {
        $(String.format(text, mapper.apply(descriptor))); return this;
      }
      <T> Generator $(String text, Function<ModuleDescriptor, ? extends Collection<? extends T>> elementMapper, Function<T, String> mapper) {
        streams.add(elementMapper.apply(descriptor).stream().map(e -> String.format(text, mapper.apply(e)))); return this;
      }
      <T> Generator $(String text, Function<ModuleDescriptor, ? extends Collection<? extends T>> elementMapper, Function<? super T, String> mapper, String text2, Function<? super T, Collection<? extends String>> mapper2) {
        streams.add(elementMapper.apply(descriptor).stream().map(e -> {
            Collection<? extends String> values = mapper2.apply(e);
            String format = values.isEmpty()? text: String.format(text, "%s " + text2);
            return String.format(format, mapper.apply(e), values.stream().collect(Collectors.joining(",")));
          }));
        return this;
      }
      String join() {
        return streams.stream().flatMap(x -> x).collect(Collectors.joining("\n"));
      }
    }
    
    return new Generator()
          .$("%s",             desc -> desc.isOpen()? "open":"")
          .$("module %s {",    ModuleDescriptor::name)
          .$("  requires %s;", ModuleDescriptor::requires, Requires::name)
          .$("  exports %s;",  ModuleDescriptor::exports,  Exports::source,   "to %s", Exports::targets)
          .$("  opens %s;",    ModuleDescriptor::opens,    Opens::source,     "to %s", Opens::targets)
          .$("")
          .$("  uses %s;",     ModuleDescriptor::uses,     Function.identity())
          .$("  provides %s;", ModuleDescriptor::provides, Provides::service, "with %s", Provides::providers)
          .$("}\n")
          .join();
  }

  public static byte[] moduleDescriptorToBinary(ModuleDescriptor descriptor) {
    ClassWriter classWriter = new ClassWriter(0);
    classWriter.visit(V1_9, ACC_MODULE, "module-info", null, null, null);
    int moduleFlags = (descriptor.isOpen()? ACC_OPEN: 0) | ACC_SYNTHETIC;   // mark all generated module-info.class as synthetic    
    String moduleVersion = descriptor.version().map(Version::toString).orElse(null);
    org.objectweb.asm.ModuleVisitor mv = classWriter.visitModule(descriptor.name(), moduleFlags, moduleVersion);
    descriptor.packages().forEach(packaze -> mv.visitPackage(packaze.replace('.', '/')));
    
    descriptor.mainClass().ifPresent(mainClass -> mv.visitMainClass(mainClass.replace('.', '/')));
    
    descriptor.requires().forEach(require -> {
      int modifiers = require.modifiers().stream().mapToInt(ModuleHelper::modifierToInt).reduce(0, (a, b) -> a | b);
      mv.visitRequire(require.name(), modifiers, null);
    });
    descriptor.exports().forEach(export -> {
      int modifiers = export.modifiers().stream().mapToInt(ModuleHelper::modifierToInt).reduce(0, (a, b) -> a | b);
      mv.visitExport(export.source().replace('.', '/'), modifiers, export.targets().toArray(new String[0]));
    });
    descriptor.opens().forEach(open -> {
      int modifiers = open.modifiers().stream().mapToInt(ModuleHelper::modifierToInt).reduce(0, (a, b) -> a | b);
      mv.visitExport(open.source().replace('.', '/'), modifiers, open.targets().toArray(new String[0]));
    });
    descriptor.uses().forEach(service -> mv.visitUse(service.replace('.', '/')));
    descriptor.provides().forEach(provide -> {
      mv.visitProvide(provide.service().replace('.', '/'), provide.providers().stream().map(name -> name.replace('.', '/')).toArray(String[]::new));
    });
    mv.visitEnd();
    classWriter.visitEnd();
    return classWriter.toByteArray();
  }
  
  private static int modifierToInt(Requires.Modifier modifier) {
    switch(modifier) {
    case MANDATED:
      return ACC_MANDATED;
    case SYNTHETIC:
      return ACC_SYNTHETIC;
    case STATIC:
      return ACC_STATIC_PHASE;
    case TRANSITIVE:
      return ACC_TRANSITIVE;
    default:
      throw new IllegalStateException("unknown modifier " + modifier);
    }
  }
  
  private static int modifierToInt(Exports.Modifier modifier) {
    switch(modifier) {
    case MANDATED:
      return ACC_MANDATED;
    case SYNTHETIC:
      return ACC_SYNTHETIC;
    default:
      throw new IllegalStateException("unknown modifier " + modifier);
    }
  }
  
  private static int modifierToInt(Opens.Modifier modifier) {
    switch(modifier) {
    case MANDATED:
      return ACC_MANDATED;
    case SYNTHETIC:
      return ACC_SYNTHETIC;
    default:
      throw new IllegalStateException("unknown modifier " + modifier);
    }
  }
}
