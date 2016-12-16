package com.github.forax.pro.api;

import java.nio.file.Path;

public interface WatcherRegistry {
  public void watch(Path directory);
}
