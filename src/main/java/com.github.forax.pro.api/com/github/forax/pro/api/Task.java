package com.github.forax.pro.api;

import java.io.IOException;

/**
 * Define a task.
 * 
 * A task is {@link #execute(Config) executed} with a specific read-only configuration.
 * 
 * A task should never do side effects.
 */
@FunctionalInterface
public interface Task {
  /**
   * Execute the current task with a specific read-only configuration
   * 
   * @param config a read only configuration
   * @return an error code, 0 means OK, any other values means error.
   * @throws IOException if an I/O error occurs
   */
  public int execute(Config config) throws IOException;
}
