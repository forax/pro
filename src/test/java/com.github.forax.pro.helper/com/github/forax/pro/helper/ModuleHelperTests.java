package com.github.forax.pro.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.module.ModuleDescriptor;
import java.util.Set;

@SuppressWarnings("static-method")
class ModuleHelperTests {
  @Test
  void mergeModules() {
    var sourceModule = ModuleDescriptor.newModule("a.b.c")
            .requires(Set.of(ModuleDescriptor.Requires.Modifier.TRANSITIVE), "a.b.d")
            .build();
    var testerModule = ModuleDescriptor.newModule("a.b.c")
            .requires("org.junit.jupiter.api")
            .build();
    var mergedModule = ModuleHelper.mergeModuleDescriptor(sourceModule, testerModule);
    Assertions.assertEquals("a.b.c", mergedModule.name());
    Assertions.assertTrue(mergedModule.requires().toString().contains("transitive a.b.d"));
    Assertions.assertTrue(ModuleHelper.moduleDescriptorToSource(mergedModule).contains("requires transitive a.b.d;"));
  }
}
