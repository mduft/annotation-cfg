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

public class NestedTest {

    @Test
    public void testNested() {
        Configuration c = new Configuration();
        c.add("--testChar=x");

        TestConfig tc = c.get(TestConfig.class);
        assertEquals(tc.testChar(), 'x');
        assertEquals(tc.testConfig2().testChar(), 'x');
    }

    private @interface TestConfig {

        char testChar() default 'z';

        TestConfig2 testConfig2();

    }

    private @interface TestConfig2 {

        char testChar() default 'z';
    }

}
