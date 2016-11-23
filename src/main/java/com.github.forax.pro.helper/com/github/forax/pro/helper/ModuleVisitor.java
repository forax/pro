package com.github.forax.pro.helper;

import java.util.List;

public interface ModuleVisitor {
  int ACC_OPEN = 0x0020;
  int ACC_TRANSITIVE = 0x0020;
  int ACC_STATIC = 0x0040;
  
  public void visitModule(int modifiers, String name);
  public void visitRequires(int modifiers, String module);
  public void visitExports(String packaze, List<String> toModules);
  public void visitOpens(String packaze, List<String> toModules);
  public void visitUses(String service);
  public void visitProvides(String service, List<String> providers);
}