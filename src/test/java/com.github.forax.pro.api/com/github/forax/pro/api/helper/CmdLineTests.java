package com.github.forax.pro.api.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class CmdLineTests {
  @Test
  void toStringWithoutArguments() {
    var cmdLine = new CmdLine();
    assertEquals("", cmdLine.toString());
  }

  @Test
  void toStringWithSingleArgument() {
    var cmdLine = new CmdLine();
    cmdLine.add("one");
    assertEquals("one", cmdLine.toString());
  }

  @Test
  void toStringWithMultipleArguments() {
    var cmdLine = new CmdLine();
    cmdLine.add("one");
    cmdLine.add("two");
    cmdLine.add("three");
    assertEquals("one two three", cmdLine.toString());
  }
}
