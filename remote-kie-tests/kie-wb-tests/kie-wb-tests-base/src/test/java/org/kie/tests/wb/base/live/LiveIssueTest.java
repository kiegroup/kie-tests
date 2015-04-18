package org.kie.tests.wb.base.live;

import static org.kie.tests.wb.base.methods.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.methods.TestConstants.*;

import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.internal.deployment.DeploymentUnit.RuntimeStrategy;
import org.kie.tests.wb.base.methods.RestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveIssueTest {
    
    protected static final Logger logger = LoggerFactory.getLogger(LiveIssueTest.class);
    
    private final RestIntegrationTestMethods restTests;

    URL deploymentUrl;
    { 
        try {
            deploymentUrl = new URL("http://localhost:8080/business-central/");
        } catch( MalformedURLException e ) {
            e.printStackTrace();
        }
    }
   
    public LiveIssueTest() { 
         restTests = new RestIntegrationTestMethods(KJAR_DEPLOYMENT_ID, MediaType.APPLICATION_XML_TYPE, false, RuntimeStrategy.SINGLETON);
    }

    
    protected void printTestName() { 
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println( "-=> " + testName );
    }
    
    @Test
    public void testDeploymentGroupAssignment() throws Exception { 
        printTestName();
        for( int i = 0; i < 1005; ++i ) { 
          restTests.urlsStartHumanTaskProcess(deploymentUrl, SALA_USER, SALA_PASSWORD);
        }
    }
    
}
