package com.github.forax.pro.helper.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.IntFunction;

@SuppressWarnings("static-method")
class StableListTests {
  @Test
  public void of() {
    assertAll("empty stable list",
      () -> assertSame(StableList.<String>of(), StableList.of(), "should be interned"),
      () -> assertTrue(StableList.of().isEmpty()),
      () -> assertEquals(0, StableList.of().size()),
      () -> assertEquals("[]", StableList.of().toString())
    );
  }

  @Test
  public void ofVarargs() {
    assertAll("stable list from varargs",
      () -> assertThrows(NullPointerException.class, () -> StableList.of((Object[])null)),
      () -> assertThrows(NullPointerException.class, () -> StableList.of((Object)null)),
      () -> assertThrows(NullPointerException.class, () -> StableList.of("foo", null, "bar")),
      () -> assertTrue(StableList.of(new Object[0]).isEmpty()),
      () -> assertEquals("[biz, baz, booz]", StableList.of("biz", "baz", "booz").toString())
    );
  }

  @Test
  public void toStableList() {
    List<Integer> list = List.of(1, 5, 8);
    assertAll(
      () -> assertEquals(StableList.of(1, 5, 8), list.stream().collect(StableList.toStableList())),
      () -> assertThrows(NullPointerException.class,
          () -> list.stream().map(__ -> null).collect(StableList.toStableList()))
    );
  }

  @Test
  public void testToString() {
    StableList<String> list1 = StableList.of("foo", "bar");
    StableList<String> list2 = list1.append("baz");
    StableList<String> list3 = list1.append("bang");
    assertAll(
      () -> assertEquals("[foo, bar]", list1.toString()),
      () -> assertEquals("[foo, bar, baz]", list2.toString()),
      () -> assertEquals("[foo, bar, bang]", list3.toString())
    );
  }

  @Test
  public void append() {
    StableList<String> list = StableList.<String>of().append("foo").append("bar");
    assertAll(
      () -> assertEquals(List.of("foo", "bar"), list),
      () -> assertEquals(List.of("foo", "bar", "baz"), list.append("baz")),
      () -> assertEquals(List.of("foo", "bar", "bang"), list.append("bang")),
      () -> assertThrows(NullPointerException.class, () -> StableList.of().append(null))
    );
  }

  @Test
  public void appendAllVarargs() {
    StableList<String> list = StableList.of("bob");
    assertAll(
      () -> assertEquals(List.of(), StableList.of().appendAll()),
      () -> assertEquals(List.of("zoo"), StableList.of().appendAll("zoo")),
      () -> assertEquals(List.of("bob"), list.appendAll()),
      () -> assertEquals(List.of("bob", "bab"), list.appendAll("bab")),
      () -> assertEquals(List.of("bob", "bat", "big"), list.appendAll("bat", "big")),
      () -> assertThrows(NullPointerException.class, () -> StableList.of().appendAll((Object[])null)),
      () -> assertThrows(NullPointerException.class, () -> StableList.of().appendAll("foo", null, "bar"))
    );
  }

  @Test
  public void appendAllCollection() {
    StableList<String> list = StableList.of("bob");
    assertAll(
      () -> assertEquals(List.of(), StableList.of().appendAll(List.of())),
      () -> assertEquals(List.of("zoo"), StableList.of().appendAll(List.of("zoo"))),
      () -> assertEquals(List.of("bob"), list.appendAll(List.of())),
      () -> assertEquals(List.of("bob", "bab"), list.appendAll(List.of("bab"))),
      () -> assertEquals(List.of("bob", "bat", "big"), list.appendAll(List.of("bat", "big"))),
      () -> assertThrows(NullPointerException.class, () -> StableList.of().appendAll((List<?>)null)),
      () -> assertThrows(NullPointerException.class, () -> StableList.of().appendAll(List.of("foo", null)))
    );
  }

  @Test
  public void filter() {
    assertAll(
      () -> StableList.of().filter(__ -> { fail("should not be called"); return true; }),
      () -> assertEquals(List.of("foo"), StableList.of("foo").filter(__ -> true)),
      () -> assertEquals(List.of(2), StableList.of(1, 2, 3).filter(i -> i % 2 == 0)),
      () -> assertThrows(NullPointerException.class, () -> StableList.of().filter(null))
    );
  }

  @Test
  public void map() {
    assertAll(
      () -> StableList.of().map(__ -> { fail("should not be called"); return ""; }),
      () -> assertEquals(List.of(3), StableList.of("3").map(Integer::parseInt)),
      () -> assertEquals(List.of("0", "42"), StableList.of(0, 42).map(Object::toString)),
      () -> assertThrows(NullPointerException.class, () -> StableList.of().map(null)),
      () -> assertThrows(NullPointerException.class, () -> StableList.of(1).map(__ -> null))
    );
  }

  @Test
  public void join() {
    assertAll(
      () -> assertEquals("{}", StableList.of().join(",", "{", "}")),
      () -> assertEquals("", StableList.of().join(",")),
      () -> assertEquals("{3}", StableList.of("3").join(",", "{", "}")),
      () -> assertEquals("3", StableList.of("3").join(",")),
      () -> assertEquals("[0, 42]", StableList.of(0, 42).join(", ", "[", "]")),
      () -> assertEquals("0, 42", StableList.of(0, 42).join(", ")),
      () -> assertThrows(NullPointerException.class, () -> StableList.of().join(null, "", "")),
      () -> assertThrows(NullPointerException.class, () -> StableList.of().join("", null, "")),
      () -> assertThrows(NullPointerException.class, () -> StableList.of().join("", "", null)),
      () -> assertThrows(NullPointerException.class, () -> StableList.of().join(null))
    );
  }
  
  @Test
  public void toArrayFactory() {
    assertAll(
      () -> assertArrayEquals(new Object[0], StableList.of().toArray(Object[]::new)),
      () -> assertArrayEquals(new Integer[] {1, 3}, StableList.of(1, 3).toArray(Integer[]::new)),
      () -> assertArrayEquals(new Object[] { "foo" }, StableList.of("foo").toArray(Object[]::new)),
      () -> assertArrayEquals(new String[] { "foo", "bar", "baz" }, StableList.of("foo", "bar", "baz").toArray(String[]::new)),
      () -> assertThrows(NullPointerException.class, () -> StableList.of().toArray((IntFunction<Object[]>)null)),
      () -> assertThrows(ArrayStoreException.class, () -> StableList.of(1).toArray(String[]::new))
    );
  }
}
