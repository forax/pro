package com.github.forax.pro.api;

import java.io.IOException;

public interface Plugin {
  public String name();
  public void init(MutableConfig config);
  public void configure(MutableConfig config);
  public int execute(Config config) throws IOException;
}
