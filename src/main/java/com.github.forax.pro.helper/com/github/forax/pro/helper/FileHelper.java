package com.github.forax.pro.helper;

import static com.github.forax.pro.helper.util.Unchecked.getUnchecked;
import static com.github.forax.pro.helper.util.Unchecked.runUnchecked;
import static com.github.forax.pro.helper.util.Unchecked.suppress;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newOutputStream;
import static java.nio.file.Files.walk;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.github.forax.pro.helper.util.Unchecked;

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
   */
  public static void deleteAllFiles(Path directory, boolean removeDirectory) {
    // IOExceptions are suppressed
    runUnchecked(() -> walkFileTree(directory, new FileVisitor<>() {
      @Override
      public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
        if (removeDirectory || !path.equals(directory)) {
          Files.delete(path);  
        }
        return CONTINUE;
      }

      @Override
      public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
        return CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        Files.delete(path);
        return CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
        return CONTINUE;
      }
    }));
  }
  
  public static Predicate<Path> pathFilenameEndsWith(String text) {
    return path -> path.getFileName().toString().endsWith(text);
  }
  
  public static Predicate<Path> pathFilenameEquals(String text) {
    return path -> path.getFileName().toString().equals(text);
  }
  
  public static List<Path> walkIfNecessary(List<Path> list, Predicate<? super Path> filter) {
    // IOExceptions are suppressed
    return list.stream()
               .flatMap(suppress(path -> {
                   if (!isDirectory(path)) {
                     return Stream.of(path);
                   }
                   return walk(path).filter(filter);
               }))
               .collect(toUnmodifiableList());
  }
  
  public static void walkAndFindCounterpart(Path srcPath, Path dstPath, Function<Stream<Path>, Stream<Path>> configure, Unchecked.IOBiConsumer<Path, Path> consumer) {
    // IOExceptions are suppressed
    try(var stream = getUnchecked(() -> Files.walk(srcPath))) {
      configure.apply(stream)
        .forEach(suppress(path -> {
          var targetPath = dstPath.resolve(srcPath.relativize(path));
          consumer.accept(path, targetPath);
      }));
    }
  }
  
  public static void download(URI uri, Path targetDirectory) {
    // IOExceptions are suppressed
    var fileName = Path.of(uri.getPath()).getFileName();
    var targetFile = targetDirectory.resolve(fileName);
    if (exists(targetFile)) {
      return;
    }
    runUnchecked(() -> {
      try { 
        createDirectories(targetDirectory);
        try(var input = uri.toURL().openStream();
            var output = newOutputStream(targetFile, CREATE_NEW, WRITE)) {
          input.transferTo(output);
        }
      } catch (IOException e) {
        throw new IOException("download failed: url=" + uri + " targetDirectory=" + targetDirectory, e);
      }
    });
  }
}
