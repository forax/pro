package com.github.forax.pro.api;

/**
 * Define a stateless plugin.
 * 
 * A plugin can {@link #init(MutableConfig) initialization} a configuration,
 * {@link #configure(MutableConfig)}e a configuration to add derived values,
 * a be {@link #execute(Config) executed} with a specific configuration.
 * 
 * A class that implements this interface should *never* have fields !
 */
public interface Plugin extends Task {
  /**
   * Called to register the plugin properties with their default values
   * (with {@link MutableConfig#set(String, Object)}).
   * 
   * This method can be called several times with different configurations.
   * The configuration of other plugins may not available when this method
   * is called. If you want to access to the configuration of other plugins,
   * see {@link #configure(MutableConfig)}.
   * 
   * This method can only set values for properties that starts with the
   * {@link #name()} of the plugin.
   * 
   * The implementation of this method should *never* try to store the
   * {@code config} object send as parameter.
   * 
   * @param config a mutable configuration
   */
  public void init(MutableConfig config);
  
  /**
   * Called to register plugin properties that depends on values of other plugins.
   * 
   * This method can be called several times with different configurations.
   * 
   * The implementation of this method should use 
   * {@link MutableConfig#derive(Object, java.util.function.BiConsumer, Object, java.util.function.Function)}
   * to explain how a value is computed from the values of other properties.
   * 
   * The implementation of this method should *never* try to store the
   * {@code config} object send as parameter.
   * 
   * @param config a mutable configuration
   */
  public void configure(MutableConfig config);
  
  /**
   * Register the paths that should be watched when pro is started in daemon mode
   * 
   * THIS METHOD IS EXPERIMENTAL AND MAY CHANGE IN THE FUTURE
   * 
   * @param config    a read only configuration
   * @param registry  the registry of path that need to be watched 
   * 
   */
  public void watch(Config config, WatcherRegistry registry);
}
