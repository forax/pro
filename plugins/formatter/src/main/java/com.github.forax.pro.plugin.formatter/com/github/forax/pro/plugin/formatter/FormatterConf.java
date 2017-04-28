package com.github.forax.pro.plugin.formatter;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;

@TypeCheckedConfig
public interface FormatterConf {
  List<Path> javaFiles();
  void javaFiles(List<Path> javaFilesToFormat);
}
