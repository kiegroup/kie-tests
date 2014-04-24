package org.kie.tests.wb.base;

import static org.kie.tests.wb.base.methods.TestConstants.*;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.internal.deployment.DeploymentUnit.RuntimeStrategy;
import org.kie.tests.wb.base.methods.JmsIntegrationTestMethods;
import org.kie.tests.wb.base.methods.RestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRemoteApiIntegrationTest {
    
    protected static final Logger logger = LoggerFactory.getLogger(AbstractRemoteApiIntegrationTest.class);
    
    private final RestIntegrationTestMethods restTests;
    private final JmsIntegrationTestMethods jmsTests;

    @ArquillianResource
    URL deploymentUrl;
   
    public abstract boolean doDeploy();
    public abstract MediaType getMediaType();
    public abstract boolean jmsQueuesAvailable();
    public abstract boolean doRestTests();
    public abstract boolean useFormBasedAuth();
    public abstract RuntimeStrategy getStrategy();
   
    public AbstractRemoteApiIntegrationTest() { 
         restTests = new RestIntegrationTestMethods(KJAR_DEPLOYMENT_ID, getMediaType(), useFormBasedAuth(), getStrategy());
         if( jmsQueuesAvailable() ) { 
             jmsTests = new JmsIntegrationTestMethods(KJAR_DEPLOYMENT_ID);
         } else { 
             jmsTests = null;
         }
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
    @InSequence(0)
    public void setupDeployment() throws Exception {
        Assume.assumeTrue(doDeploy());
        
        printTestName();
        restTests.urlsDeployModuleForOtherTests(deploymentUrl, MARY_USER, MARY_PASSWORD, false);
        Thread.sleep(5000);
    }

    @Test
    @InSequence(2)
    public void testRestUrlStartHumanTaskProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsStartHumanTaskProcess(deploymentUrl, SALA_USER, SALA_PASSWORD);
    }
    
    @Test
    @InSequence(1)
    public void testUrlsGetDeployments() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsGetDeployments(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestHistoryLogs() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsHistoryLogs(deploymentUrl, MARY_USER, MARY_PASSWORD);
        restTests.urlsStartScriptProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestExecuteStartProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.commandsStartProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestRemoteApiHumanTaskProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestExecuteTaskCommands() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.commandsTaskCommands(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestDataServicesCoupling() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsDataServiceCoupling(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestJsonAndXmlStartProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsJsonJaxbStartProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestHumanTaskCompleteWithVariable() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsHumanTaskWithFormVariableChange(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestHttpURLConnection() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsHttpURLConnectionAcceptHeaderIsFixed(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestRemoteApiProcessInstances() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiSerialization(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestRemoteApiExtraJaxbClasses() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiExtraJaxbClasses(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestRemoteApiRuleTaskProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiRuleTaskProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestRemoteApiGetTaskInstance() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiGetTaskInstance(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestUrlsGetTaskContent() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsGetTaskAndTaskContent(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestUrlsVariableHistory() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsVariableHistory(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testRestUrlsRetrieveProcVar() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsGetRealProcessVariable(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
   
    @Test
    @InSequence(2)
    public void testRestRemoteApiHumanTaskGroupId() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskGroupIdTest(deploymentUrl);
    }
    
    @Test
    @InSequence(2)
    public void testRestUrlsGroupAssignmentProcess() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        Assume.assumeTrue(jmsQueuesAvailable());
        restTests.remoteApiGroupAssignmentTest(deploymentUrl);
    }
   
    @Test
    @InSequence(2)
    public void testRestRemoteApiHumanTaskGroupVarAssign() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskGroupVarAssignTest(deploymentUrl);
    }
    
    // JMS ------------------------------------------------------------------------------------------------------------------------
    
    @Test
    @InSequence(2)
    public void testJmsStartProcess() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.commandsStartProcess(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testJmsRemoteApiHumanTaskProcess() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiHumanTaskProcess(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testJmsRemoteApiExceptions() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiException(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsNoProcessInstanceFound() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiNoProcessInstanceFound(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsCompleteSimpleHumanTask() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiAndCommandsCompleteSimpleHumanTask(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testJmsExtraJaxbClasses() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiExtraJaxbClasses(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsRemoteApiRuleTaskProcess() throws Exception { 
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiRuleTaskProcess(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsRemoteApiStartProcessInstanceInitiator() throws Exception { 
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiInitiatorIdentityTest(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsRemoteApiHumanTaskGroupId() throws Exception { 
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiHumanTaskGroupIdTest(deploymentUrl);
    }
    
}
