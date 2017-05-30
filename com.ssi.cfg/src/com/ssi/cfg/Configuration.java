/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package com.ssi.cfg;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.google.common.base.Splitter;

public class Configuration {

    private final Map<String, Object> objects = new TreeMap<>();
    private final Map<Method, Object> conversions = new HashMap<>();

    public void add(String... arguments) {
        for (String arg : arguments) {
            if (arg.startsWith("--")) {
                String stripped = arg.substring(2);
                int equalsIndex = stripped.indexOf('=');
                if (equalsIndex != -1) {
                    String key = stripped.substring(0, equalsIndex);
                    String value = stripped.substring(equalsIndex + 1);

                    objects.put(key, value);
                } else {
                    objects.put(stripped, Boolean.TRUE);
                }
            } else {
                // unsupported right now
                throw new IllegalStateException("Unsupported argument format: " + arg);
            }
        }
    }

    public void add(Map<String, ?> arguments) {
        objects.putAll(arguments);
    }

    public void add(Properties properties) {
        properties.forEach((k, v) -> objects.put((String) k, v));
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T get(Class<T> target) {
        return (T) Proxy.newProxyInstance(target.getClassLoader(), new Class[] { target }, this::doMap);
    }

    private Object doMap(Object proxy, Method method, Object[] arguments) {
        if (!objects.containsKey(method.getName()) && !method.getReturnType().isAnnotation()) {
            return method.getDefaultValue();
        }

        return doConvert(method, objects.get(method.getName()));
    }

    private Object doConvert(Method method, Object object) {
        Class<?> returnType = method.getReturnType();
        if (object != null && returnType.isAssignableFrom(object.getClass())) {
            return object;
        }

        // lookup existing conversion
        Object conversion = conversions.get(method);
        if (conversion != null) {
            return conversion;
        }

        if (returnType.isPrimitive() && !(object instanceof String)) {
            // implicit conversion through boxing/unboxing. let's just hope the types match ;)
            return object;
        }

        // check source type
        if (!(object instanceof String) && !returnType.isAnnotation()) {
            throw new IllegalStateException(
                    "Illegal conversion from non-string object to different type: " + object.getClass() + " to " + returnType);
        }

        // do actual conversion
        conversion = convertType(returnType, (String) object);

        // remember the result in the mapping, so we don't need to convert back and forth all the time.
        conversions.put(method, conversion);

        return conversion;
    }

    @SuppressWarnings("unchecked")
    public Object convertType(Class<?> target, String source) {
        if (target.equals(String.class)) {
            return source;
        } else if (target.equals(long.class)) {
            return Long.parseLong(source);
        } else if (target.equals(int.class)) {
            return Integer.parseInt(source);
        } else if (target.equals(short.class)) {
            return Short.parseShort(source);
        } else if (target.equals(byte.class)) {
            return Byte.parseByte(source);
        } else if (target.equals(boolean.class)) {
            return Boolean.parseBoolean(source);
        } else if (target.equals(double.class)) {
            return Double.parseDouble(source);
        } else if (target.equals(float.class)) {
            return Float.parseFloat(source);
        } else if (target.equals(char.class)) {
            if (source.length() > 1) {
                throw new IllegalArgumentException("Character conversion with input length > 1: " + source);
            }
            return source.charAt(0);
        } else if (target.isEnum()) {
            try {
                return target.getMethod("valueOf", String.class).invoke(null, source);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(
                        "internal error resolving enumeration literal for " + target + " '" + source + "'", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getTargetException());
            }
        } else if (target.isArray()) {
            List<String> split = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(source);
            Object targetArray = Array.newInstance(target, split.size());
            for (int i = 0; i < split.size(); ++i) {
                Array.set(targetArray, i, convertType(target.getComponentType(), split.get(i)));
            }
            return targetArray;
        } else if (target.isAnnotation()) {
            return get((Class<? extends Annotation>) target);
        }

        throw new IllegalStateException("Unsupported conversion to " + target);
    }
}
