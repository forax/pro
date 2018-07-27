package com.github.forax.pro.main.runner;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public interface PropertySequence {
  public Stream<Map.Entry<String, Object>> stream();
  
  public default PropertySequence append(PropertySequence seq) {
    Objects.requireNonNull(seq);
    return () -> Stream.concat(stream(), seq.stream());
  }
  
  public default void forEach(BiConsumer<? super String, Object> consumer) {
    Objects.requireNonNull(consumer);
    stream().forEach(entry -> consumer.accept(entry.getKey(), entry.getValue()));
  }
  
  public static PropertySequence empty() {
    return Stream::empty;
  }
  
  public static PropertySequence property(String name, Object value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
    return () -> Stream.of(Map.entry(name, value));
  }
}
