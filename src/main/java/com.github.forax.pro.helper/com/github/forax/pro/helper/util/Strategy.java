package com.github.forax.pro.helper.util;

import java.util.List;
import java.util.Optional;

/**
 *  Represent a strategy of evaluation.
 *
 * @param <V> the type of parameter of the evaluation.
 * @param <R> the type of the result of the evaluation.
 */
public interface Strategy<V, R> {
  /**
   * Try to evaluate the value, it can fails in that case {@link Optional#empty()} is returned.
   * @param value the parameter to evaluate.
   * @return the result of the evaluation or empty.
   */
  Optional<R> get(V value);
  
  /**
   * Create a strategy that try each strategy in the order of the array.
   * @param <V> the type of parameter of the evaluation.
   * @param <R> the type of the result of the evaluation.
   * @param strategies an array of strategies
   * @return a strategy that will delegate its computation to several strategies.
   */
  @SafeVarargs
  static <V, R> Strategy<V, R> compose(Strategy<V, R>... strategies) {
    List<Strategy<V, R>> list = List.of(strategies);
    return value -> {
      for(Strategy<V, R> strategy: list) {
        var result = strategy.get(value);
        if (result.isPresent()) {
          return result;
        }
      }
      return Optional.empty();
    };
  }
}
