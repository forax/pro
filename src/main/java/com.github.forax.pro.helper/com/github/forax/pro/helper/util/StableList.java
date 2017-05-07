package com.github.forax.pro.helper.util;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * An unmodifiable list that tries to share it's backing array if possible. This list is optimize
 * for the case where {@link StableList#append(Object)} is used without keeping a reference to the
 * original stable list.
 *
 * @param <E> the type of the element
 */
public final class StableList<E> extends AbstractList<E> implements RandomAccess {
  private final int size;
  private final E[] array;
  private boolean shared;

  private static final Object[] EMPTY_ARRAY = new Object[0];
  private static final StableList<?> EMPTY = new StableList<>();

  private StableList(int size, E[] array) {
    this.size = size;
    this.array = array;
  }

  @SuppressWarnings("unchecked")
  public StableList() {
    this(0, (E[]) EMPTY_ARRAY);
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
    return Spliterators.spliterator(array, 0, size, Spliterator.NONNULL);
  }

  /**
   * Append the element at the end of the current list with the element append at the end.
   *
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
    E[] newArray =
        (length == size)
            ? Arrays.copyOf(array, Math.max(1, length) << 1)
            : shared ? Arrays.copyOf(array, length) : array;
    newArray[size] = element;
    this.shared = shared | array == newArray;
    return new StableList<>(size + 1, newArray);
  }

  /**
   * Append all elements of the collection taken as parameter at the end of the current list.
   *
   * @param collection a collection of elements, each one must be non null.
   * @return a new StableList with the elements of the collection appended at the end.
   * @throws NPE if one element of the collection is null.
   */
  public StableList<E> appendAll(Collection<? extends E> list) {
    // TODO optimize !
    StableList<E> result = this;
    for (E element : list) {
      result = result.append(element);
    }
    return result;
  }

  /**
   * Append all elements of the array taken as parameter at the end of the current list.
   *
   * @param elements a array of elements, each one must be non null.
   * @return a new StableList with the elements of the array appended at the end.
   * @throws NPE if one element of the array is null.
   */
  @SafeVarargs
  public final StableList<E> appendAll(E... elements) {
    // TODO optimize !
    StableList<E> result = this;
    for (E element : elements) {
      result = result.append(element);
    }
    return result;
  }

  /**
   * Returns a stable list consisting of the elements of this list that match the given predicate.
   *
   * @param predicate a function that returns true is the element will be in the resulting list.
   * @param a new stable list
   */
  public StableList<E> filter(Predicate<? super E> predicate) {
    return stream().filter(predicate).collect(toStableList());
  }

  /**
   * Returns a stable list consisting of the results of applying the given function to the elements
   * of this list.
   *
   * @param mapper a function that takes an element and returns the new element to be stored in the
   *     new list
   * @param a new stable list
   */
  public <R> StableList<R> map(Function<? super E, ? extends R> mapper) {
    return stream().map(mapper).collect(toStableList());
  }

  /**
   * Concatenate all elements separated by a delimiter. The prefix is added before
   *
   * @param delimiter a delimiter
   * @param prefix a prefix
   * @param suffix a suffix
   * @return a string joining a string representation of all elements separated by the delimiter
   */
  public String join(String delimiter, String prefix, String suffix) {
    return stream().map(Object::toString).collect(Collectors.joining(delimiter, prefix, suffix));
  }

  /**
   * Concatenate all elements separated by a delimiter. This call is equivalent to {@link
   * #join(String, String, String) join(delimiter, "", "")}.
   *
   * @param delimiter a delimiter
   * @return a string joining a string representation of all elements separated by the delimiter
   */
  public String join(String delimiter) {
    return join(delimiter, "", "");
  }

  /**
   * Create an empty StableList.
   *
   * @return an empty StableList.
   */
  @SuppressWarnings("unchecked")
  public static <E> StableList<E> of() {
    return (StableList<E>) EMPTY;
  }

  /**
   * Create a new StableList and populate it with the elements taken as parameters.
   *
   * @param elements a array of elements.
   * @return a new StableList with all elements of the array.
   * @throws NPE if one element of the array is null.
   */
  @SafeVarargs
  public static <E> StableList<E> of(E... elements) {
    return StableList.<E>of().appendAll(elements);
  }

  /**
   * Create a new StableList and populate it with the elements of the list taken as parameters.
   *
   * @param list a list of elements.
   * @return a new StableList with all elements of the list.
   * @throws NPE if one element of the array is null.
   */
  public static <E> StableList<E> from(Collection<? extends E> list) {
    return StableList.<E>of().appendAll(list);
  }

  /**
   * Returns a collector that store all elements in a stable list.
   *
   * @return a stable list storing all elements.
   */
  @SuppressWarnings("unchecked")
  public static <T> Collector<T, ?, StableList<T>> toStableList() {
    return Collector.of(
        () -> (StableList<T>[]) new StableList<?>[] {StableList.of()},
        (array, element) -> array[0] = array[0].append(element),
        (array1, array2) -> {
          array1[0] = array1[0].appendAll(array2[0]);
          return array1;
        },
        array -> array[0]);
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
