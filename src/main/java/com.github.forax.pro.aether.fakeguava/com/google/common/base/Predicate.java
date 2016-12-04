package com.google.common.base;

@FunctionalInterface
public interface Predicate<T> extends java.util.function.Predicate<T> {
  boolean apply(T input);

  @Override
  default boolean test(T input) {
    return apply(input);
  }
}