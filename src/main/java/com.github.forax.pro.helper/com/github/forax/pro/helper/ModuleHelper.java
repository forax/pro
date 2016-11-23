package com.github.forax.pro.helper;

import static java.util.stream.Collectors.toList;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Builder;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.forax.pro.helper.parser.JavacModuleParser;

public class ModuleHelper {
  private ModuleHelper() {
    throw new AssertionError(); 
  }

  private static boolean containsAtLeastAService(ModuleReference ref, List<String> serviceNames) {
     Set<String> provides = ref.descriptor().provides().stream().map(Provides::service).collect(Collectors.toSet());
     return serviceNames.stream().anyMatch(provides::contains);
  } 
  
  public static Set<String> findAllModulesWhichProvideAService(List<String> serviceNames, ModuleFinder finder) {
    return finder.findAll().stream()
        .filter(ref -> containsAtLeastAService(ref, serviceNames))
        .map(ref -> ref.descriptor().name())
        .collect(Collectors.toSet());
  }
  
  private static Set<Requires.Modifier> requireModifiers(int modifiers) {
    if ((modifiers & ModuleVisitor.ACC_TRANSITIVE) != 0) {
      return Set.of(Requires.Modifier.TRANSITIVE);
    }
    return Set.of();
  }
  
  public static class ModuleInfo {
    int modifiers;
    String name;
    final LinkedHashMap<String, Integer> requires = new LinkedHashMap<>();
    final LinkedHashMap<String, Set<String>> exports = new LinkedHashMap<>();
    final LinkedHashMap<String, Set<String>> opens = new LinkedHashMap<>();
    final LinkedHashSet<String> uses = new LinkedHashSet<>();
    final LinkedHashMap<String, List<String>> provides = new LinkedHashMap<>();
    
    public int getModifiers() {
      return modifiers;
    }
    public String getName() {
      return name;
    }
    public Map<String, Integer> getRequires() {
      return requires;
    }
    public Map<String, Set<String>> getExports() {
      return exports;
    }
    public Map<String, Set<String>> getOpens() {
      return opens;
    }
    public Map<String, List<String>> getProvides() {
      return provides;
    }
  }
  
  public static void parseModule(Path moduleInfoPath, ModuleVisitor visitor) {
    try {
      //ModuleParser.parse(moduleInfoPath, visitor);
      JavacModuleParser.parse(moduleInfoPath, visitor);
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  
  public static ModuleInfo sourceModuleInfo(Path moduleInfoPath) {
    ModuleInfo moduleInfo = new ModuleInfo();
    moduleInfo.requires.put("java.base", 0);
    ModuleVisitor visitor = new ModuleVisitor() {
      @Override
      public void visitModule(int modifiers, String name) {
        moduleInfo.modifiers = modifiers;
        moduleInfo.name = name;
      }
      
      @Override
      public void visitRequires(int modifiers, String module) {
        moduleInfo.requires.put(module, modifiers);
      }
      @Override
      public void visitExports(String packaze, List<String> toModules) {
        moduleInfo.exports.put(packaze, new LinkedHashSet<>(toModules));
      }
      
      @Override
      public void visitOpens(String packaze, List<String> toModules) {
        moduleInfo.opens.put(packaze, new LinkedHashSet<>(toModules));
      }
      
      @Override
      public void visitUses(String service) {
        moduleInfo.uses.add(service);
      }
      
      @Override
      public void visitProvides(String service, List<String> providers) {
        moduleInfo.provides.put(service, providers);
      }
    };
    parseModule(moduleInfoPath, visitor);
    return moduleInfo;
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
  
  public static ModuleDescriptor sourceModuleDescriptor(Path moduleInfoPath) {
    ModuleInfo moduleInfo = sourceModuleInfo(moduleInfoPath);
    //ModuleDescriptor.Builder builder = Secret.moduleDescriptor_Builder_init(moduleInfo.getName(), true);
    ModuleDescriptor.Builder builder = ModuleDescriptor.module(moduleInfo.getName());
    moduleInfo.requires.forEach((module, modifiers) -> builder.requires(requireModifiers(modifiers), module));
    moduleInfo.exports.forEach((packaze, modules) -> {
      if (modules.isEmpty()) {
        builder.exports(packaze);
      } else {
        builder.exports(packaze, modules);
      }
    });
    moduleInfo.opens.forEach((packaze, modules) -> {
      if (modules.isEmpty()) {
        builder.opens(packaze);
      } else {
        builder.opens(packaze, modules);
      }
    });
    moduleInfo.uses.forEach(builder::uses);
    moduleInfo.provides.forEach(builder::provides);

    Path moduleDirectory = moduleInfoPath.getParent();
    Set<String> javaPackages = findJavaPackages(moduleDirectory);
    javaPackages.removeAll(moduleInfo.exports.keySet());
    javaPackages.removeAll(moduleInfo.opens.keySet());
    builder.contains(javaPackages);

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
                                   .map(ModuleHelper::sourceModuleDescriptor)
                                   .map(descriptor -> new ModuleReference(descriptor, path.toUri(), () -> null)));
      }
    };
  }
  
  public static ModuleFinder sourceModuleFinders(List<Path> directories) {
    return ModuleFinder.compose(
        directories.stream()
                   .map(ModuleHelper::sourceModuleFinder)
                   .toArray(ModuleFinder[]::new));
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
        listener.dependencyNotFound(work.moduleName, chain.get());
      }
    }
  }

  
  public static ModuleDescriptor mergeModuleDescriptor(ModuleDescriptor sourceModule, ModuleDescriptor testModule) {
    Builder builder = ModuleDescriptor.module(testModule.name());  //FIXME open module ?
    
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
    packages.keySet().forEach(builder::contains);
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
  
  
  public static void write(Path path, ModuleDescriptor descriptor) {
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
    
    try(BufferedWriter writer = Files.newBufferedWriter(path)) {
      writer.write(new Generator()
          .$("module %s {",    ModuleDescriptor::name)
          .$("  requires %s;", ModuleDescriptor::requires, Requires::name)
          .$("  exports %s;",  ModuleDescriptor::exports,  Exports::source,   "to %s", Exports::targets)
          .$("  opens %s;",    ModuleDescriptor::opens,    Opens::source,     "to %s", Opens::targets)
          .$("")
          .$("  uses %s;",     ModuleDescriptor::uses,     Function.identity())
          .$("  provides %s;", ModuleDescriptor::provides, Provides::service, "with %s", Provides::providers)
          .$("}\n")
          .join());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
