package com.github.forax.pro.api.impl;

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.forax.pro.api.TypeCheckedConfig;

public final class Configs {
  private Configs() {
    throw new AssertionError();
  }
  
  public interface Query {
    public void _set_(String key, Object value);
    public void _derive_(String key, Eval eval);
    public <T> Optional<T> _get_(String key, Class<T> type, boolean readOnly);
    public Map<String, Object> _map_();
    public String _id_();
    public Class<?> _type_();
    public Object _duplicate_(EvalContext context);
  }
  
  private static Query asQuery(Object proxy) {
    if (!(proxy instanceof Query)) {
      throw new IllegalStateException("invalid proxy object " + proxy);
    }
    return (Query)proxy;
  }
  
  private static void setProperty(Object proxy, String key, Object value) {
    asQuery(proxy)._set_(key, value);
  }
  
  private static <T> Optional<T> getProperty(Object proxy, String key, Class<T> type, boolean readOnly) {
    return asQuery(proxy)._get_(key, type, readOnly);
  }
  
  @SuppressWarnings("unchecked")
  public static <T, U, V> void derive(T to, BiConsumer<? super T, ? super V> setter, U from, Function<? super U, ? extends V> eval) {
    var fromQuery = asQuery(from);
    var fromId = fromQuery._id_();
    var fromType = (Class<U>)fromQuery._type_();
    var value = Eval.of(context -> eval.apply(context.get(fromId, fromType).orElseThrow()));
    var toQuery = asQuery(to);
    var key = findKeyOf((Class<T>)toQuery._type_(), setter);
    toQuery._derive_(key, value);
  }

  private static Object traverse(Object proxy, String[] properties, int count, String key) {
    var result = proxy;
    for(var i = 0; i < count; i++) {
      var property = properties[i];
      result = getProperty(result, property, Object.class, true)
          .orElseThrow(() -> new IllegalArgumentException("property " + property + " in '" + key + "' is not defined"));
    }
    return result;
  }
  
  private static String[] splitAsProperties(String key) {
    var properties = key.split("\\.");
    if (properties.length == 0) {
      throw new IllegalArgumentException("invalid key " + key);
    }
    return properties;
  }
  
  static void set(Object proxy, String key, Object value) {
    var properties = splitAsProperties(key);
    var result = traverse(proxy, properties, properties.length - 1, key);
    setProperty(result, properties[properties.length - 1], value);
  }
  
  static <T> Optional<T> get(Object proxy, String key, Class<T> type, boolean readOnly) {
    var properties = splitAsProperties(key);
    var result = traverse(proxy, properties, properties.length - 1, key);
    return getProperty(result, properties[properties.length - 1], type, readOnly);
  }

  static Stream<String> toStringStream(String prefix, Object value, EvalContext context) {
    if (!(value instanceof Query)) {
      return Stream.of(prefix + " = " + value);
    }
    var map = ((Query)value)._map_();
    return map.entrySet()
       .stream()
       .flatMap(entry -> toStringStream(prefix.isEmpty()? entry.getKey(): prefix + "." + entry.getKey(), eval(entry.getValue(), context), context));
  }
  
  private static Object eval(Object value, EvalContext context) {
    if (value instanceof Eval) { // need to evaluate
      return ((Eval)value).eval(context);
    }
    return value;
  }
  
  static Object duplicate(Object proxy, EvalContext context) {
    return asQuery(proxy)._duplicate_(context);
  }
  
