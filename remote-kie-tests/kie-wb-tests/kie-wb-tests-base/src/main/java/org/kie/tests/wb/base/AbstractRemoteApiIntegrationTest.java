package org.kie.tests.wb.base;

import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;

import java.net.URL;

import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.tests.wb.base.methods.KieWbJmsIntegrationTestMethods;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.kie.tests.wb.base.methods.KieWbWebServicesIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRemoteApiIntegrationTest {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractRemoteApiIntegrationTest.class);

    private final KieWbRestIntegrationTestMethods restTests;
    private final KieWbWebServicesIntegrationTestMethods wsTests = new KieWbWebServicesIntegrationTestMethods();
    private final KieWbJmsIntegrationTestMethods jmsTests;

    @ArquillianResource
    protected URL deploymentUrl;

    protected void liveSetDeploymentUrl() throws Exception {
       // do nothing, but can be overridden
    }

    public abstract boolean doDeploy();
    public abstract String getContentType();
    public abstract boolean jmsQueuesAvailable();
    public abstract boolean doRestTests();
    public abstract RuntimeStrategy getStrategy();
    public abstract int getTimeoutInSecs();

    public AbstractRemoteApiIntegrationTest() {
         restTests = KieWbRestIntegrationTestMethods.newBuilderInstance()
                 .setDeploymentId(KJAR_DEPLOYMENT_ID)
                 .setMediaType(getContentType())
                 .setStrategy(getStrategy())
                 .setTimeoutInSecs(getTimeoutInSecs())
                 .build();
         if( jmsQueuesAvailable() ) {
             jmsTests = new KieWbJmsIntegrationTestMethods(KJAR_DEPLOYMENT_ID);
         } else {
             jmsTests = null;
         }
    }

    private final static int SETUP = 0;

    private final static int REST_ERROR = 1;
    private final static int REST_FAILING = 2;
    private final static int REST_REPAIRED = 3;
    private final static int REST_SUCCEEDING = 4;
    private final static int REST_RANDOM = 5;

    private final static int JMS_ERROR = 6;
    private final static int JMS_FAILING = 7;
    private final static int JMS_REPAIRED = 8;
    private final static int JMS_SUCCEEDING = 9;
    private final static int JMS_RANDOM = 10;

    @AfterClass
    public static void waitForTxOnServer() throws InterruptedException {
        long sleep = 1000;
        logger.info("Waiting " + sleep / 1000 + " secs for tx's on server to close.");
//        Thread.sleep(sleep);
    }

    @BeforeClass
    public static void waitForDeployedKmodulesToLoad() throws InterruptedException {
        long sleep = 2000;
        logger.info("Waiting " + sleep / 1000 + " secs for server to finish starting up.");
//        Thread.sleep(sleep);
    }

    protected void printTestName() {
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println( "-=> " + testName );
    }

    @Before
    public void optionalSetDeploymentUrl() throws Exception {
        liveSetDeploymentUrl();
    }

    @Test
    @InSequence(SETUP)
    public void setupDeployment() throws Exception {
        Assume.assumeTrue(doDeploy());

        printTestName();
        restTests.urlsDeployModuleForOtherTests(deploymentUrl, MARY_USER, MARY_PASSWORD);
        Thread.sleep(5000);
    }

    @Test
    @InSequence(REST_ERROR)
    public void webserviceTest() throws Exception {
        printTestName();
        wsTests.startSimpleProcess(deploymentUrl);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsGetDeployments() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsGetDeployments(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsHistoryLogs() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsHistoryLogs(deploymentUrl, MARY_USER, MARY_PASSWORD);
        restTests.urlsStartScriptProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_FAILING)
    public void testRestUrlsCommandsStartProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsCommandsStartProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_FAILING)
    public void testRestRemoteApiHumanTaskProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsCommandsTaskCommands() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsCommandsTaskCommands(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsJsonJaxbStartProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsJsonJaxbStartProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_FAILING)
    public void testRestUrlsHumanTask() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsHumanTask(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsHttpURLConnectionAcceptHeaderIsFixed() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsHttpURLConnectionAcceptHeaderIsFixed(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_FAILING)
    public void testRestRemoteApiProcessInstances() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiProcessInstances(deploymentUrl, MARY_USER, MARY_PASSWORD);
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
    public void testRestUrlsVariableHistory() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsVariableHistory(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsGetRealProcessVariable() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsGetRealProcessVariable(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiHumanTaskGroupId() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskGroupId(deploymentUrl);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsHumanTaskGroupAssignment() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsHumanTaskGroupAssignment(deploymentUrl);
    }

    @Test
    @InSequence(REST_ERROR)
    public void testRestRemoteApiHumanTaskGroupVarAssign() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskGroupVarAssign(deploymentUrl);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiHumanTaskOwnType() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskOwnType(deploymentUrl);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsGetProcessDefinitions() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsGetProcessDefinitions(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

//    @Test
//    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsCreateMemoryLeakOnTomcat() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsCreateMemoryLeakOnTomcat(deploymentUrl, MARY_USER, MARY_PASSWORD, getTimeoutInSecs());
    }

    @Test
    @InSequence(REST_FAILING)
    public void testRestUrlsAndRemoteApiDeploymentRedeployClassPath() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsAndRemoteApiDeploymentRedeployClassPath(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiGroupAssignmentEngineering() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiGroupAssignmentEngineering(deploymentUrl);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiHumanTaskComment() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskComment(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_ERROR)
    public void testRestRemoteApiCorrelationKeyOperations() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiCorrelationKeyOperations(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsByteArrayProcessVariable() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsByteArrayProcessVariable(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiExceptionNoDeployment() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiExceptionNoDeployment(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsStartScriptProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsStartScriptProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsQueryProcessInstancesNotFiltering() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsQueryProcessInstancesNotFiltering(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiFunnyCharacters() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiFunnyCharacters(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiHistoryVariables() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHistoryVariables(deploymentUrl);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiStartScriptProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiStartScriptProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsDeploymentProcessDefinitions() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsDeploymentProcessDefinitions(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestUrlsProcessQueryOperations() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsProcessQueryOperations(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiInsecureHumanTaskProcess() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiInsecureHumanTaskProcess(deploymentUrl);
    }

    // JMS ------------------------------------------------------------------------------------------------------------------------

    @Test
    @InSequence(JMS_RANDOM)
    public void testJmsQueueCommandsStartProcess() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.queueCommandsStartProcess(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(JMS_RANDOM)
    public void testJmsRemoteApiHumanTaskProcess() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiHumanTaskProcess(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(JMS_FAILING)
    public void testJmsRemoteApiExceptionNoDeployment() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiExceptionNoDeployment(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(JMS_SUCCEEDING)
    public void testJmsRemoteApiProcessInstances() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiProcessInstances(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(JMS_SUCCEEDING)
    public void testJmsQueueAndRemoteApiCompleteSimpleHumanTask() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.queueAndRemoteApiCompleteSimpleHumanTask(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(JMS_ERROR)
    public void testJmsRemoteApiExtraJaxbClasses() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiExtraJaxbClasses(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(JMS_RANDOM)
    public void testJmsRemoteApiRuleTaskProcess() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiRuleTaskProcess(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(JMS_RANDOM)
    public void testJmsRemoteApiInitiatorIdentity() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiInitiatorIdentity(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(JMS_RANDOM)
    public void testJmsRemoteApiHumanTaskGroupId() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiHumanTaskGroupId(deploymentUrl);
    }

    @Test
    @InSequence(JMS_ERROR)
    public void testJmsRemoteApiGroupAssignmentEngineering() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiGroupAssignmentEngineering(deploymentUrl);
    }

    @Test
    @InSequence(JMS_SUCCEEDING)
    public void testJmsRemoteApiHistoryVariables() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.remoteApiHistoryVariables(deploymentUrl);
    }

    @Test
    @InSequence(JMS_SUCCEEDING)
    public void testJmsQueueSendEmptyRequest() throws Exception {
        Assume.assumeTrue(jmsQueuesAvailable());
        printTestName();
        jmsTests.queueSendEmptyRequest(MARY_USER, MARY_PASSWORD);
    }
}
