/*
 * JBoss, Home of Professional Open Source
 * 
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.tests.wb.eap;

import static org.kie.tests.wb.base.methods.TestConstants.*;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.tests.wb.base.methods.JmsIntegrationTestMethods;
import org.kie.tests.wb.base.methods.RestIntegrationTestMethods;
import org.kie.tests.wb.eap.deploy.KieWbWarJbossEapDeploy;

@RunAsClient
@RunWith(Arquillian.class)
public class JbossEapRemoteApiIntegrationTest extends KieWbWarJbossEapDeploy {

    @Deployment(testable = false, name = "kie-wb-basic-auth")
    public static Archive<?> createWar() {
        return createTestWar("eap-6_1");
    }

    @ArquillianResource
    URL deploymentUrl;

    private RestIntegrationTestMethods restTests = new RestIntegrationTestMethods(KJAR_DEPLOYMENT_ID, MediaType.APPLICATION_JSON);
    private JmsIntegrationTestMethods jmsTests = new JmsIntegrationTestMethods(KJAR_DEPLOYMENT_ID);

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
        printTestName();
        restTests.urlsDeployModuleForOtherTests(deploymentUrl, MARY_USER, MARY_PASSWORD, MediaType.APPLICATION_JSON_TYPE, false);
    }

    @Test
    @InSequence(2)
    public void testRestUrlStartHumanTaskProcess() throws Exception {
        printTestName();
        restTests.urlsStartHumanTaskProcess(deploymentUrl, SALA_USER, SALA_PASSWORD);
    }
    
    @Test
    @InSequence(1)
    public void testScratch() throws Exception {
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
        restTests.urlsRetrieveTaskContent(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestUrlsVariableHistory() throws Exception {
        printTestName();
        restTests.urlsVariableHistory(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }
    
    // JMS ------------------------------------------------------------------------------------------------------------------------
    
    @Test
    @InSequence(2)
    public void testJmsStartProcess() throws Exception {
        printTestName();
        jmsTests.commandsStartProcess(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testJmsRemoteApiHumanTaskProcess() throws Exception {
        printTestName();
        jmsTests.remoteApiHumanTaskProcess(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testJmsRemoteApiExceptions() throws Exception {
        printTestName();
        jmsTests.remoteApiException(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsNoProcessInstanceFound() throws Exception {
        printTestName();
        jmsTests.remoteApiNoProcessInstanceFound(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsCompleteSimpleHumanTask() throws Exception {
        printTestName();
        jmsTests.remoteApiAndCommandsCompleteSimpleHumanTask(MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testJmsExtraJaxbClasses() throws Exception {
        printTestName();
        jmsTests.remoteApiExtraJaxbClasses(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsRemoteApiRuleTaskProcess() throws Exception { 
        jmsTests.remoteApiRuleTaskProcess(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsRemoteApiStartProcessInstanceInitiator() throws Exception { 
        jmsTests.remoteApiInitiatorIdentityTest(MARY_USER, MARY_PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsRemoteApiRunEvaluationProcess() throws Exception { 
        jmsTests.remoteApiRunEvaluationProcess();
    }
}
