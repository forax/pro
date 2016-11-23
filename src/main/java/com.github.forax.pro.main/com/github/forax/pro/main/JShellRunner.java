package com.github.forax.pro.main;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Path;

import com.github.forax.pro.helper.secret.Secret;
import com.github.forax.pro.main.runner.Runner;

public class JShellRunner implements Runner {
  @Override
  public boolean accept(Path config) {
    return config.toString().endsWith(".pro");
  }
  
  @Override
  public void run(Path configFile) {
    //System.out.println("run with jshell " + configFile);
    
    // awful hack, please do not read the following lines or you will lost your faith on the human kind
    /*Lookup lookup = MethodHandles.lookup();
    
    try {
      Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafe.setAccessible(true);
      sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);

      Field allowedModes = Lookup.class.getDeclaredField("allowedModes");
      long allowedModesOffset = unsafe.objectFieldOffset(allowedModes);
      unsafe.putInt(lookup, allowedModesOffset, -1);

      Field lookupClass = Lookup.class.getDeclaredField("lookupClass");
      long lookupClassOffset = unsafe.objectFieldOffset(lookupClass);
      unsafe.putObject(lookup, lookupClassOffset, Object.class);

      Class<?> jShellToolClass = Class.forName("jdk.internal.jshell.tool.JShellTool");
      MethodHandle mh = lookup.findStatic(jShellToolClass, "main", MethodType.methodType(void.class, String[].class));
      mh.invokeExact(new String[] { configFile.toString() });
    } catch(Throwable t) {
      if (t instanceof RuntimeException) {
        throw (RuntimeException)t;
      }
      if (t instanceof Error) {
        throw (Error)t;
      }
      throw new UndeclaredThrowableException(t);
    }*/
    
    Secret.jShellTool_main(new String[] { configFile.toString() });
  }
}
