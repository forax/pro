package com.github.forax.pro.api.impl;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.forax.pro.api.TypeCheckedConfig;

public class Configs {
  private Configs() {
    throw new AssertionError();
  }
  
  /*package private*/ interface Query {
    public void _set_(String key, Object value);
    public <T> Optional<T> _get_(String key, Class<T> type, boolean readOnly);
    public Map<String, Object> _map_();
    public Object _duplicate_();
  }
  
  private static void setProperty(Object proxy, String key, Object value) {
    if (!(proxy instanceof Query)) {
      throw new IllegalStateException("invalid proxy object " + proxy);
    }
    ((Query)proxy)._set_(key, value);
  }
  
  private static <T> Optional<T> getProperty(Object proxy, String key, Class<T> type, boolean readOnly) {
    if (!(proxy instanceof Query)) {
      throw new IllegalStateException("invalid proxy object " + proxy);
    }
    return ((Query)proxy)._get_(key, type, readOnly);
  }
  
  private static void forEachProperty(Object proxy, BiConsumer<String, Object> consumer) {
    if (!(proxy instanceof Query)) {
      throw new IllegalStateException("invalid proxy object " + proxy);
    }
    ((Query)proxy)._map_().forEach(consumer);
  }
  
  private static Object traverse(Object proxy, String[] properties, int count, String key) {
    Object result = proxy;
    for(int i = 0; i < count; i++) {
      String property = properties[i];
      result = getProperty(result, property, Object.class, true)
          .orElseThrow(() -> new IllegalArgumentException("property " + property + " in '" + key + "' is not defined"));
    }
    return result;
  }
  
  static void set(Object proxy, String key, Object value) {
    String[] properties = key.split("\\.");
    if (properties.length == 0) {
      throw new IllegalArgumentException("invalid key " + key);
    }
    Object result = traverse(proxy, properties, properties.length - 1, key);
    setProperty(result, properties[properties.length - 1], value);
  }
  
  static <T> Optional<T> get(Object proxy, String key, Class<T> type, boolean readOnly) {
    String[] properties = key.split("\\.");
    if (properties.length == 0) {
      throw new IllegalArgumentException("invalid key " + key);
    }
    Object result = traverse(proxy, properties, properties.length - 1, key);
    return getProperty(result, properties[properties.length - 1], type, readOnly);
  }
  
  static void forEach(Object proxy, String key, BiConsumer<String, Object> consumer) {
    String[] properties = key.split("\\.");
    if (properties.length == 0) {
      throw new IllegalArgumentException("invalid key " + key);
    }
    Object result = traverse(proxy, properties, properties.length, key);
    forEachProperty(result, consumer);
  }

  static Stream<String> toStringStream(String prefix, Object value) {
    if (!(value instanceof Query)) {
      return Stream.of(prefix + " = " + value);
    }
    Map<String, Object> map = ((Query)value)._map_();
    return map.entrySet()
       .stream()
       .flatMap(entry -> toStringStream(prefix.isEmpty()? entry.getKey(): prefix + "." + entry.getKey(), entry.getValue()));
  }
  
  static Object duplicate(Object proxy) {
    if (!(proxy instanceof Query)) {
      throw new IllegalStateException("invalid proxy object " + proxy);
    }
    return ((Query)proxy)._duplicate_();
  }
  
  private static Object getFromMap(Map<String, Object> map, Class<?> type, boolean readOnly, String key) {
    if (type == Optional.class) {
      return Optional.ofNullable(map.get(key));
    }
    Object value = map.computeIfAbsent(key, __ -> { // auto-vivification if possible 
      if (readOnly || !type.isAnnotationPresent(TypeCheckedConfig.class)) {
        throw new IllegalStateException("no value for key " + key);
      }
      return proxy(type, new HashMap<>(), false);
    });
    
    if (type.isPrimitive()) {  //FIXME, use a wrapper type instead 
      return value;
    }
    if (!readOnly && type.isInstance(value)) {  // avoid to create a proxy if not necessary
      return value;
    }
    // auto-wrapping, or wrap if readOnly
    if (value instanceof Query && type.isAnnotationPresent(TypeCheckedConfig.class)) {
      Query query = (Query)value;
      Map<String, Object> backingMap = query._map_();
      return proxy(type, backingMap, readOnly);
    }
    return type.cast(value);
  }
  
  private static void setFromMap(Map<String, Object> map, Object value, String key, Class<?> type) {
    Objects.requireNonNull(value);
    
    if (!(type.isPrimitive())) {  //FIXME, use a wrapper type instead
      type.cast(value);
      //checkTypeImmutable(value.getClass());  //TODO ??
    }
    map.put(key, value);
  }
  
