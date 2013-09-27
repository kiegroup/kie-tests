package org.kie.tests.wb.eap.rest;

import static org.kie.tests.wb.base.methods.TestConstants.*;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.tests.wb.base.methods.RestIntegrationTestMethods;
import org.kie.tests.wb.base.setup.EmailServerSetupTask;
import org.kie.tests.wb.eap.base.KieWbWarDeploy;

/**
 * Ensures that our SMTP Service sends emails to an SMTP Server in both
 * synchronous and asynchronous calls.
 *
 * @author ALR
 */
//@RunAsClient
//@RunWith(Arquillian.class)
//@ServerSetup({EmailServerSetupTask.class})
@Ignore
public class JbossEapEmailIntegrationSkip extends KieWbWarDeploy {

    private static final String DEPLOYMENT_NAME = "kie-wb-email-test";

    
    /**
     * Deployment to be tested; will be manually deployed/undeployed
     * such that we can configure the server first
     *
     * @return
     */
    @Deployment(managed = false, name = DEPLOYMENT_NAME)
    public static WebArchive getApplicationDeployment() {
        return createWarWithTestDeploymentLoader(DEPLOYMENT_NAME, "eap-6_1", false, true);
    }

    /**
     * Tests
     * 
     */
    private RestIntegrationTestMethods restTests = new RestIntegrationTestMethods(KJAR_DEPLOYMENT_ID);
    
    /*
     * TESTS
     */

    /**
     * Tests that mail can be sent asynchronously via a JMS Queue
     * @throws Exception 
     */
    @Test
    @RunAsClient
    public void testEscalationEmailSnafu() throws Exception {

        // Set the body of the email to be sent
        final String body = "emailEscalationBody";

        URL deploymentUrl = new URL( "http://127.0.0.1:8080/" + DEPLOYMENT_NAME );
        ClientRequestFactory requestFactory = createBasicAuthRequestFactory(deploymentUrl, USER, PASSWORD);
        restTests.restEscalationEmailSnafu(deploymentUrl, requestFactory, USER, body, JOHN_USER);
    }

}
