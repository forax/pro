package com.google.common.collect;

import java.util.Collection;

import com.google.common.base.Predicate;

public final class Iterables {
  private Iterables() {}
  
  public static <T> boolean removeIf(Iterable<T> iterable, Predicate<? super T> predicate) {
    return ((Collection<T>)iterable).removeIf(predicate);
  }
}
