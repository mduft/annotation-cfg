/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package com.ssi.cfg.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ssi.cfg.Configuration;

public class BasicTest {

    @Test
    public void testBasic() {
        Configuration c = new Configuration();
        c.add("--booleanArg", "--intArg=3");

        TestConfig tc = c.get(TestConfig.class);
        assertTrue(tc.booleanArg());
        assertEquals(tc.intArg(), 3);
    }

    @Test
    public void testDefault() {
        Configuration c = new Configuration();

        TestConfig tc = c.get(TestConfig.class);
        assertTrue(!tc.booleanArg());
        assertEquals(tc.intArg(), 1);
    }

    private @interface TestConfig {

        boolean booleanArg() default false;

        int intArg() default 1;
    }

}
