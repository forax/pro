package com.github.forax.pro.api.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class CmdLineTests {
  @Test
  public void toStringWithoutArguments() {
    CmdLine cmdLine = new CmdLine();
    assertEquals("", cmdLine.toString());
  }

  @Test
  public void toStringWithSingleArgument() {
    CmdLine cmdLine = new CmdLine();
    cmdLine.add("one");
    assertEquals("one", cmdLine.toString());
  }

  @Test
  public void toStringWithMultipleArguments() {
    CmdLine cmdLine = new CmdLine();
    cmdLine.add("one");
    cmdLine.add("two");
    cmdLine.add("three");
    assertEquals("one two three", cmdLine.toString());
  }
}
