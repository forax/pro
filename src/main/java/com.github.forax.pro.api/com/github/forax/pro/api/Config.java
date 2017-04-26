package com.github.forax.pro.api;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface Config {
  public <T> Optional<T> get(String key, Class<T> type);
  
  public void forEach(String key, BiConsumer<? super String, Object> consumer);
  
  public void addListener(String key, Consumer<? super String> consumer);
  
  public default <T> T getOrThrow(String key, Class<T> type) {
    return get(key, type).orElseThrow(() -> new NoSuchElementException("no key " + key + " defined"));  
  }
}
