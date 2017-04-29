package com.github.forax.pro.api;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.github.forax.pro.api.impl.Configs;

public interface MutableConfig extends Config {
  public <T> T getOrUpdate(String key, Class<T> type);
  public void set(String key, Object object);
  
  public static <T, U, V> void derive(T to, BiConsumer<? super T, ? super V> setter, U from, Function<? super U, ? extends V> eval) {
    Configs.derive(to, setter, from, eval);
  }
}
