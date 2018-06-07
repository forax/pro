package com.github.forax.pro.helper;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileHelper {
  private FileHelper() {
    throw new AssertionError();
  }

  public static List<Path> pathFromFilesThatExist(List<Path> paths) {
    return List.of(paths.stream().filter(Files::exists).toArray(Path[]::new));
  }

  /**
   * Delete recursively all files inside a directory.
   * @param directory a directory
   * @param removeDirectory also delete the top level directory.
   * @throws IOException if a file can not be deleted
   */
  public static void deleteAllFiles(Path directory, boolean removeDirectory) throws IOException {
    Files.walkFileTree(directory, new FileVisitor<Path>() {
      @Override
      public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
        if (removeDirectory || !path.equals(directory)) {
          Files.delete(path);  
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        Files.delete(path);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
        return FileVisitResult.CONTINUE;
      }
    });
  }
  
  public interface IORunnable {
    public void run() throws IOException;
  }
  public interface IOConsumer<T> {
    public void accept(T element) throws IOException;
  }
  public interface IOBiConsumer<T, U> {
    public void accept(T t, U u) throws IOException;
  }
  public interface IOFunction<T, R> {
    public R apply(T element) throws IOException;
  }
  public interface IOBiFunction<T, U, R> {
    public R apply(T element, U element2) throws IOException;
  }

  public static Runnable unchecked(IORunnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch(IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }
  public static <T> Consumer<T> unchecked(IOConsumer<? super T> consumer) {
    return element -> {
      try {
        consumer.accept(element);
      } catch(IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }
  public static <T, U> BiConsumer<T, U> unchecked(IOBiConsumer<? super T, ? super U> consumer) {
    return (element, element2) -> {
      try {
        consumer.accept(element, element2);
      } catch(IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }
  public static <T, R> Function<T, R> unchecked(IOFunction<? super T, ? extends R> function) {
    return element -> {
      try {
        return function.apply(element);
      } catch(IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }
  public static <T, U, R> BiFunction<T, U, R> unchecked(IOBiFunction<? super T, ? super U, ? extends R> function) {
    return (element, element2) -> {
      try {
        return function.apply(element, element2);
      } catch(IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }
  
  public static Predicate<Path> pathFilenameEndsWith(String text) {
    return path -> path.getFileName().toString().endsWith(text);
  }
  
  public static Predicate<Path> pathFilenameEquals(String text) {
    return path -> path.getFileName().toString().equals(text);
  }
  
  public static List<Path> walkIfNecessary(List<Path> list, Predicate<? super Path> filter) {
    return list.stream()
               .flatMap(FileHelper.<Path, Stream<Path>>unchecked(path -> {
                   if (!Files.isDirectory(path)) {
                     return Stream.of(path);
                   }
                   return Files.walk(path).filter(filter);
               }))
               .collect(Collectors.toList());
  }
  
  public static void walkAndFindCounterpart(Path srcPath, Path dstPath, Function<Stream<Path>, Stream<Path>> configure, IOBiConsumer<Path, Path> consumer) {
    try(Stream<Path> stream = Files.walk(srcPath)) {
      configure.apply(stream)
        .forEach(unchecked(path -> {
          Path targetPath = dstPath.resolve(srcPath.relativize(path));
          consumer.accept(path, targetPath);
      }));
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  
  public static void download(URI uri, Path targetDirectory) {
    var fileName = Paths.get(uri.getPath()).getFileName();
    var targetFile = targetDirectory.resolve(fileName);
    if (Files.exists(targetFile)) {
      return;
    }
    try { 
      Files.createDirectories(targetDirectory);
      try(var input = uri.toURL().openStream();
           var output = Files.newOutputStream(targetFile, CREATE_NEW, WRITE)) {
        input.transferTo(output);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("download failed: url=" + uri + " targetDirectory=" + targetDirectory, e);
    }
  }
}
