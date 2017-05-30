/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package com.ssi.cfg;

public class TypeConversion {

    public static Object convert(Class<?> target, String source) {
        if (target.isAssignableFrom(Integer.class) || target.isAssignableFrom(int.class)) {
            return Integer.parseInt(source);
        }
        // TODO

        throw new IllegalStateException("Unsupported conversion to " + target);
    }

}
