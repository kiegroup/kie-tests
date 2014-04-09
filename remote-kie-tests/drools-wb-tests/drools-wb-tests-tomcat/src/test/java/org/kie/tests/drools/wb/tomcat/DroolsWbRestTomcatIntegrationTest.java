package org.kie.tests.drools.wb.tomcat;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.tests.drools.wb.base.methods.RestIntegrationTestMethods;

@RunAsClient
@RunWith(Arquillian.class)
public class DroolsWbRestTomcatIntegrationTest extends DroolsWbWarTomcatDeploy {

    @Deployment(testable = false, name="drools-wb")
    public static Archive<?> createWar() {
       return createWarWithTestDeploymentLoader("test");
    }
    
    private RestIntegrationTestMethods restTests = new RestIntegrationTestMethods();
    
    @ArquillianResource
    URL deploymentUrl;
    
    @Test
    public void manipulatingRepositories() throws Exception {
        restTests.manipulatingRepositories(deploymentUrl);
    }
    
    @Test
    public void mavenOperations() throws Exception {
        restTests.mavenOperations(deploymentUrl);
    }
    
    @Test
    public void manipulatingOUs() throws Exception {
        restTests.manipulatingOUs(deploymentUrl);
    }
    
}
