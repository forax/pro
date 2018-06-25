package com.github.forax.pro.plugin.tester;

import java.lang.module.ModuleDescriptor;

public class TesterFixture {
  final ModuleDescriptor moduleDescriptor;
  final boolean parallel;

  public TesterFixture(ModuleDescriptor moduleDescriptor, boolean parallel) {
    this.moduleDescriptor = moduleDescriptor;
    this.parallel = parallel;
  }
}
