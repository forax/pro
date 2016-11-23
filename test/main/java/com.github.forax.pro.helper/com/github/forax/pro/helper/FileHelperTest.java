package com.github.forax.pro.helper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import com.github.forax.pro.helper.FileHelper;

public class FileHelperTest {

  public static void main(String[] args) {
    Predicate<Path> predicate = FileHelper.pathFilenameEndsWith("foo");
    predicate.test(Paths.get("bar.foo"));
  }

}
