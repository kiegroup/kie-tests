package org.kie.tests.console.util;

import java.util.Properties;

public class TestConstants {
   
    /**
     * Project version (arquillian jars)
     */
    public final static String PROJECT_VERSION;
    static { 
        Properties testProps = new Properties();
        try {
            testProps.load(TestConstants.class.getResourceAsStream("/test.properties"));
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize projectVersion property: " + e.getMessage(), e);
        }
        PROJECT_VERSION = testProps.getProperty("project.version");
    }
    
}