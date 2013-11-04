package org.kie.tests.drools.wb.eap.test;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.tests.drools.wb.eap.base.DroolsWbWarEapDeploy;

@RunAsClient
@RunWith(Arquillian.class)
public class DroolsWbEapRestIntegrationTest extends DroolsWbWarEapDeploy {

    public static final String USER = "mary";
    public static final String PASSWORD = "mary123@";
    public static final String SALA_USER = "salaboy";
    public static final String SALA_PASSWORD = "sala123@";
    public static final String JOHN_USER = "john";
    public static final String JOHN_PASSWORD = "john123@";
    
    @Deployment(testable = false)
    public static Archive<?> createWar() {
       return createWarWithTestDeploymentLoader("kie-drools-wb-deploy-test", "eap-6_1");
    }
    
    @Test
    public void firstRestTest() { 
        
    }
}
