package com.github.forax.pro.plugin.formatter;

import java.nio.file.Path;
import java.util.List;

import com.github.forax.pro.api.TypeCheckedConfig;
import java.util.Optional;

@TypeCheckedConfig
public interface FormatterConf {
  Optional<List<Path>> files();
  void files(List<Path> files);
}
