package org.kie.tests.wb.base.live;

import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.base.util.TestConstants.SALA_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.SALA_USER;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.junit.InSequence;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.tests.wb.base.methods.KieWbJmsIntegrationTestMethods;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbRemoteApiIntegrationTest {
    
    protected static final Logger logger = LoggerFactory.getLogger(KieWbRemoteApiIntegrationTest.class);
    
    private final KieWbRestIntegrationTestMethods restTests;
    private final KieWbJmsIntegrationTestMethods jmsTests;

    private URL deploymentUrl;
    {
        // Modify this string to match your kie-wb/BPMS installation
        String urlString = "http://localhost:8080/kie-wb/";
        try { 
            deploymentUrl = new URL(urlString);
        } catch( Exception e ) { 
            System.err.println( "The following URL is not a valid URL: '" + urlString + "'");
            e.printStackTrace();
        }
    }
   
    public boolean doDeploy() { 
       return false; 
    }
    
    public MediaType getMediaType() { 
       return MediaType.APPLICATION_XML_TYPE; 
    }
    
    public boolean jmsQueuesAvailable() { 
       return false; 
    }
    
    public boolean doRestTests() { 
       return true; 
    }
    
    public RuntimeStrategy getStrategy() { 
       return RuntimeStrategy.SINGLETON; 
    }
    
    public int getTimeoutInSecs() { 
       return 5; 
    }
   
    public KieWbRemoteApiIntegrationTest() { 
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
    
    private final static int REST_ERROR = 1;
    private final static int REST_FAILING = 2;
    private final static int REST_REPAIRED = 3;
    private final static int REST_SUCCEEDING = 4;
    private final static int REST_RANDOM = 5;
    
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
        restTests.urlsDeployModuleForOtherTests(deploymentUrl, MARY_USER, MARY_PASSWORD, false);
        Thread.sleep(5000);
    }

    @Test
    @InSequence(REST_FAILING)
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
    @InSequence(REST_REPAIRED)
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
    @InSequence(REST_FAILING)
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
    @InSequence(REST_REPAIRED)
    public void testRestRemoteApiExtraJaxbClasses() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiExtraJaxbClasses(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(REST_REPAIRED)
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
    @InSequence(REST_ERROR)
    public void testRestUrlsRetrieveProcVar() throws Exception {
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsGetRealProcessVariable(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
   
    @Test
    @InSequence(REST_FAILING)
    public void testRestRemoteApiHumanTaskGroupId() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskGroupIdTest(deploymentUrl);
    }
    
    @Test
    @InSequence(REST_FAILING)
    public void testRestUrlsGroupAssignmentProcess() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.urlsGroupAssignmentTest(deploymentUrl);
    }
   
    @Test
    @InSequence(REST_SUCCEEDING)
    public void testRestRemoteApiHumanTaskGroupVarAssign() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskGroupVarAssignTest(deploymentUrl);
    }
    
    @Test
    @InSequence(REST_ERROR)
    public void testRestRemoteApiHumanTaskOwnType() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiHumanTaskOwnTypeTest(deploymentUrl);
    }
   
    @Test
    @InSequence(REST_SUCCEEDING) 
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
    @InSequence(REST_FAILING)
    public void testDeploymentRedeployClassPathTest() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiDeploymentRedeployClassPathTest(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
   
    @Test
    @InSequence(REST_FAILING)
    public void testRemoteApiGroupAssignmentEngineeringTest() throws Exception { 
        Assume.assumeTrue(doRestTests());
        printTestName();
        restTests.remoteApiGroupAssignmentEngineeringTest(deploymentUrl);
    }
    
    // JMS ------------------------------------------------------------------------------------------------------------------------


}
