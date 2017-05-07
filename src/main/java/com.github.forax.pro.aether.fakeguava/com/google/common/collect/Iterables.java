package com.google.common.collect;

import com.google.common.base.Predicate;
import java.util.Collection;

public final class Iterables {
  private Iterables() {}

  public static <T> boolean removeIf(Iterable<T> iterable, Predicate<? super T> predicate) {
    return ((Collection<T>) iterable).removeIf(predicate);
  }
}
