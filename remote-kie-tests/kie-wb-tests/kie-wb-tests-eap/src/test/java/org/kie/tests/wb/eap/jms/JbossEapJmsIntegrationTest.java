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
package org.kie.tests.wb.eap.jms;

import static org.kie.tests.wb.base.methods.TestConstants.*;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.kie.tests.wb.base.methods.JmsIntegrationTestMethods;
import org.kie.tests.wb.base.methods.RestIntegrationTestMethods;
import org.kie.tests.wb.eap.deploy.KieWbWarJbossEapDeploy;

@RunAsClient
@RunWith(Arquillian.class)
public class JbossEapJmsIntegrationTest extends KieWbWarJbossEapDeploy {

    @Deployment(testable = false, name="kie-wb-basic-auth")
    public static Archive<?> createWar() {
       return createTestWar("eap-6_1");
    }

    @ArquillianResource
    URL deploymentUrl;

    private JmsIntegrationTestMethods jmsTests = new JmsIntegrationTestMethods(KJAR_DEPLOYMENT_ID);
    private RestIntegrationTestMethods restTests = new RestIntegrationTestMethods(KJAR_DEPLOYMENT_ID);
    
    @Rule
    public TestName testName;
    
    @AfterClass
    public static void waitForTxOnServer() throws InterruptedException { 
        long sleep = 1000;
        logger.info("Waiting " + sleep/1000 + " secs for tx's on server to close.");
        Thread.sleep(sleep);
    }

    @BeforeClass
    public static void waitForDeployedKmodulesToLoad() throws InterruptedException { 
        long sleep = 2000;
        logger.info("Waiting " + sleep/1000 + " secs for KieModules to deploy and load..");
        Thread.sleep(sleep);
    }

    @Test
    @InSequence(1)
    public void deployTestDeployment() throws Exception {
        printTestName();
        restTests.deployModuleForOtherTests(deploymentUrl, USER, PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testJmsStartProcess() throws Exception {
        printTestName();
        jmsTests.commandsStartProcess(USER, PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testJmsRemoteApiHumanTaskProcess() throws Exception {
        printTestName();
        jmsTests.remoteApiHumanTaskProcess(USER, PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testJmsRemoteApiExceptions() throws Exception {
        printTestName();
        jmsTests.remoteApiException(USER, PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testJmsNoProcessInstanceFound() throws Exception {
        printTestName();
        jmsTests.remoteApiNoProcessInstanceFound(USER, PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testCompleteSimpleHumanTask() throws Exception {
        printTestName();
        jmsTests.remoteApiAndCommandsCompleteSimpleHumanTask(USER, PASSWORD);
    }

    @Test
    @InSequence(2)
    public void testExtraJaxbClasses() throws Exception {
        printTestName();
        jmsTests.remoteApiExtraJaxbClasses(USER, PASSWORD);
    }
    
    @Test
    @InSequence(2)
    public void testRemoteApiRuleTaskProcess() throws Exception { 
        jmsTests.remoteApiRuleTaskProcess(USER, PASSWORD);
    }
}