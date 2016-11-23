package com.github.forax.pro.api.helper;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class CmdLine {
  private final ArrayList<String> arguments = new ArrayList<>();
  
  public CmdLine add(Object value) {
    Objects.requireNonNull(value);
    arguments.add(value.toString());
    return this;
  }
  
  public CmdLine add(Collection<?> collection, String separator) {
    return add(collection.stream().map(Object::toString).collect(joining(separator)));
  }
  
  public String[] toArguments() {
    return arguments.toArray(new String[0]);
  }
}
