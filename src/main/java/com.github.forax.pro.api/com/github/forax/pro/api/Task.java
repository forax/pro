package com.github.forax.pro.api;

import java.io.IOException;

/**
 * Define a task.
 * 
 * A task is {@link #execute(Config) executed} with a specific read-only configuration.
 * 
 * A task should never do side effects.
 */
public interface Task {
  /**
   * Name of the task.
   * The name is used when calling Pro.run() and it's the name of the node
   * in the configuration tree.
   * The name should use the camel case convention.
   * 
   * @return the name of the task.
   */
  public String name();
  
  /**
   * Execute the current task with a specific read-only configuration
   * 
   * @param config a read only configuration
   * @return an error code, 0 means OK, any other values means error.
   * @throws IOException if an I/O error occurs
   */
  public int execute(Config config) throws IOException;
}
