package org.kie.tests.wb.base;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.kie.internal.runtime.conf.RuntimeStrategy;

public class FakeIntegrationTest extends AbstractRemoteApiIntegrationTest {

    @Override
    public boolean doDeploy() {
        return false;
    }

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_XML;
    }

    @Override
    public boolean jmsQueuesAvailable() {
        return false;
    }

    @Override
    public boolean doRestTests() {
        return true;
    }

    @Override
    public RuntimeStrategy getStrategy() {
        return RuntimeStrategy.SINGLETON;
    }

    @Override
    public int getTimeoutInSecs() {
        return 5;
    }

    private static FakeRestServer fakeRestServer;
    
    @BeforeClass
    public static void beforeClass() throws Exception { 
        fakeRestServer = new FakeRestServer();
        fakeRestServer.start();
    }
  
    @Override
    public void liveSetDeploymentUrl() throws Exception { 
        this.deploymentUrl = new URL("http://localhost:" + fakeRestServer.getPort() + "/kie-wb/rest/" );
    }
    
    @AfterClass
    public static void afterClass() throws Exception { 
       fakeRestServer.stop(); 
    }
}
