package org.kie.tests.drools.wb.jboss.test;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.tests.drools.wb.jboss.base.DroolsWbWarJbossDeploy;

@RunAsClient
@RunWith(Arquillian.class)
public class DroolsWbRestJbossIntegrationTest extends DroolsWbWarJbossDeploy {

    public static final String USER = "mary";
    public static final String PASSWORD = "mary123@";
    public static final String SALA_USER = "salaboy";
    public static final String SALA_PASSWORD = "sala123@";
    public static final String JOHN_USER = "john";
    public static final String JOHN_PASSWORD = "john123@";
    
    @ArquillianResource
    URL deploymentUrl;
    
    @Deployment(testable = false, name="drools-wb")
    public static Archive<?> createWar() {
       return createWarWithTestDeploymentLoader("test");
    }
    
    @Test
    public void deployTest() throws Exception { 
        ClientRequestFactory requestFactory = createBasicAuthRequestFactory(deploymentUrl, USER, PASSWORD);
        requestFactory.createRequest(null);
    }
}
