package org.kie.tests.wb.base;

import static org.kie.tests.wb.base.methods.TestConstants.JOHN_PASSWORD;
import static org.kie.tests.wb.base.methods.TestConstants.JOHN_USER;
import static org.kie.tests.wb.base.methods.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.methods.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.methods.TestConstants.MARY_USER;
import static org.kie.tests.wb.base.methods.TestConstants.SALA_PASSWORD;
import static org.kie.tests.wb.base.methods.TestConstants.SALA_USER;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.internal.deployment.DeploymentUnit.RuntimeStrategy;
import org.kie.services.client.api.RemoteJmsRuntimeEngineFactory;
import org.kie.services.client.api.builder.RemoteJmsRuntimeEngineFactoryBuilder;
import org.kie.tests.wb.base.methods.JmsIntegrationTestMethods;
import org.kie.tests.wb.base.methods.RestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractIssueIntegrationTest {
    
    protected static final Logger logger = LoggerFactory.getLogger(AbstractIssueIntegrationTest.class);
    
    private final RestIntegrationTestMethods restTests;

    @ArquillianResource
    URL deploymentUrl;
   
    public abstract boolean doDeploy();
    public abstract MediaType getMediaType();
    public abstract boolean useFormBasedAuth();
    public abstract RuntimeStrategy getStrategy();
    public abstract long getTimeout();
   
    public AbstractIssueIntegrationTest() { 
         restTests = new RestIntegrationTestMethods(KJAR_DEPLOYMENT_ID, getMediaType(), useFormBasedAuth(), getStrategy());
    }

    
    @AfterClass
    public static void waitForTxOnServer() throws InterruptedException {
        long sleep = 1000;
        logger.info("Waiting " + sleep / 1000 + " secs for tx's on server to close.");
        Thread.sleep(sleep);
    }

    @BeforeClass
    public static void waitForDeployedKmodulesToLoad() throws InterruptedException {
        long sleep = 2000;
        logger.info("Waiting " + sleep / 1000 + " secs for KieModules to deploy and load..");
        Thread.sleep(sleep);
    }

    protected void printTestName() { 
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println( "-=> " + testName );
    }
    
    @Test
    public void testDeploymentGroupAssignment() throws Exception { 
        printTestName();
        restTests.urlsDeployModuleForOtherTests(deploymentUrl, MARY_USER, MARY_PASSWORD);
        restTests.remoteApiGroupAssignmentEngineeringTest(deploymentUrl);
    }
    
    // JMS ------------------------------------------------------------------------------------------------------------------------

    
}
