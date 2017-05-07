package com.github.forax.pro.api;

import com.github.forax.pro.api.impl.Configs;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Read/write part of the configuration tree API.
 *
 * @see Config
 */
public interface MutableConfig extends Config {
  /**
   * Returns the value associated with the key or use vivification to dynamically create an object
   * implementing the interface {@code type}.
   *
   * <p>This method can only be used if the tree is in read/write mode.
   *
   * @see #get(String, Class) for a read-only access.
   * @param key a qualified name
   * @param type the type of the value, it must be an interface tagged with the annotation {@code
   *     TypeCheckedConfig} to allow vivification.
   * @throws NoSuchElementException if there is no value associated with the key.
   * @throws ClassCastException if the value is not a subclass of the given type.
   * @throws IllegalStateException if one of the component of the key is not a tree node.
   */
  public <T> T getOrUpdate(String key, Class<T> type);

  /**
   * Change the value associated with the key.
   *
   * @param key a qualified name
   * @param value a value
   * @throws ClassCastException if the component prefix is a vivified object and the value is not a
   *     subclass of the type of the getter method of the interface used to create the vivified
   *     object.
   * @throws IllegalStateException if one of the component of the key is not a tree node.
   */
  public void set(String key, Object value);

  /**
   * Allow to create a computed value that depends on another value from the same tree. Each time
   * the value will be asked, the function {@code eval} will be evaluated.
   *
   * @param to a vivified object that was obtained from the tree
   * @param setter a lambda that will set a key of the vivified object, it must be a method of the
   *     interface of the vivified object.
   * @param from a vivified object containing the value from which the current value will be derived
   * @param eval a function that calculate the value from the from object.
   */
  public static <T, U, V> void derive(
      T to,
      BiConsumer<? super T, ? super V> setter,
      U from,
      Function<? super U, ? extends V> eval) {
    Configs.derive(to, setter, from, eval);
  }
}
