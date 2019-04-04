package com.github.forax.pro.helper.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 */
public class Unchecked {
  @FunctionalInterface
  public interface IORunnable {
    public void run() throws Throwable;
  }
  public interface IOSupplier<T> {
    public T get() throws Throwable;
  }
  public interface IOConsumer<T> {
    public void accept(T element) throws Throwable;
  }
  public interface IOBiConsumer<T, U> {
    public void accept(T t, U u) throws Throwable;
  }
  public interface IOFunction<T, R> {
    public R apply(T element) throws Throwable;
  }
  public interface IOBiFunction<T, U, R> {
    public R apply(T element, U element2) throws Throwable;
  }

  public static Runnable suppress(IORunnable runnable) {
    return () -> runUnchecked(runnable);
  }
  public static <T> Supplier<T> suppress(IOSupplier<? extends T> supplier) {
    return () -> getUnchecked(supplier);
  }
  public static <T> Consumer<T> suppress(IOConsumer<? super T> consumer) {
    return element -> acceptUnchecked(element, consumer);
  }
  public static <T, U> BiConsumer<T, U> suppress(IOBiConsumer<? super T, ? super U> consumer) {
    return (element, element2) -> acceptUnchecked(element, element2, consumer);

  }
  public static <T, R> Function<T, R> suppress(IOFunction<? super T, ? extends R> function) {
    return element -> applyUnchecked(element, function);
  }
  public static <T, U, R> BiFunction<T, U, R> suppress(IOBiFunction<? super T, ? super U, ? extends R> function) {
    return (element, element2) -> applyUnchecked(element, element2, function);
  }
  
  public static <V> V getUnchecked(IOSupplier<? extends V> supplier) {
    try {
      return supplier.get(); 
     } catch(Error | RuntimeException e) {
       throw e;
     } catch(Throwable throwable) {
       throw Hack.rethrow(throwable);
     }
  }
  public static void runUnchecked(IORunnable runnable) {
    try {
     runnable.run(); 
    } catch(Error | RuntimeException e) {
      throw e;
    } catch(Throwable throwable) {
      throw Hack.rethrow(throwable);
    }
  }
  public static <T> void acceptUnchecked(T element, IOConsumer<? super T> consumer) {
      try {
        consumer.accept(element);
      } catch(Error | RuntimeException e) {
        throw e;
      } catch(Throwable e) {
        throw Hack.rethrow(e);
      }
  }
  public static <T, U> void acceptUnchecked(T element, U element2, IOBiConsumer<? super T, ? super U> consumer) {
    try {
      consumer.accept(element, element2);
    } catch (Error | RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw Hack.rethrow(e);
    }
  }
  public static <T, R> R applyUnchecked(T element, IOFunction<? super T, ? extends R> function) {
    try {
      return function.apply(element);
    } catch(Error | RuntimeException e) {
      throw e;
    } catch(Throwable e) {
      throw Hack.rethrow(e);
    }
  }
  public static <T, U, R> R applyUnchecked(T element, U element2, IOBiFunction<? super T, ? super U, ? extends R> function) {
    try {
      return function.apply(element, element2);
    } catch(Error | RuntimeException e) {
      throw e;
    } catch(Throwable e) {
      throw Hack.rethrow(e);
    }
  }
  
  /**
   * Empty method that allow to mark that a suppressed exception will be propagated.
   * 
   * @param type type of the exception that will be raised by the calling method.
   * @param <T> the type of the exception that will be raised by the calling method.
   * @throws T the type of the exception that will be raised by the calling method.
   */
  public static <T extends Throwable> void raises(Class<T> type) throws T {
    // empty
  }

  private interface Hack<T extends Throwable> {
    RuntimeException call(Throwable t) throws T;
    
    @SuppressWarnings("unchecked")
    private static RuntimeException rethrow(Throwable throwable) {
      throw ((Hack<RuntimeException>)(Hack<?>)(Hack<Throwable>) t -> { throw t; }).call(throwable);
    }
  } 
}