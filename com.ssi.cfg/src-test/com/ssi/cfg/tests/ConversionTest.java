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
package com.ssi.cfg.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ssi.cfg.Configuration;

public class ConversionTest {

    @Test
    public void testConversion() {
        Configuration c = new Configuration();

        c.add("--testByte=3", "--testChar=a", "--testInt=9", "--testShort=8", "--testLong=7", "--testFloat=0.9",
                "--testDouble=0.9", "--testEnum=TEST1", "--testString=abc", "--testStringArray=abc,def,ghi",
                "--testLongArray=1,2,3,4,5", "--testBoolean");

        TestConfig tc = c.get(TestConfig.class);
        assertEquals(tc.testByte(), 3);
        assertEquals(tc.testChar(), 'a');
        assertEquals(tc.testInt(), 9);
        assertEquals(tc.testShort(), 8);
        assertEquals(tc.testLong(), 7);
        assertEquals(tc.testFloat(), 0.9f, 0.0);
        assertEquals(tc.testDouble(), 0.9, 0.0);
        assertEquals(tc.testEnum(), TestEnum.TEST1);
        assertEquals(tc.testString(), "abc");
        assertEquals(tc.testStringArray().length, 3);
        assertEquals(tc.testStringArray()[0], "abc");
        assertEquals(tc.testStringArray()[1], "def");
        assertEquals(tc.testStringArray()[2], "ghi");
        assertEquals(tc.testLongArray().length, 5);
        assertEquals(tc.testLongArray()[0], 1);
        assertEquals(tc.testLongArray()[1], 2);
        assertEquals(tc.testLongArray()[2], 3);
        assertEquals(tc.testLongArray()[3], 4);
        assertEquals(tc.testLongArray()[4], 5);
    }
    
    @Test
    public void testMultiAdd() {
    	Configuration c = new Configuration();
    	c.add("--testStringArray=1");
    	c.add("--testStringArray=2");
    	c.add("--testStringArray=3");
    	c.add("--testStringArray=4");
    	
    	TestConfig tc = c.get(TestConfig.class);
    	
    	assertEquals(tc.testStringArray().length, 4);
    	assertEquals(tc.testStringArray()[0], "1");
        assertEquals(tc.testStringArray()[1], "2");
        assertEquals(tc.testStringArray()[2], "3");
        assertEquals(tc.testStringArray()[3], "4");
    }

    private @interface TestConfig {

        byte testByte() default 0x1;

        char testChar() default 'z';

        int testInt() default 2;

        short testShort() default 3;

        long testLong() default 4;

        float testFloat() default 0.5f;

        double testDouble() default 0.55555;

        boolean testBoolean() default false;

        TestEnum testEnum() default TestEnum.TEST2;

        String testString() default "Test";

        String[] testStringArray() default {};

        long[] testLongArray() default {};
    }

    public enum TestEnum {
        TEST1,
        TEST2
    }

}