  private static final int GETTER = 0, SETTER = 1;
  static final MethodHandle GET_HASH, SET_HASH;
  static {
    Lookup lookup = MethodHandles.lookup();
    try {
      GET_HASH = lookup.findStatic(Configs.class, "getFromMap", methodType(Object.class, Map.class, Class.class, boolean.class, String.class));
      SET_HASH = lookup.findStatic(Configs.class, "setFromMap", methodType(void.class, Map.class, Object.class, String.class, Class.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }
  
  private static final ClassValue<HashMap<String, MethodHandle>[]> ACCESSORS = 
      new ClassValue<>() {
        @Override
        protected HashMap<String, MethodHandle>[] computeValue(Class<?> type) {
          HashMap<String, MethodHandle> getterMap = new HashMap<>();
          HashMap<String, MethodHandle> setterMap = new HashMap<>();
          for(Method method: type.getMethods()) {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass == Object.class) {
              continue;
            }
            //FIXME support default method
            
            String name = method.getName();
            switch(method.getParameterCount()) {
            case 0: {
              MethodHandle mh = MethodHandles.insertArguments(GET_HASH, 3, name);
              getterMap.put(name, mh);
              continue;
            }
            case 1: {
              MethodHandle mh = MethodHandles.insertArguments(SET_HASH, 2, name, method.getParameterTypes()[0]);
              setterMap.put(name, mh);
              continue;
            }
            default:
              throw new IllegalStateException("invalid method " + method + " on " + declaringClass.getName());
            }
          }
          @SuppressWarnings("unchecked")
          HashMap<String, MethodHandle>[] accessors =
            (HashMap<String, MethodHandle>[])new HashMap<?,?>[] { getterMap, setterMap};
          return accessors;
        }
      };
  
  @TypeCheckedConfig
  private interface Group {
    // empty
  }
  
  
  public static Object newRoot() {
    return proxy(Group.class, new HashMap<>(), false);
  }
  
  private static <T> T proxy(Class<T> proxyClass, Map<String, Object> map, boolean proxyReadOnly) {
    if (!proxyClass.isAnnotationPresent(TypeCheckedConfig.class)) {
      throw new IllegalArgumentException("can only proxy interface (" + proxyClass.getSimpleName() + ") tagged with @TypeCheckedConfig");
    }
    Configs.class.getModule().addReads(proxyClass.getModule());
    
    HashMap<String, MethodHandle>[] accessors = ACCESSORS.get(proxyClass);
    HashMap<String, MethodHandle> getterMap = accessors[GETTER];
    HashMap<String, MethodHandle> setterMap = accessors[SETTER];
    return proxyClass.cast(Proxy.newProxyInstance(proxyClass.getClassLoader(),
        new Class<?>[] { proxyClass, Query.class },
        (Object proxy, Method method, Object[] args) -> {
          Class<?> declaringClass = method.getDeclaringClass();
          String name = method.getName();
          if (declaringClass == Query.class) {
            switch(name) {
            case "_get_": {
              String key = (String)args[0];
              Class<?> type = (Class<?>)args[1];
              boolean readOnly = (Boolean)args[2];
              MethodHandle mh = getterMap.get(key);
              Object result;
              if (mh == null) {
                result = getFromMap(map, type, proxyReadOnly | readOnly, key);
              } else {
                result = mh.invokeExact(map, type, proxyReadOnly | readOnly);
              }
              if (!(result instanceof Optional<?>)) {
                return Optional.of(result);
              }
              return result;
            }
            case "_set_": {
              if (proxyReadOnly) {
                throw new UnsupportedOperationException("configuration is read only for type " + proxyClass.getSimpleName());
              }
              String key = (String)args[0];
              Object value = args[1];
              MethodHandle mh = setterMap.get(key);
              if (mh == null) {
                setFromMap(map, value, key,  Object.class);
                return null;
              }
              mh.invokeExact(map, value);
              return null;
            }
            case "_map_":
              return map;
            case "_duplicate_": {
              HashMap<String, Object> newMap = new HashMap<>();
              map.forEach((key, value) -> {
                Object newValue = (value instanceof Query)?((Query)value)._duplicate_(): value;
                newMap.put(key, newValue);
              });
              return proxy(proxyClass, newMap, proxyReadOnly);
            }
              
            default:
            }
          } else {
            if (declaringClass == Object.class) {
              switch(name) {
              case "toString":
                return toStringStream("", proxy).collect(Collectors.joining("\n", "{\n", "\n}"));
              case "equals":
                return proxy == args[1];
              case "hashCode":
                return System.identityHashCode(proxy);
              default:
              }
            } else {
              int parameterCount = method.getParameterCount();
              if (parameterCount == 0) {
                return getterMap.get(name).invokeExact(map, method.getReturnType(), proxyReadOnly);
              }
              if (parameterCount == 1) {
                if (proxyReadOnly) {
                  throw new UnsupportedOperationException("configuration is read only");
                }
                Object value = args[0];
                setterMap.get(name).invokeExact(map, value);
                return null;
              }
            }
          }
          throw new IllegalStateException("invalid method " + method + " on " + declaringClass.getName());
        }));
  }
}
