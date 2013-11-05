package org.kie.tests.drools.wb.jboss.test;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.tests.drools.wb.jboss.base.DroolsWbWarJbossDeploy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunAsClient
@RunWith(Arquillian.class)
public class DroolsWbRestJbossIntegrationTest extends DroolsWbWarJbossDeploy {

    private static Logger logger = LoggerFactory.getLogger(DroolsWbRestJbossIntegrationTest.class);
    
    public static final String USER = "admin";
    public static final String PASSWORD = "admin";
    public static final String MARY_USER = "mary";
    public static final String MARY_PASSWORD = "mary123@";
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
        String urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/repositories").toExternalForm();
        ClientRequest restRequest = createRequest(requestFactory, urlString);
        ClientResponse<?> responseObj = checkResponse(restRequest.get());
        System.out.println(responseObj.getEntity(String.class));
    }
    
    private ClientResponse<?> checkResponse(ClientResponse<?> responseObj) throws Exception {
        responseObj.resetStream();
        int status = responseObj.getStatus(); 
        if( status != 200 ) { 
            logger.warn("Response with exception:\n" + responseObj.getEntity(String.class));
            assertEquals( "Status OK", 200, status);
        } 
        return responseObj;
    }
    
    private ClientRequest createRequest(ClientRequestFactory requestFactory, String urlString) { 
        ClientRequest restRequest = requestFactory.createRequest(urlString);
        restRequest.accept(MediaType.APPLICATION_JSON_TYPE);
        logger.debug( ">> " + urlString);
        return restRequest;
    }
}
