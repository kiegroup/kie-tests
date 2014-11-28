package org.kie.tests.wb.base.live;

import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.base.util.TestConstants.SALA_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.SALA_USER;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.junit.InSequence;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.tests.wb.base.methods.KieWbJmsIntegrationTestMethods;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbRemoteApiIssueIntegrationTest {
    
    protected static final Logger logger = LoggerFactory.getLogger(KieWbRemoteApiIssueIntegrationTest.class);
    
    private final KieWbRestIntegrationTestMethods restTests;
    private final KieWbJmsIntegrationTestMethods jmsTests;

    private URL deploymentUrl;
    {
        // Modify this string to match your kie-wb/BPMS installation
        String urlString = "http://localhost:8080/business-central/";
        try { 
            deploymentUrl = new URL(urlString);
        } catch( Exception e ) { 
            System.err.println( "The following URL is not a valid URL: '" + urlString + "'");
            e.printStackTrace();
        }
    }
   
    public boolean doDeploy() { 
       return true; 
    }
    
    public MediaType getMediaType() { 
       return MediaType.APPLICATION_XML_TYPE; 
    }
    
    public boolean jmsQueuesAvailable() { 
       return false; 
    }
    
    public boolean doRestTests() { 
       return true; 
    }
    
    public RuntimeStrategy getStrategy() { 
       return RuntimeStrategy.SINGLETON; 
    }
    
    public int getTimeoutInSecs() { 
       return 5; 
    }
   
    public KieWbRemoteApiIssueIntegrationTest() { 
         restTests = KieWbRestIntegrationTestMethods.newBuilderInstance()
                 .setDeploymentId(KJAR_DEPLOYMENT_ID)
                 .setMediaType(getMediaType())
                 .setStrategy(getStrategy())
                 .setTimeoutInSecs(getTimeoutInSecs())
                 .build();
         if( jmsQueuesAvailable() ) { 
             jmsTests = new KieWbJmsIntegrationTestMethods(KJAR_DEPLOYMENT_ID);
         } else { 
             jmsTests = null;
         }
    }

    private final static int SETUP = 0;
    
    protected void printTestName() { 
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println( "-=> " + testName );
    }
    
    @Test
    @InSequence(SETUP)
    public void undeployRedeployTest() throws Exception {
        Assume.assumeTrue(doDeploy());
        
        printTestName();
        restTests.remoteApiDeploymentRedeployClassPathTest(deploymentUrl, MARY_USER, MARY_PASSWORD);
        Thread.sleep(5000);
    }

}
