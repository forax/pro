package com.github.forax.pro.helper.secret;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

public class Secret {
  public static void jShellTool_main(String... arguments) {
    try {
      jShellTool_main.invokeExact(arguments);
    } catch(Throwable t) {
      throw raise(t); 
    }
  }
  
  public static ModuleDescriptor.Builder moduleDescriptor_Builder_init(String name, boolean strict) {
    try {
      return (ModuleDescriptor.Builder)moduleDescriptor_Builder_init.invokeExact(name, strict);
    } catch(Throwable t) {
      throw raise(t); 
    }
  }
  
  private static final MethodHandle jShellTool_main;
  private static final MethodHandle moduleDescriptor_Builder_init;
  
  private static RuntimeException raise(Throwable t) {
    if (t instanceof RuntimeException) {
      throw (RuntimeException)t;
    }
    if (t instanceof Error) {
      throw (Error)t;
    }
    throw new UndeclaredThrowableException(t);
  }
  
  static {
    // awful hack, please do not read the following lines or you will lost faith on the human kind
    Lookup lookup = MethodHandles.lookup();
    
    try {
      Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
      Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
      theUnsafe.setAccessible(true);
      Object unsafe = theUnsafe.get(null);
      
      Method objectFieldOffset = unsafeClass.getMethod("objectFieldOffset", Field.class);
      objectFieldOffset.setAccessible(true);
      Method putInt = unsafeClass.getMethod("putInt", Object.class, long.class, int.class);
      putInt.setAccessible(true);
      Method putObject = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);
      putInt.setAccessible(true);
      
      Field allowedModes = Lookup.class.getDeclaredField("allowedModes");
      long allowedModesOffset = (Long)objectFieldOffset.invoke(unsafe, allowedModes);
      putInt.invoke(unsafe, lookup, allowedModesOffset, -1);

      Field lookupClass = Lookup.class.getDeclaredField("lookupClass");
      long lookupClassOffset = (Long)objectFieldOffset.invoke(unsafe, lookupClass);
      putObject.invoke(unsafe, lookup, lookupClassOffset, Object.class);
    
      Class<?> jShellToolClass = Class.forName("jdk.internal.jshell.tool.JShellTool");
      jShellTool_main = lookup.findStatic(jShellToolClass, "main", methodType(void.class, String[].class));
      
      Class<?> moduleDescriptor_Builder = ModuleDescriptor.Builder.class;
      moduleDescriptor_Builder_init = lookup.findConstructor(moduleDescriptor_Builder, methodType(void.class, String.class, boolean.class));
      
    } catch(ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

}
