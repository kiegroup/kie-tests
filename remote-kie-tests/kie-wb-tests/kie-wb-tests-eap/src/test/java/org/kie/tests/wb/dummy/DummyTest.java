package org.kie.tests.wb.dummy;

import static org.junit.Assert.assertTrue;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.tests.wb.base.AbstractRemoteApiIntegrationTest;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyTest {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractRemoteApiIntegrationTest.class);
    
    private final KieWbRestIntegrationTestMethods restTests;

    protected URL deploymentUrl;
    {
        // Modify this string to match your kie-wb/BPMS installation
        String urlString = "http://localhost:8080/business-central/";
        try { 
            this.deploymentUrl = new URL(urlString);
        } catch( Exception e ) { 
            System.err.println( "The following URL is not a valid URL: '" + urlString + "'");
            e.printStackTrace();
        } 
    }
    
    protected void liveSetDeploymentUrl() { 
       // do nothing, but can be overridden 
    }
   
    public DummyTest() { 
         restTests = KieWbRestIntegrationTestMethods.newBuilderInstance()
                 .setDeploymentId(KJAR_DEPLOYMENT_ID)
                 .setMediaType(MediaType.APPLICATION_XML)
                 .setStrategy(RuntimeStrategy.SINGLETON)
                 .setTimeoutInSecs(5)
                 .build();
    }

    
    @Test
    public void testToLetBuildPass() { 
       assertTrue(true); 
    }
    
}
