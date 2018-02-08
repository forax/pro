package com.github.forax.pro.helper.util;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * An unmodifiable list that tries to share it's backing array if possible.
 * 
 * This list is not thread safe.
 * 
 * This list is optimize for the case where {@link StableList#append(Object)} is used
 * without keeping a reference to the original stable list. 
 *
 * @param <E> the type of the element
 */
public final class StableList<E> extends AbstractList<E> implements RandomAccess {
  private final int size;
  private final E[] array;
  private boolean shared;
  
  private static final StableList<?> EMPTY = new StableList<>(0, new Object[0]); 
  
  private StableList(int size, E[] array) {
    this.size = size;
    this.array = array;
  }
  
  @Override
  public int size() {
    return size;
  }
  
  @Override
  public E get(int index) {
    Objects.checkIndex(index, size);
    return array[index];
  }
  
  @Override
  public Spliterator<E> spliterator() {
    return Spliterators.spliterator(array, 0, size, Spliterator.NONNULL|Spliterator.IMMUTABLE);
  }
  
  /**
   * Append the element at the end of the current list
   * with the element append at the end.
   * @param element the element to append to the current stable list, must be non null
   * @return a new StableList with the element appended at the end.
   * @throws NullPointerException is the element is null.
   */
  public StableList<E> append(E element) {
    Objects.requireNonNull(element);
    E[] array = this.array;
    int size = this.size;
    boolean shared = this.shared;
    int length = array.length;
    E[] newArray = (length == size)? Arrays.copyOf(array, Math.max(1, length) << 1): shared? Arrays.copyOf(array, length): array;
    newArray[size] = element;
    this.shared = shared | array == newArray;
    return new StableList<>(size + 1, newArray);
  }
  
  /**
   * Append all elements of the collection taken as parameter at the end of the current list.
   * @param list a list of elements, each one must be non null.
   * @return a new StableList with the elements of the collection appended at the end.
   * @throws NullPointerException if one element of the collection is null.
   */
  public StableList<E> appendAll(Collection<? extends E> list) {
    // TODO optimize !
    StableList<E> result = this;
    for(E element: list) {
      result = result.append(element);
    }
    return result;
  }
  
  /**
   * Append all elements of the array taken as parameter at the end of the current list.
   * @param elements a array of elements, each one must be non null.
   * @return a new StableList with the elements of the array appended at the end.
   * @throws NullPointerException if one element of the array is null.
   */
  @SafeVarargs
  public final StableList<E> appendAll(E... elements) {
    // TODO optimize !
    StableList<E> result = this;
    for(E element: elements) {
      result = result.append(element);
    }
    return result;
  }
  
  /**
   * Returns a stable list consisting of the elements of this list
   * that match the given predicate.
   * 
   * @param predicate a function that returns true is the element will be
   *        in the resulting list.
   * @return a new stable list
   */
  public StableList<E> filter(Predicate<? super E> predicate) {
    Objects.requireNonNull(predicate);
    return stream().filter(predicate).collect(toStableList());
  }
  
  /**
   * Returns a stable list consisting of the results of applying the given
   * function to the elements of this list. 
   * 
   * @param mapper a function that takes an element and returns the new
   *        element to be stored in the new list
   * @return a new stable list
   */
  public <R> StableList<R> map(Function<? super E, ? extends R> mapper) {
    Objects.requireNonNull(mapper);
    return stream().map(e -> Objects.requireNonNull(mapper.apply(e))).collect(toStableList());
  }
  
  /**
   * Concatenate all elements separated by a delimiter. The prefix is added before  
   * @param delimiter a delimiter
   * @param prefix a prefix
   * @param suffix a suffix
   * @return a string joining a string representation of all elements separated
   *         by the delimiter
   */
  public String join(String delimiter, String prefix, String suffix) {
    return stream().map(Object::toString).collect(Collectors.joining(delimiter, prefix, suffix));
  }
  
  /**
   * Concatenate all elements separated by a delimiter.
   * This call is equivalent to
   * {@link #join(String, String, String) join(delimiter, "", "")}.
   * 
   * @param delimiter a delimiter
   * @return a string joining a string representation of all elements separated
   *         by the delimiter
   */
  public String join(String delimiter) {
    return join(delimiter, "", "");
  }
  
  /**
   * Create an array containing the elements of the current list.
   * 
   * @param factory a function which produces a new array from a provided length
   * @return the array created by the factory populated with the values
   */
  public <T> T[] toArray(IntFunction<T[]> factory) {
    T[] array = factory.apply(size);
    System.arraycopy(this.array, 0, array, 0, size);
    return array;
  }
  
  /**
   * Create an empty StableList.
   * @return an empty StableList.
   */
  @SuppressWarnings("unchecked")
  public static <E> StableList<E> of() {
    return (StableList<E>)EMPTY;
  }
  
  /**
   * Create a new StableList and populate it with the elements taken as parameters.
   * @param elements a array of elements.
   * @return a new StableList with all elements of the array.
   * @throws NullPointerException if one element of the array is null.
   */
  @SafeVarargs
  public static <E> StableList<E> of(E... elements) {
    return fromTrustedArray(Arrays.copyOf(elements, elements.length));
  }
  
  private static <E> StableList<E> fromTrustedArray(E[] array) {
    for(E e: array) {
      Objects.requireNonNull(e);
    }
    return new StableList<>(array.length, array);
  }
  
  /**
   * Create a new StableList and populate it with the elements of the collection taken as parameters.
   * @param collection a collection of elements.
   * @return a new StableList with all elements of the list.
   * @throws NullPointerException if one element of the collection is null or
   *         the collection itself is null.
   */
  @SuppressWarnings("unchecked")
  public static <E> StableList<E> from(Collection<? extends E> collection) {
    if (collection.isEmpty()) {
      return StableList.of();
    }
    if (collection instanceof StableList<?>) {
      return (StableList<E>)collection;
    }
    return fromTrustedArray((E[])collection.toArray(new Object[0]));
  }
  
  /**
   * Returns a collector that store all elements in a stable list.
   * @return a stable list storing all elements.
   */
  public static <T> Collector<T, ?, StableList<T>> toStableList() {
    class Box {
      StableList<T> list;
      public Box(StableList<T> list) { this.list = list; }
    }
    return Collector.of(() -> new Box(StableList.of()),
        (box, element) -> box.list = box.list.append(element),
        (box1, box2) -> { box1.list = box1.list.appendAll(box2.list); return box1; },
        box -> box.list);
  }
  
  /*
  public static void main(String[] args) {
    StableList<String> list = new StableList<>();
    StableList<String> list2 = list.append("foo").append("bar");
    System.out.println(list2);
    StableList<String> list3 = list2.append("baz");
    StableList<String> list4 = list2.append("bang");
    System.out.println(list3);
    System.out.println(list4);
  }*/
}
