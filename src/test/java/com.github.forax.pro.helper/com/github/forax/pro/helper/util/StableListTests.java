package com.github.forax.pro.helper.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class StableListTests {

  @Test
  void append() {
    StableList<String> list1 = new StableList<>();
    StableList<String> list2 = list1.append("foo").append("bar");
    StableList<String> list3 = list2.append("baz");
    StableList<String> list4 = list2.append("bang");
    assertAll("append assertions",
        () -> assertEquals("[]", list1.toString()),
        () -> assertEquals("[foo, bar]", list2.toString()),
        () -> assertEquals("[foo, bar, baz]", list3.toString()),
        () -> assertEquals("[foo, bar, bang]", list4.toString())
    );
  }
}
