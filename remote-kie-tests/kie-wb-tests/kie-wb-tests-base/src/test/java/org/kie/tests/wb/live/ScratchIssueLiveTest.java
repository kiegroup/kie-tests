package org.kie.tests.wb.live;

import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;

import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;

//@Ignore // add Junit "Ping Succeed or Ignore" rule
public class ScratchIssueLiveTest {

    private static URL deploymentUrl;
    static { 
        try {
            deploymentUrl = new URL("http://localhost:8080/business-central/");
        } catch( MalformedURLException e ) {
            // do nothingj
        }
    }
    
    KieWbRestIntegrationTestMethods restTests = KieWbRestIntegrationTestMethods.newBuilderInstance()
            .setDeploymentId(KJAR_DEPLOYMENT_ID)
            .setMediaType(MediaType.APPLICATION_XML)
            .build();
        
    @Test
    public void queryUrlTest() throws Exception { 
        restTests.urlsHumanTaskWithVariableChangeFormParameters(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
    
    @Test 
    public void remoteApiTest() throws Exception { 
        restTests.getDeploymentIdFromTaskTest(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
}
