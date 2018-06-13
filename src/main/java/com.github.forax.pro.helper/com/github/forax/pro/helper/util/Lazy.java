package com.github.forax.pro.helper.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Represent a lazy evaluation of a value.
 *
 * @param <V> the type of the value
 */
public interface Lazy<V> {
  /**
   * Evaluate the value if the value was not evaluated yet.
   * @return the value.
   */
  public V eval();

  /**
   * Create a lazy evaluation of a value from a computation (a supplier).
   * @param <V> the type of the value
   * @param supplier the computation of the value, should be side effect free.
   * @return the result of the computation
   */
  static <V> Lazy<V> lazy(Supplier<? extends V> supplier) {
    Objects.requireNonNull(supplier);
    return new Lazy<>() {
      private V value;

      @Override
      public V eval() {
        if (value != null) {
          return value;
        }
        return value = supplier.get();
      }
    };
  }
}