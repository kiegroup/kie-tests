package org.kie.tests.wb.live;

import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;

import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.junit.Ignore;
import org.junit.Test;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.kie.tests.wb.base.methods.KieWbWebServicesIntegrationTestMethods;

@Ignore // add Junit "Ping Succeed or Ignore" rule
public class ScratchIssueLiveTest {

    private KieWbWebServicesIntegrationTestMethods wsTests = new KieWbWebServicesIntegrationTestMethods();
 
    private static URL deploymentUrl;
    static { 
        try {
            deploymentUrl = new URL("http://localhost:8080/kie-wb/");
        } catch( MalformedURLException e ) {
            // do nothingj
        }
    }
    
    @Test
    public void issueTest() throws Exception { 
        KieWbRestIntegrationTestMethods restTests = KieWbRestIntegrationTestMethods.newBuilderInstance()
                .setDeploymentId(KJAR_DEPLOYMENT_ID)
                .setMediaType(MediaType.APPLICATION_XML)
                .build();
        
        // deploy
        restTests.remoteApiGetTaskInstance(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
}
