package com.github.forax.pro.api.helper;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collection;

public class CmdLine {
  private final ArrayList<String> arguments = new ArrayList<>();
  
  public CmdLine add(Object value) {
    arguments.add(value.toString());  // implicit nullcheck
    return this;
  }
  
  public CmdLine addAll(Object... values) {
    for(Object value: values) {
      add(value);
    }
    return this;
  }
  
  public CmdLine add(Collection<?> collection, String separator) {
    return add(collection.stream().map(Object::toString).collect(joining(separator)));
  }
  
  public String[] toArguments() {
    return arguments.toArray(new String[0]);
  }
  
  @Override
  public String toString() {
    return arguments.stream().collect(joining(" "));
  }
}
