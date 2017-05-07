package com.github.forax.pro.api;

import java.nio.file.Path;

/** Ask to watch a specific directory in pro daemon mode. */
public interface WatcherRegistry {
  /**
   * Register a specific directory to be watch
   *
   * @param directory a directory
   */
  public void watch(Path directory);
}
