package com.github.forax.pro.api;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Read only part of the configuration tree API.
 * A config can be seen as set of qualified properties with their associated value.
 * Asking for a qualified key give you the value and asking for a component prefix of the qualified key
 * give you an object that contains all the keys that starts with this prefix.
 * 
 * By example,
 * {@code get("foo.bar")} returns the value associated with the key "bar" inside the config node "foo",
 * {@code forEach("foo", (k, v) -> System.out.println("key:" + k + " value:" + v));} will print the 
 * key "bar" with its value.
 * 
 * To create a tree node see {@link MutableConfig#getOrUpdate(String, Class)},
 * to update a value of a key, see {@link MutableConfig#set(String, Object)}.
 * 
 * @see MutableConfig
 */
public interface Config {
  /**
   * Returns the value associated with the key or {@code Optional#empty()} if there is no key registered. 
   * @param key a qualified name 
   * @param type the expected type of the value associated with the key.
   * @return either the value corresponding to the key or {@code Optional#empty()} otherwise.
   * 
   * @throws ClassCastException if the value is not a subclass of the given type.
   * @throws IllegalStateException if one of the component of the key is not a tree node.
   */
  public <T> Optional<T> get(String key, Class<T> type);
  
  /**
   * Calls the consumer with each pair key/value contained in a node and its sub nodes.
   * @param key a qualified name corresponding to a node
   * @param consumer the consumer called for each pair key/value
   */
  public void forEach(String key, BiConsumer<? super String, Object> consumer);
  
  /**
   * Returns the value associated with a key or throw a {@link NoSuchElementException} if the value do not exist.
   * @param key a qualified name.
   * @param type the expected type of the value corresponding to the key
   * @return  the value associated with the key.
   * 
   * @throws NoSuchElementException if the key has no corresponding value
   * @throws ClassCastException if the value is not a subclass of the given type.  
   * @throws IllegalStateException if one of the component of the key is not a tree node.
   */
  public default <T> T getOrThrow(String key, Class<T> type) {
    return get(key, type).orElseThrow(() -> new NoSuchElementException("no key " + key + " defined"));  
  }
}
