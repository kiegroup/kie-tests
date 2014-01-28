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
package org.kie.tests.wb.eap.live;

import static org.kie.tests.wb.base.methods.TestConstants.*;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.tests.wb.base.methods.RestIntegrationTestMethods;
import org.kie.tests.wb.eap.deploy.KieWbWarJbossEapDeploy;

@Ignore
public class LiveJbossEapRestIntegrationTest extends KieWbWarJbossEapDeploy {

    private URL deploymentUrl; 
    
    public LiveJbossEapRestIntegrationTest() { 
        try { 
         deploymentUrl = new URL("http://localhost:8080/business-central/");
        } catch(Exception e) { 
            e.printStackTrace();
        }
    }

    private RestIntegrationTestMethods restTests = new RestIntegrationTestMethods(KJAR_DEPLOYMENT_ID, MediaType.APPLICATION_JSON);

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
    @InSequence(1)
    public void setupDeployment() throws Exception {
        printTestName();
        restTests.urlsDeployModuleForOtherTests(deploymentUrl, MARY_USER, MARY_PASSWORD, MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    @InSequence(2)
    public void testRestUrlStartHumanTaskProcess() throws Exception {
        printTestName();
        restTests.urlsStartHumanTaskProcess(deploymentUrl, SALA_USER, SALA_PASSWORD);
    }

    @Test
    @InSequence(3)
    public void testRestExecuteStartProcess() throws Exception {
        printTestName();
        restTests.commandsStartProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(4)
    public void testRestRemoteApiHumanTaskProcess() throws Exception {
        printTestName();
        restTests.remoteApiHumanTaskProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(5)
    public void testRestExecuteTaskCommands() throws Exception {
        printTestName();
        restTests.commandsTaskCommands(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testRestHistoryLogs() throws Exception {
        printTestName();
        restTests.urlsHistoryLogs(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

//    @Test
//    @InSequence(7)
    public void testRestDataServicesCoupling() throws Exception {
        printTestName();
        restTests.urlsDataServiceCoupling(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(8)
    public void testJsonAndXmlStartProcess() throws Exception {
        printTestName();
        restTests.urlsJsonJaxbStartProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(9)
    public void testHumanTaskCompleteWithVariable() throws Exception {
        printTestName();
        restTests.urlsHumanTaskWithFormVariableChange(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(10)
    public void testHttpURLConnection() throws Exception {
        printTestName();
        restTests.urlsHttpURLConnectionAcceptHeaderIsFixed(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(11)
    public void testRemoteApiProcessInstances() throws Exception {
        printTestName();
        restTests.remoteApiSerialization(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(12)
    public void testRemoteApiExtraJaxbClasses() throws Exception {
        printTestName();
        restTests.remoteApiExtraJaxbClasses(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(13)
    public void testRemoteApiRuleTaskProcess() throws Exception {
        printTestName();
        restTests.remoteApiRuleTaskProcess(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

    @Test
    @InSequence(14)
    public void testRemoteApiGetTaskInstance() throws Exception {
        printTestName();
        restTests.remoteApiGetTaskInstance(deploymentUrl, MARY_USER, MARY_PASSWORD);
    }

}