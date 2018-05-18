package com.github.forax.pro;

import java.lang.reflect.Proxy;

import com.github.forax.pro.api.impl.Configs.Query;

/**
 * Entry points for the typesafe builder API. 
 */
public class BuilderSupport {
  /**
   * Entry point of the builder API, create a proxy that will redirect all the calls to the current configuration
   * so the user code can use the same builder to change different underlying configuration.
   * 
   * @param <T> type of the plugin
   * @param key the name of the plugin that the builder reflect.
   * @param type the interface of the plugin that the builder reflect.
   * @return a new builder
   */
  public static <T> T createBuilderProxy(String key, Class<T> type) {
    Pro.getOrUpdate(key, type);  // create the 
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type, Query.class },
        (proxy, method, args) -> {
          T delegate = Pro.getOrUpdate(key, type);
          return method.invoke(delegate, args);
        }));
  }
}
