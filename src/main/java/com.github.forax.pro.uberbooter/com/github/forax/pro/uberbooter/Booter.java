package com.github.forax.pro.uberbooter;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Set;

public class Booter {
  private static final StackWalker STACK_WALKER = StackWalker.getInstance(Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE));

  public static void main(Class<?> mainClass, String[] args) throws NoSuchMethodException, IllegalAccessException {
    if (!STACK_WALKER.getCallerClass().getName().equals("com.github.forax.pro.ubermain.Main")) {
      throw new SecurityException("caller is not permitted to call this method");
    }
    
    var lookup = MethodHandles.lookup();
    try {
      Booter.class.getModule().addReads(mainClass.getModule());
      var mh = lookup.findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class));
      mh.invokeExact(args);
    } catch(NoSuchMethodException | IllegalAccessException e) {
      throw e;
    } catch(RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      throw new UndeclaredThrowableException(e);
    }
  }
}
