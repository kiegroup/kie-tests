package org.kie.tests.wb.base;

import static org.kie.tests.wb.base.util.TestConstants.JOHN_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.JOHN_USER;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.base.util.TestConstants.SALA_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.SALA_USER;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.services.client.api.RemoteJmsRuntimeEngineFactory;
import org.kie.tests.wb.base.methods.KieWbJmsIntegrationTestMethods;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRemoteApiIntegrationTest {
    
    protected static final Logger logger = LoggerFactory.getLogger(AbstractRemoteApiIntegrationTest.class);
    
    private final KieWbRestIntegrationTestMethods restTests;
    private final KieWbJmsIntegrationTestMethods jmsTests;

    @ArquillianResource
    URL deploymentUrl;
   
    public abstract boolean doDeploy();
    public abstract MediaType getMediaType();
    public abstract boolean jmsQueuesAvailable();
    public abstract boolean doRestTests();
    public abstract RuntimeStrategy getStrategy();
    public abstract int getTimeoutInSecs();
   
    public AbstractRemoteApiIntegrationTest() { 
         restTests = KieWbRestIntegrationTestMethods.newBuilderInstance()
                 .setDeploymentId(KJAR_DEPLOYMENT_ID)
                 .setMediaType(getMediaType())
                 .setStrategy(getStrategy())
                 .setTimeout(getTimeoutInSecs())
                 .build();
         if( jmsQueuesAvailable() ) { 
             jmsTests = new KieWbJmsIntegrationTestMethods(KJAR_DEPLOYMENT_ID);
         } else { 
             jmsTests = null;
         }
    }

    private final static int SETUP = 0;
    
    private final static int REST_FAILING = 1;
    private final static int REST_SUCCEEDING = 2;
    private final static int REST_RANDOM = 3;
    
    private final static int JMS_FAILING = 4;
    private final static int JMS_SUCCEEDING = 5;
    private final static int JMS_RANDOM = 6;
    
    @AfterClass
    public static void waitForTxOnServer() throws InterruptedException {
        long sleep = 1000;
        logger.info("Waiting " + sleep / 1000 + " secs for tx's on server to close.");
        Thread.sleep(sleep);
    }

    @BeforeClass
    public static void waitForDeployedKmodulesToLoad() throws InterruptedException {
        long sleep = 2000;
        logger.info("Waiting " + sleep / 1000 + " secs for server to finish starting up.");
        Thread.sleep(sleep);
    }

    protected void printTestName() { 
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println( "-=> " + testName );
    }
    
    @Test
    @InSequence(SETUP)
    public void setupDeployment() throws Exception {
        Assume.assumeTrue(doDeploy());
        
        printTestName();
        restTests.urlsDeployModuleForOtherTests(deploymentUrl, MARY_USER, MARY_PASSWORD, true);
        Thread.sleep(5000);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlStartHumanTaskProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsStartHumanTaskProcess(deploymentUrl, SALA_USER, SALA_PASSWORD);
    }
    
    @Test
    @InSequence(REST_SUCCEEDING)
    public void testUrlsGetDeployments() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsGetDeployments(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestHistoryLogs() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsHistoryLogs(deploymentUrl, MARY_USER, MARY_PASSWORD);
        restTests.urlsStartScriptProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestExecuteStartProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsCommandsStartProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_RANDOM)
    public void testRestRemoteApiHumanTaskProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestExecuteTaskCommands() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsCommandsTaskCommands(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestJsonAndXmlStartProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsJsonJaxbStartProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestHumanTaskCompleteWithVariable() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsHumanTaskWithVariableChangeFormParameters(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestHttpURLConnection() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsHttpURLConnectionAcceptHeaderIsFixed(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_FAILING)
    public void testRestRemoteApiProcessInstances() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiSerialization(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiExtraJaxbClasses() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiExtraJaxbClasses(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiRuleTaskProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiRuleTaskProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiGetTaskInstance() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiGetTaskInstance(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_RANDOM)
    public void testRestUrlsGetTaskContent() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsGetTaskAndTaskContent(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsVariableHistory() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsVariableHistory(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsRetrieveProcVar() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsGetRealProcessVariable(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
   
    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiHumanTaskGroupId() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskGroupIdTest(deploymentUrl);
    }
    
    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsGroupAssignmentProcess() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsGroupAssignmentTest(deploymentUrl);
    }
   
    @Test
    @InSequence(REST_FAILING)
    public void testRestRemoteApiHumanTaskGroupVarAssign() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskGroupVarAssignTest(deploymentUrl);
    }
    
    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiHumanTaskOwnType() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskOwnTypeTest(deploymentUrl);
    }
   
    @Test
    @InSequence(REST_FAILING) 
    public void testRestUrlsGetProcessDefinitions() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsGetProcessDefinitionInfo(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestTomcatMemoryLeak() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsCreateMemoryLeakOnTomcat(deploymentUrl, MARY_USER, MARY_PASSWORD, getTimeoutInSecs());
    }
    
    @Test
    @InSequence(REST_SUCCEEDING)
    public void testDeploymentRedeployClassPathTest() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiDeploymentRedeployClassPathTest(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
    
    // JMS ------------------------------------------------------------------------------------------------------------------------
    
    @Test
    @InSequence(JMS_SUCCEEDING)
    public void testJmsStartProcess() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.commandsStartProcess(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(JMS_SUCCEEDING)
    public void testJmsRemoteApiHumanTaskProcess() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiHumanTaskProcess(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(JMS_SUCCEEDING)
    public void testJmsRemoteApiExceptions() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiException(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(JMS_SUCCEEDING)
    public void testJmsNoProcessInstanceFound() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiNoProcessInstanceFound(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(JMS_SUCCEEDING)
    public void testJmsCompleteSimpleHumanTask() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiAndCommandsCompleteSimpleHumanTask(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(JMS_SUCCEEDING)
    public void testJmsExtraJaxbClasses() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiExtraJaxbClasses(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(JMS_SUCCEEDING)
    public void testJmsRemoteApiRuleTaskProcess() throws Exception { 
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiRuleTaskProcess(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(JMS_SUCCEEDING)
    public void testJmsRemoteApiStartProcessInstanceInitiator() throws Exception { 
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiInitiatorIdentityTest(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(JMS_SUCCEEDING)
    public void testJmsRemoteApiHumanTaskGroupId() throws Exception { 
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiHumanTaskGroupIdTest(deploymentUrl);
    }
    
    @Test
    @InSequence(JMS_FAILING)
    public void testJmsRemoteApiGroupAssignmentEngineering() throws Exception { 
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        RuntimeEngine runtimeEngine = RemoteJmsRuntimeEngineFactory.newBuilder()
                .addDeploymentId(KJAR_DEPLOYMENT_ID)
                .useSsl(true)
                .addHostName("localhost")
                .addJmsConnectorPort(5446)
                .addKeystoreLocation("ssl/client_keystore.jks")
                .addKeystorePassword("CLIENT_KEYSTORE_PASSWORD")
                .useKeystoreAsTruststore()
                .addUserName(JOHN_USER)
                .addPassword(JOHN_PASSWORD)
                .addJbossServerUrl(deploymentUrl)
                .build();
        
        jmsTests.remoteApiGroupAssignmentEngineeringTest(runtimeEngine);
    }
    
    @Test
    @InSequence(JMS_FAILING)
    public void testJmsRemoteApiHistoryVariablesTest() throws Exception { 
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiHistoryVariablesTest(deploymentUrl);
    }

}
