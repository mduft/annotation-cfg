/*******************************************************************************
 *  Copyright (c) 2017 SSI Schaefer IT Solutions GmbH and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package com.ssi.cfg;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.google.common.base.Splitter;

/**
 * The {@link Configuration} is basically a wrapper around a {@link Map} which exposes access to the {@link Map} through
 * {@link Annotation}s.
 * <p>
 * Any arbitrary {@link Annotation} can be defined, including default values, and mapped to the {@link Configuration} using
 * {@link #get(Class)}.
 * <p>
 * The mapped {@link Annotation} will access the underlying {@link Map} on every method call. If a key exists in the {@link Map}
 * that corresponds to the name of the {@link Annotation}'s {@link Method}, it will be converted to the target type and returned.
 * Otherwise the default value of the {@link Method} is returned.
 * <p>
 * It is possible to put arbitrary {@link Object}s into the {@link Map} using {@link #add(Map)}. There is no validation on the
 * types passed, but they are in reality restricted to types that are valid return types for {@link Annotation} {@link Method}s.
 * Using any other types will result in an {@link Exception}.
 * <p>
 * There is (limited) type conversion capabilities. Mainly this functionality exists to be able to map {@link String}s (e.g. when
 * mapping a command line using {@link #add(String...)}) to the target types of the {@link Annotation} {@link Method}s.
 */
public class Configuration {

    private final Map<String, Object> objects = new TreeMap<>();
    private final Map<Method, Object> conversions = new HashMap<>();

    /**
     * Add a set of command line arguments to the mapping. Arguments must currently start with '--'.
     *
     * @param arguments the command line argument as passed to the program.
     */
    @SuppressWarnings("unchecked")
	public void add(String... arguments) {
        for (String arg : arguments) {
            if (arg.startsWith("--")) {
                String stripped = arg.substring(2);
                int equalsIndex = stripped.indexOf('=');
                if (equalsIndex != -1) {
                    String key = stripped.substring(0, equalsIndex);
                    String value = stripped.substring(equalsIndex + 1);
                    
                    if(objects.containsKey(key)) {
                    	Object existing = objects.get(key);
                    	if(existing instanceof List) {
                    		((List<Object>)existing).add(value);
                    	} else {
                    		List<Object> l = new ArrayList<>();
                    		l.add(existing);
                    		l.add(value);
                    		objects.put(key, l);
                    	}
                    } else {
                    	objects.put(key, value);
                    }
                } else {
                    objects.put(stripped, Boolean.TRUE);
                }
            } else {
                // unsupported right now
                throw new IllegalStateException("Unsupported argument format: " + arg);
            }
        }
    }

    /**
     * Adds arbitrary entries to the mapping. See the class documentation for more information on supported types and implicit
     * conversions.
     *
     * @param arguments the entries to add to the mapping.
     */
    public void add(Map<String, ?> arguments) {
        objects.putAll(arguments);
    }

    /**
     * Adds arbitrary ({@link String}) properties to the mapping.
     * <p>
     * Typically used to add a configuration file or system properties to the mapping
     *
     * @param properties the entries to add to the mapping.
     */
    public void add(Properties properties) {
        properties.forEach((k, v) -> objects.put((String) k, v));
    }

    /**
     * Returns an instance of the given {@link Annotation}, mapping each {@link Method} to a value in the mapping where the
     * {@link Method} name is the key into the wrapped {@link Map}.
     *
     * @param target the {@link Class} to map this {@link Configuration} to.
     * @return a proxy mapping to the {@link Configuration}.
     */
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T get(Class<T> target) {
        return (T) Proxy.newProxyInstance(target.getClassLoader(), new Class[] { target }, this::doMap);
    }

    private Object doMap(Object proxy, Method method, Object[] arguments) {
        String key = method.getName();
        ConfigurationNameMapping mapping = method.getAnnotation(ConfigurationNameMapping.class);
        if (mapping != null) {
            key = mapping.value();
        }

        if (!objects.containsKey(key) && !method.getReturnType().isAnnotation()) {
            return method.getDefaultValue();
        }

        return doConvert(method, objects.get(key));
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
        
        if(object instanceof List && returnType.isArray()) {
        	List<?> list = (List<?>)object;
        	// perform conversion for each of the elements.
        	Object targetArray = Array.newInstance(returnType.getComponentType(), list.size());
            for (int i = 0; i < list.size(); ++i) {
                Array.set(targetArray, i, convertType(returnType.getComponentType(), (String)list.get(i)));
            }
            conversions.put(method, targetArray);
            return targetArray;
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
    private Object convertType(Class<?> target, String source) {
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
            Object targetArray = Array.newInstance(target.getComponentType(), split.size());
            for (int i = 0; i < split.size(); ++i) {
                Array.set(targetArray, i, convertType(target.getComponentType(), split.get(i)));
            }
            return targetArray;
        } else if (target.isAnnotation()) {
            return get((Class<? extends Annotation>) target);
        }

        throw new IllegalStateException("Unsupported conversion to " + target);
    }

    /**
     * Maps the annotated method to another property name in the context.
     * <p>
     * This can be used to map an arbitrary {@link Annotation} {@link Method} to another name, e.g. to access system properties
     * with property names that are not valid method names in Java.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ConfigurationNameMapping {

        String value();
    }
}
