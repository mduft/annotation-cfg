/*
 * Copyright (c) SSI Schaefer IT Solutions
 */
package com.ssi.cfg.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ssi.cfg.Configuration;
import com.ssi.cfg.Configuration.ConfigurationNameMapping;

public class PropertyTest {

    @Test
    public void testProperties() {
        String value = System.getProperty("user.home");

        Configuration c = new Configuration();
        c.add(System.getProperties());

        TestConfig tc = c.get(TestConfig.class);

        assertEquals(tc.userHome(), value);
    }

    private @interface TestConfig {

        @ConfigurationNameMapping("user.home")
        String userHome();
    }

}
