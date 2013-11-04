package org.kie.tests.wb.base.methods;

import java.util.Properties;

import org.jbpm.kie.services.impl.KModuleDeploymentUnit;

public class TestConstants {
    
    public static final String USER = "mary";
    public static final String PASSWORD = "mary123@";
    public static final String SALA_USER = "salaboy";
    public static final String SALA_PASSWORD = "sala123@";
    public static final String JOHN_USER = "john";
    public static final String JOHN_PASSWORD = "john123@";
    
    public final static String projectVersion;
    static { 
        Properties testProps = new Properties();
        try {
            testProps.load(TestConstants.class.getResourceAsStream("/test.properties"));
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize projectVersion property: " + e.getMessage(), e);
        }
        projectVersion = testProps.getProperty("project.version");
    }
    
    /**
     * Vfs deployment
     */
    
    public static final String VFS_DEPLOYMENT_ID = "test";

    /**
     * Kjar deployment
     */
    
    public static final String GROUP_ID = "org.kie.test";
    public static final String ARTIFACT_ID = "kie-wb";
    public static final String VERSION = "1.0";
    public static final String KBASE_NAME = "defaultKieBase";
    public static final String KSESSION_NAME = "defaultKieSession";
 
    public static final String KJAR_DEPLOYMENT_ID;
    static { 
        KModuleDeploymentUnit deploymentUnit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION, KBASE_NAME, KSESSION_NAME);
        KJAR_DEPLOYMENT_ID = deploymentUnit.getIdentifier();
    }
    
}
