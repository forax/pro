package com.github.forax.pro.api;

public interface MutableConfig extends Config {
  public <T> T getOrUpdate(String key, Class<T> type);
  public void set(String key, Object object);
}
