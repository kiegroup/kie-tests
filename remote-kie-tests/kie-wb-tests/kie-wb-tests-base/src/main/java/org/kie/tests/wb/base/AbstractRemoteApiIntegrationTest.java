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
    public abstract boolean useFormBasedAuth();
   
    public AbstractRemoteApiIntegrationTest() { 
         restTests = new RestIntegrationTestMethods(KJAR_DEPLOYMENT_ID, getMediaType(), useFormBasedAuth());
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
        printTestName();
        restTests.urlsStartHumanTaskProcess(deploymentUrl, SALA_USER, SALA_PASSWORD);
    }
    
    @Test
    @InSequence(1)
    public void testUrlsGetDeployments() throws Exception {
        printTestName();
        restTests.urlsGetDeployments(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestHistoryLogs() throws Exception {
        printTestName();
        restTests.urlsHistoryLogs(deploymentUrl, MARY_USER, MARY_PASSWORD);
        restTests.urlsStartScriptProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestExecuteStartProcess() throws Exception {
        printTestName();
        restTests.commandsStartProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestRemoteApiHumanTaskProcess() throws Exception {
        printTestName();
        restTests.remoteApiHumanTaskProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestExecuteTaskCommands() throws Exception {
        printTestName();
        restTests.commandsTaskCommands(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestDataServicesCoupling() throws Exception {
        printTestName();
        restTests.urlsDataServiceCoupling(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestJsonAndXmlStartProcess() throws Exception {
        printTestName();
        restTests.urlsJsonJaxbStartProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestHumanTaskCompleteWithVariable() throws Exception {
        printTestName();
        restTests.urlsHumanTaskWithFormVariableChange(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestHttpURLConnection() throws Exception {
        printTestName();
        restTests.urlsHttpURLConnectionAcceptHeaderIsFixed(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestRemoteApiProcessInstances() throws Exception {
        printTestName();
        restTests.remoteApiSerialization(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestRemoteApiExtraJaxbClasses() throws Exception {
        printTestName();
        restTests.remoteApiExtraJaxbClasses(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestRemoteApiRuleTaskProcess() throws Exception {
        printTestName();
        restTests.remoteApiRuleTaskProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestRemoteApiGetTaskInstance() throws Exception {
        printTestName();
        restTests.remoteApiGetTaskInstance(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestUrlsGetTaskContent() throws Exception {
        printTestName();
        restTests.urlsGetTaskAndTaskContent(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestUrlsVariableHistory() throws Exception {
        printTestName();
        restTests.urlsVariableHistory(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testRestUrlsRetrieveProcVar() throws Exception {
        printTestName();
        restTests.urlsGetRealProcessVariable(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
   
    @Test
    @InSequence(2)
    public void testRestRemoteApiHumanTaskGroupId() throws Exception { 
        printTestName();
        restTests.remoteApiHumanTaskGroupIdTest(deploymentUrl);
    }
    
    @Test
    @InSequence(2)
    public void testRestUrlsGroupAssignmentProcess() throws Exception { 
        printTestName();
        Assume.assumeTrue(jmsQueuesAvailable());
        restTests.remoteApiGroupAssignmentTest(deploymentUrl);
    }
    
    // JMS ------------------------------------------------------------------------------------------------------------------------
    
    @Test
    @InSequence(2)
    public void testJmsStartProcess() throws Exception {
        printTestName();
        Assume.assumeTrue(jmsQueuesAvailable());
        jmsTests.commandsStartProcess(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testJmsRemoteApiHumanTaskProcess() throws Exception {
        printTestName();
        Assume.assumeTrue(jmsQueuesAvailable());
        jmsTests.remoteApiHumanTaskProcess(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testJmsRemoteApiExceptions() throws Exception {
        printTestName();
        Assume.assumeTrue(jmsQueuesAvailable());
        jmsTests.remoteApiException(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsNoProcessInstanceFound() throws Exception {
        printTestName();
        Assume.assumeTrue(jmsQueuesAvailable());
        jmsTests.remoteApiNoProcessInstanceFound(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsCompleteSimpleHumanTask() throws Exception {
        printTestName();
        Assume.assumeTrue(jmsQueuesAvailable());
        jmsTests.remoteApiAndCommandsCompleteSimpleHumanTask(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testJmsExtraJaxbClasses() throws Exception {
        printTestName();
        Assume.assumeTrue(jmsQueuesAvailable());
        jmsTests.remoteApiExtraJaxbClasses(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsRemoteApiRuleTaskProcess() throws Exception { 
        printTestName();
        Assume.assumeTrue(jmsQueuesAvailable());
        jmsTests.remoteApiRuleTaskProcess(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsRemoteApiStartProcessInstanceInitiator() throws Exception { 
        printTestName();
        Assume.assumeTrue(jmsQueuesAvailable());
        jmsTests.remoteApiInitiatorIdentityTest(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsRemoteApiRunEvaluationProcess() throws Exception { 
        printTestName();
        Assume.assumeTrue(jmsQueuesAvailable());
        jmsTests.remoteApiRunEvaluationProcess();
    }
   
    @Test
    @InSequence(2)
    public void testJmsRemoteApiHumanTaskGroupId() throws Exception { 
        printTestName();
        Assume.assumeTrue(jmsQueuesAvailable());
        jmsTests.remoteApiHumanTaskGroupIdTest(deploymentUrl);
    }

    
}
