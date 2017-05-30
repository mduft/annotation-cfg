/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package com.ssi.cfg.tests;

import org.junit.Test;

import com.ssi.cfg.Configuration;

public class ConversionTest {

    @Test
    public void testConversion() {
        Configuration c = new Configuration();

    }

    private @interface TestConfig {

        char testChar() default 'z';
    }

}
