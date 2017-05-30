/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package com.ssi.cfg;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

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
        if (!objects.containsKey(method.getName())) {
            return method.getDefaultValue();
        }

        return doConvert(method, objects.get(method.getName()));
    }

    private Object doConvert(Method method, Object object) {
        Class<?> returnType = method.getReturnType();
        if (returnType.isAssignableFrom(object.getClass())) {
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
        if (!(object instanceof String)) {
            throw new IllegalStateException(
                    "Illegal conversion from non-string object to different type: " + object.getClass() + " to " + returnType);
        }

        // do actual conversion
        conversion = TypeConversion.convert(returnType, (String) object);

        // remember the result in the mapping, so we don't need to convert back and forth all the time.
        conversions.put(method, conversion);

        return conversion;
    }
}
