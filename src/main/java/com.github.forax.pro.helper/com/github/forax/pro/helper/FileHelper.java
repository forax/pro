package com.github.forax.pro.helper;

import static com.github.forax.pro.helper.FileHelper.unchecked;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileHelper {
  private FileHelper() {
    throw new AssertionError();
  }

  public static List<Path> pathFromFilesThatExist(Path... paths) {
    return List.of(Arrays.stream(paths).filter(Files::exists).toArray(Path[]::new));
  }
  
  public static List<Path> pathFromFilesThatExist(List<Path> paths) {
    return List.of(paths.stream().filter(Files::exists).toArray(Path[]::new));
  }
  
  public static void deleteAllFiles(Path directory) throws IOException {
    Files.walkFileTree(directory, new FileVisitor<Path>() {
      @Override
      public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
        Files.delete(path);
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
  
  public interface IOConsumer<T> {
    public void accept(T element) throws IOException;
  }
  public interface IOConsumer2<T, U> {
    public void accept(T t, U u) throws IOException;
  }
  public interface IOPredicate<T> {
    public boolean test(T element) throws IOException;
  }
  public interface IOFunction<T, R> {
    public R apply(T element) throws IOException;
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
  public static <T> Predicate<T> unchecked(IOPredicate<? super T> predicate) {
    return element -> {
      try {
        return predicate.test(element);
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
  
  public static void walkAndFindCounterpart(Path srcPath, Path dstPath, Function<Stream<Path>, Stream<Path>> configure, IOConsumer2<Path, Path> consumer) {
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
}
