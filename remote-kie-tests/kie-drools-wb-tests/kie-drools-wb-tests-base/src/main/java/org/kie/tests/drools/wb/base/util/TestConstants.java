package org.kie.tests.drools.wb.base.util;

import java.util.Properties;

public class TestConstants {

    
    public static final String KRIS_USER = "krisv";
    public static final String KRIS_PASSWORD = "krisv123@";
    
    public static final String MARY_USER = "mary";
    public static final String MARY_PASSWORD = "mary123@";
    public static final String USER = MARY_USER;
    public static final String PASSWORD = MARY_PASSWORD;
    
    public static final String SALA_USER = "salaboy";
    public static final String SALA_PASSWORD = "sala123@";
    public static final String JOHN_USER = "john";
    public static final String JOHN_PASSWORD = "john123@";;
    
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
