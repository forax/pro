package com.github.forax.pro.plugin.tester;

import java.lang.module.ModuleDescriptor;
import java.util.Objects;

public class TestConf {
  private final ModuleDescriptor moduleDescriptor;
  private final boolean parallel;

  public TestConf(ModuleDescriptor moduleDescriptor, boolean parallel) {
    this.moduleDescriptor = Objects.requireNonNull(moduleDescriptor);
    this.parallel = parallel;
  }
  
  public boolean parallel() {
    return parallel;
  }
  public String moduleName() {
    return moduleDescriptor.name();
  }
  public String moduleNameAndVersion() {
    return moduleDescriptor.toNameAndVersion();
  }
}
