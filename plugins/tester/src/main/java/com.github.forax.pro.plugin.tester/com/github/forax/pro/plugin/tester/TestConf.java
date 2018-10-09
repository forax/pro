package com.github.forax.pro.plugin.tester;

import java.lang.module.ModuleDescriptor;
import java.util.List;
import java.util.Objects;

public class TestConf {
  private final ModuleDescriptor moduleDescriptor;
  private final boolean parallel;
  private final List<String> includeTags;
  private final List<String> excludeTags;
  private final List<String> packages;

  public TestConf(ModuleDescriptor moduleDescriptor, boolean parallel, List<String> packages, List<String> includeTags, List<String> excludeTags) {
    this.moduleDescriptor = Objects.requireNonNull(moduleDescriptor);
    this.parallel = parallel;
    this.includeTags = includeTags;
    this.excludeTags = excludeTags;
    this.packages = packages;
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
  public List<String> includeTags() {
    return includeTags;
  }
  public List<String> excludeTags() {
    return excludeTags;
  }
  public List<String> packages() {
    return packages;
  }
}
