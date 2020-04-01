package com.github.forax.pro.helper;

import static java.nio.file.Files.exists;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract the layout of the source on the file system.
 * There are two implementations
 * - JDKModuleSourceLayout (prefix + module)
 * - MavenModuleSourceLayout (module + suffix)
 *
 * The method {@link #toModule(ModuleReference, Path)}
 * does the transformation for a layout, a relative path and a module.
 * By example, for the JDK source layout, the path {@code src/main/java} of
 * the module foo is in {@code src/main/java/foo} while for the Maven source layout,
 * the module foo is in {@code foo/src/main/java}.
 *
 * The method {@link #toAll(Path)} return an expression to find all the modules.
 * By example, for the JDK source layout, the path {@code src/main/java}
 * is in {@code src/main/java/'*'} while for the Maven source layout,
 * the result is {@code '*'/src/main/java}.
 *
 * The method {@link #findModuleRefs(List)} find all the source modules
 * in a path by finding the {@code module-info.java}.
 */
public interface ModuleSourceLayout {
  /**
   * Factory of module source layout.
   */
  @FunctionalInterface
  interface Factory {
    /**
     * Create a layout if
     * @param root the root folder of the layout.
     * @return a module source layout.
     */
    Optional<ModuleSourceLayout> createLayout(Path root);

    /**
     * Return a factory that will try to create a layout or try to create another one.
     *
     * @param factory a factory of layout.
     * @return a factory that will try to create a layout or try to create another one.
     */
    default Factory or(Factory factory) {
      Objects.requireNonNull(factory);
      return root -> createLayout(root).or(() -> factory.createLayout(root));
    }

    /**
     * Return the factory pass as argument.
     * @param factory a factory of layout.
     * @return the factory pass as argument.
     */
    static Factory of(Factory factory) {
      return Objects.requireNonNull(factory);
    }
  }

  /**
   * Find all source modules from a path depending on the current layout.
   *
   * @param moduleSourcePath the path where the source module may be
   * @return a set of module references
   */
  Set<ModuleReference> findModuleRefs(List<Path> moduleSourcePath);

  /**
   * Return a Path from the current layout, a module and the local path.
   *
   * @param moduleRef   a module
   * @param moduleLocal a path relative to the module
   * @return a Path (or not) that allow to find the data.
   */
  Optional<Path> toModule(ModuleReference moduleRef, Path moduleLocal);

  /**
   * Return a Path allowing to find all modules from a local path. This Path may contains some '*'
   * to indicate that where the path should be expanded.
   *
   * @param moduleLocal a path relative to the module
   * @return a Path (or not) that indicates where all modules are
   * @see FileHelper#expand(Path)
   */
  Optional<Path> toAll(Path moduleLocal);

  /**
   * A generalization of {@link #toModule(ModuleReference, Path)} with several local paths.
   *
   * @param moduleRef       a module
   * @param moduleLocalPath a list of local path
   * @return a list of paths resolved by {@link #toModule(ModuleReference, Path)}.
   * @see #toModule(ModuleReference, Path)
   */
  default List<Path> toModulePath(ModuleReference moduleRef, List<Path> moduleLocalPath) {
    Objects.requireNonNull(moduleRef);
    Objects.requireNonNull(moduleLocalPath);
    return moduleLocalPath.stream()
        .flatMap(path -> toModule(moduleRef, path).stream())
        .collect(toUnmodifiableList());
  }

  /**
   * A generalization of {@link #toAll(Path)} with several local paths.
   *
   * @param moduleLocalPath a list of local path
   * @return a list of paths resolved by {@link #toAll(Path)}.
   * @see #toModule(ModuleReference, Path)
   */
  default List<Path> toAllPath(List<Path> moduleLocalPath) {
    Objects.requireNonNull(moduleLocalPath);
    return moduleLocalPath.stream()
        .flatMap(path -> toAll(path).stream())
        .collect(toUnmodifiableList());
  }

  private static Set<ModuleReference> findModules(Path rootModulesPath, List<Path> preModules,
      List<Path> postModules) {
    Objects.requireNonNull(rootModulesPath);
    Objects.requireNonNull(preModules);
    Objects.requireNonNull(postModules);
    return preModules.stream()
        .map(preModule -> rootModulesPath.resolve(preModule).normalize())
        .filter(Files::isDirectory)
        .flatMap(sourceFolder -> {
          return FileHelper.list(sourceFolder)
              .filter(Files::isDirectory)
              .flatMap(moduleFolder -> {
                return postModules.stream()
                    .flatMap(postModule -> ModuleHelper
                        .findSourceModuleReference(moduleFolder, postModule).stream());
              });
        })
        .collect(toCollection(LinkedHashSet::new));
  }

  static final ModuleSourceLayout JDK_LAYOUT = new ModuleSourceLayout() {
    @Override
    public Set<ModuleReference> findModuleRefs(List<Path> moduleSourcePath) {
      Objects.requireNonNull(moduleSourcePath);
      return findModules(Path.of("."), moduleSourcePath, List.of(Path.of(".")));
    }

    @Override
    public Optional<Path> toAll(Path moduleLocal) {
      Objects.requireNonNull(moduleLocal);
      return Optional.of(moduleLocal)
          .filter(Files::exists)
          .map(Path::normalize);
    }

    @Override
    public Optional<Path> toModule(ModuleReference moduleRef, Path moduleLocal) {
      Objects.requireNonNull(moduleRef);
      Objects.requireNonNull(moduleLocal);
      return Optional.of(moduleLocal)
          .filter(Files::exists)
          .map(path -> path.resolve(moduleRef.descriptor().name()))
          .filter(Files::exists)
          .map(Path::normalize);
    }

    @Override
    public String toString() {
      return "jdk layout";
    }
  };

  /**
   * Returns the JDK layout at that folder if it exists
   *
   * @param root the root folder containing the layout
   * @return a JDK layout (or not).
   */
  static Optional<ModuleSourceLayout> lookupForJdkLayout(Path root) {
    Objects.requireNonNull(root);
    if (!exists(root) || !exists(root.resolve("src"))) {  // FIXME
      return Optional.empty();
    }
    return Optional.of(JDK_LAYOUT);
  }

  /**
   * Returns the Maven multi layout at that folder if it exists
   *
   * @param root the root folder containing the layout
   * @return a Maven multi layout (or not).
   */
  static Optional<ModuleSourceLayout> lookupForMavenMultiLayout(Path root) {
    Objects.requireNonNull(root);
    if (!exists(root)) {
      return Optional.empty();
    }
    return Optional.of(new ModuleSourceLayout() {
      @Override
      public Set<ModuleReference> findModuleRefs(List<Path> moduleSourcePath) {
        return findModules(root, List.of(Path.of(".")), moduleSourcePath);
      }

      @Override
      public Optional<Path> toAll(Path moduleLocal) {
        Objects.requireNonNull(moduleLocal);
        return Optional.of(root)
            .map(p -> p.resolve("*/").resolve(moduleLocal));
      }

      @Override
      public Optional<Path> toModule(ModuleReference moduleRef, Path moduleLocal) {
        Objects.requireNonNull(moduleRef);
        Objects.requireNonNull(moduleLocal);
        return Optional.of(root)
            .map(p -> p.resolve(moduleRef.descriptor().name()))
            .filter(Files::exists)
            .map(p -> p.resolve(moduleLocal))
            .filter(Files::exists)
            .map(Path::normalize);
      }

      @Override
      public String toString() {
        return "maven multi layout";
      }
    });
  }
}