package com.github.forax.pro;

import static java.util.Objects.requireNonNull;

import com.github.forax.pro.helper.FileHelper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.CRC32;

public class Tools {
  private Tools() {
    throw new AssertionError();
  }

  /**
   * Delete recursively all files inside a directory.
   * @param directory a directory
   * @param removeDirectory also delete the top level directory.
   * @throws IOException if a file can not be deleted
   */
  public static void deleteTree(Path directory, boolean removeDirectory) throws IOException {
    FileHelper.deleteAllFiles(directory, removeDirectory);
  }

  /**
   * Copies an existing source directory recursively to the target directory.
   * <p>If the source directory does not exist, this method returns silently.
   * @param source source directory
   * @param target target directory
   * @throws IOException if a file can not be copied
   */
  public static void copyTree(Path source, Path target) throws IOException {
    if (!Files.exists(source)) {
      return;
    }
    FileHelper.walkAndFindCounterpart(source, target, Function.identity(), Files::copy);
  }

  /**
   * Compare two file trees by computing and comparing the CRC-32 of all sub-path names.
   * @param one first tree to check
   * @param two second tree to check
   * @return {@code true} if both trees are equals regarding their
   * @see CRC32
   */
  public static boolean compareTree(Path one, Path two) {
    return compareTree(one, two, (path, crc) -> crc.update(path.toString().getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Compare two file trees by applying the provided consumer on every sub-path.
   * @param one first tree to check
   * @param two second tree to check
   * @param consumer updates the crc instance with the current path
   * @return {@code true} if both trees are equals regarding their
   * @see CRC32
   */
  public static boolean compareTree(Path one, Path two, BiConsumer<Path, CRC32> consumer) {
    CRC32 crcOne = new CRC32();
    FileHelper.walkIfNecessary(List.of(one), __ -> true).forEach(path -> consumer.accept(one.relativize(path), crcOne));
    CRC32 crcTwo = new CRC32();
    FileHelper.walkIfNecessary(List.of(two), __ -> true).forEach(path -> consumer.accept(two.relativize(path), crcTwo));
    return crcOne.getValue() == crcTwo.getValue();
  }

  /**
   * Download the resource specified by its URI to the target directory.
   */
  public static Path download(URI uri, Path targetDirectory) throws IOException {
    return download(uri, targetDirectory, fileName(uri), targetPath -> true);
  }

  /**
   * Download the resource specified by its URI to the target directory using the provided file name.
   */
  public static Path download(URI uri, Path targetDirectory, String targetFileName, Predicate<Path> useTimeStamp) throws IOException {
    URL url = requireNonNull(uri, "uri must not be null").toURL();
    requireNonNull(targetDirectory, "targetDirectory must be null");
    if (requireNonNull(targetFileName, "targetFileName must be null").isEmpty()) {
      throw new IllegalArgumentException("targetFileName must be blank");
    }
    Files.createDirectories(targetDirectory);
    Path targetPath = targetDirectory.resolve(targetFileName);
    URLConnection urlConnection = url.openConnection();
    FileTime urlLastModifiedTime = FileTime.fromMillis(urlConnection.getLastModified());
    if (Files.exists(targetPath)) {
      if (Files.getLastModifiedTime(targetPath).equals(urlLastModifiedTime)) {
        if (Files.size(targetPath) == urlConnection.getContentLengthLong()) {
          if (useTimeStamp.test(targetPath)) {
            return targetPath;
          }
        }
      }
      Files.delete(targetPath);
    }
    try (InputStream sourceStream = url.openStream(); OutputStream targetStream = Files.newOutputStream(targetPath)) {
      sourceStream.transferTo(targetStream);
    }
    Files.setLastModifiedTime(targetPath, urlLastModifiedTime);
    return targetPath;
  }

  /**
   * Extract the file name from the uri.
   */
  public static String fileName(URI uri) {
    String urlString = uri.getPath();
    return urlString.substring(urlString.lastIndexOf('/') + 1).split("\\?")[0].split("#")[0];
  }

}