  private static Object getFromMap(EvalContext context, String id, Map<String, Object> map, Class<?> type, boolean readOnly, boolean frozen, String key) {
    Object value;
    if (type == Optional.class) {
      value = map.get(key);
      if (value == null) {
        return Optional.empty();
      }
    } else {
      value = map.computeIfAbsent(key, __ -> { // auto-vivification if possible 
        if (readOnly || !type.isAnnotationPresent(TypeCheckedConfig.class)) {
          throw new NoSuchElementException("no value for key " + key);
        }
        return proxy(type, context, id.isEmpty()? key: id + '.' + key, new HashMap<>(), /*readOnly*/ false, /*frozen*/ true);
      });
    }
    
    if (value instanceof Eval) { // need to evaluate
      value = ((Eval)value).eval(context);
    }
    
    if (type.isPrimitive()) {  //FIXME, use a wrapper type instead 
      return value;
    }
    if (type == Optional.class) {
      return Optional.of(value);
    }
    
    if (!readOnly && type.isInstance(value)) {  // avoid to create a proxy if not necessary
      return value;
    }
    // auto-wrapping, or wrap if readOnly
    if (value instanceof Query && type.isAnnotationPresent(TypeCheckedConfig.class)) {
      var query = (Query)value;
      return proxy(type, context, query._id_(), query._map_(), readOnly, frozen);
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
    var lookup = MethodHandles.lookup();
    try {
      GET_HASH = lookup.findStatic(Configs.class, "getFromMap", methodType(Object.class, EvalContext.class, String.class, Map.class, Class.class, boolean.class, boolean.class, String.class));
      SET_HASH = lookup.findStatic(Configs.class, "setFromMap", methodType(void.class, Map.class, Object.class, String.class, Class.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }
  
  private static final ClassValue<HashMap<String, MethodHandle>[]> ACCESSORS = 
      new ClassValue<>() {
        @Override
        protected HashMap<String, MethodHandle>[] computeValue(Class<?> type) {
          var getterMap = new HashMap<String, MethodHandle>();
          var setterMap = new HashMap<String, MethodHandle>();
          for(var method: type.getMethods()) {
            var declaringClass = method.getDeclaringClass();
            if (declaringClass == Object.class) {
              continue;
            }
            //FIXME support default method
            
            var name = method.getName();
            switch(method.getParameterCount()) {
            case 0: {
              var mh = MethodHandles.insertArguments(GET_HASH, 6, name);
              getterMap.put(name, mh);
              continue;
            }
            case 1: {
              if (method.isVarArgs()) {  // skip varags methods as they will hide the real implementation
                continue;
              }
              var mh = MethodHandles.insertArguments(SET_HASH, 2, name, method.getParameterTypes()[0]);
              setterMap.put(name, mh);
              continue;
            }
            default:
              throw new IllegalStateException("invalid method " + method + " on " + declaringClass.getName());
            }
          }
          @SuppressWarnings("unchecked")
          var accessors =
            (HashMap<String, MethodHandle>[])new HashMap<?,?>[] { getterMap, setterMap};
          return accessors;
        }
      };
  
      
  public interface EvalContext {
    <T> Optional<T> get(String key, Class<T> type);
  }
  
  @FunctionalInterface
  private interface Eval {
    Object eval(EvalContext context);
  
    static Eval of(Eval eval) {
      return eval;
    }
  }
      
  
  
  @TypeCheckedConfig
  private interface Group {
    // empty
  }
  
  static Object newRoot(EvalContext context) {
    return proxy(Group.class, context, "", new HashMap<>(), /*readOnly*/ false, /*frozen*/ false);
  }
  
  private static <T> T proxy(Class<T> proxyClass, EvalContext context, String id, Map<String, Object> map,
      boolean proxyReadOnly, boolean proxyFrozen) {
    if (!proxyClass.isAnnotationPresent(TypeCheckedConfig.class)) {
      throw new IllegalArgumentException("can only proxy interface (" + proxyClass.getSimpleName() + ") tagged with @TypeCheckedConfig");
    }
    Configs.class.getModule().addReads(proxyClass.getModule());
    
    var accessors = ACCESSORS.get(proxyClass);
    var getterMap = accessors[GETTER];
    var setterMap = accessors[SETTER];
    return proxyClass.cast(Proxy.newProxyInstance(proxyClass.getClassLoader(),
        new Class<?>[] { proxyClass, Query.class },
        (Object proxy, Method method, Object[] args) -> {
          Class<?> declaringClass = method.getDeclaringClass();
          var name = method.getName();
          if (declaringClass == Query.class) {
            switch(name) {
            case "_get_": {
              var key = (String)args[0];
              var type = (Class<?>)args[1];
              var readOnly = (Boolean)args[2];
              var mh = getterMap.get(key);
              Object result;
              if (mh == null) {
                result = getFromMap(context, id, map, type, proxyReadOnly | readOnly, proxyFrozen, key);
              } else {
                result = mh.invokeExact(context, id, map, type, proxyReadOnly | readOnly, proxyFrozen);
              }
              if (!(result instanceof Optional<?>)) {
                return Optional.of(result);
              }
              return result;
            }
            case "_set_": {
              var key = (String)args[0];
              var value = args[1];
              if (proxyReadOnly) {
                throw readOnly(proxyClass, key);
              }
              var mh = setterMap.get(key);
              if (mh == null) {
                if (proxyFrozen) {
                  throw frozen(proxyClass, key);
                }
                setFromMap(map, value, key,  Object.class);
              } else {
                mh.invokeExact(map, value);
              }
              return null;
            }
            case "_derive_": {
              var key = (String)args[0];
              var eval = (Eval)args[1];
              if (proxyReadOnly) {
                throw readOnly(proxyClass, key);
              }
              if (proxyFrozen && !setterMap.containsKey(key)) {
                throw frozen(proxyClass, key);
              }
              setFromMap(map, eval, key,  Object.class);
              return null;
            }
            case "_map_":
              return map;
            case "_id_":
              return id;
            case "_type_":
              return proxyClass;
            case "_duplicate_": {
              var ctx = (EvalContext)args[0];
              var newMap = new HashMap<String, Object>();
              map.forEach((key, value) -> {
                Object newValue = (value instanceof Query)?((Query)value)._duplicate_(ctx): value;
                newMap.put(key, newValue);
              });
              return proxy(proxyClass, ctx, id, newMap, proxyReadOnly, proxyFrozen);
            }
              
            default:
            }
          } else {
            if (declaringClass == Object.class) {
              switch(name) {
              case "toString":
                return toStringStream("", proxy, context).collect(Collectors.joining("\n", "{\n", "\n}"));
              case "equals":
                return proxy == args[1];
              case "hashCode":
                return System.identityHashCode(proxy);
              default:
              }
            } else {
            	var parameterCount = method.getParameterCount();
            	if (parameterCount == 0) {
            		return getterMap.get(name).invokeExact(context, id, map, method.getReturnType(), proxyReadOnly, proxyFrozen);
            	}
            	if (parameterCount == 1) {
            		if (proxyReadOnly) {
            			throw readOnly(proxyClass, method.getName());
            		}
            		var value = method.isVarArgs()? List.of((Object[])args[0]): args[0];
            		setterMap.get(name).invokeExact(map, value);
            		return proxy; // acts as a builder;
            	}
            }
          }
          throw new IllegalStateException("invalid method " + method + " on " + declaringClass.getName());
        }));
  }

  
  
  private static <T> String findKeyOf(Class<T> type, BiConsumer<? super T, ?> setter) {
    var builder = new StringBuilder();
    Configs.class.getModule().addReads(type.getModule());
    T proxy = type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
        (proxyObject, method, args) -> {
          builder.append(method.getName());
          return null;
        }));
    setter.accept(proxy, null);   //FIXME, will not work with primitive types :(
    return builder.toString();
  }
  
  private static UnsupportedOperationException readOnly(Class<?> proxyClass, String key) {
    return new UnsupportedOperationException(proxyClass.getSimpleName() + "." + key +": configuration is read only");
  }
  
  private static UnsupportedOperationException frozen(Class<?> proxyClass, String key) {
    return new UnsupportedOperationException(proxyClass.getSimpleName() + "." + key +": unknown key");
  }
}
