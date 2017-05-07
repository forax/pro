package com.github.forax.pro.api;

import java.io.IOException;

/**
 * Define a stateless plugin.
 *
 * <p>A plugin can {@link #init(MutableConfig) initialization} a configuration, {@link
 * #configure(MutableConfig)}e a configuration to add derived values, a be {@link #execute(Config)
 * executed} with a specific configuration.
 *
 * <p>A class that implements this interface should *never* have fields !
 */
public interface Plugin {
  /**
   * Name of the plugin. The name is used when calling Pro.run() and it's the name of the node in
   * the configuration tree. The name should use the camel case convention.
   *
   * @return the name of the plugin.
   */
  public String name();

  /**
   * Called to register the plugin properties with their default values (with {@link
   * MutableConfig#set(String, Object)}).
   *
   * <p>This method can be called several times with different configurations. The configuration of
   * other plugins may not available when this method is called. If you want to access to the
   * configuration of other plugins, see {@link #configure(MutableConfig)}.
   *
   * <p>This method can only set values for properties that starts with the {@link #name()} of the
   * plugin.
   *
   * <p>The implementation of this method should *never* try to store the {@code config} object send
   * as parameter.
   *
   * @param config a mutable configuration
   */
  public void init(MutableConfig config);

  /**
   * Called to register plugin properties that depends on values of other plugins.
   *
   * <p>This method can be called several times with different configurations.
   *
   * <p>The implementation of this method should use
   *
   * @link {@link MutableConfig#derive(Object, java.util.function.BiConsumer, Object,
   *     java.util.function.Function)} to explain how a value is computed from the values of other
   *     properties.
   *     <p>The implementation of this method should *never* try to store the {@code config} object
   *     send as parameter.
   * @param config a mutable configuration
   */
  public void configure(MutableConfig config);

  /**
   * Register the paths that should be watched when pro is started in daemon mode
   *
   * <p>THIS METHOD IS EXPERIMENTAL AND MAY CHANGE IN THE FUTURE
   *
   * @param config a read only configuration
   * @param registry the registry of path that need to be watched
   */
  public void watch(Config config, WatcherRegistry registry);

  /**
   * Execute the current plugin with a specific read only configuration
   *
   * @param config a read only configuration
   * @return an error code, 0 means OK, any other values means error.
   * @throws IOException if an I/O error occurs
   */
  public int execute(Config config) throws IOException;
}
