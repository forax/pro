package com.github.forax.pro.helper.parser;

import org.objectweb.asm.ModuleVisitor;

public interface ModuleClassVisitor {
  ModuleVisitor visitModule(String name, int flags, String version);
}
