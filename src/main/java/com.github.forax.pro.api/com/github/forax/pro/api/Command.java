package com.github.forax.pro.api;

import java.io.IOException;

/**
 * Define a command.
 * 
 * A command is {@link #execute(Config) executed} with a specific read-only configuration.
 * 
 * A command should never do side effects.
 */
public interface Command {
  /**
   * Name of the command.
   * The name is used when calling Pro.run() and it's the name of the node
   * in the configuration tree.
   * The name should use the camel case convention.
   * 
   * @return the name of the command.
   */
  public String name();
  
  /**
   * Execute the current command with a specific read-only configuration
   * 
   * @param config a read only configuration
   * @return an error code, 0 means OK, any other values means error.
   * @throws IOException if an I/O error occurs
   */
  public int execute(Config config) throws IOException;
}
