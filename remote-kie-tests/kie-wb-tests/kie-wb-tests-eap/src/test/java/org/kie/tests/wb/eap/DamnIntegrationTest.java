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
import static org.kie.tests.wb.eap.KieWbWarJbossEapDeploy.createTestWar;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.tests.wb.base.AbstractRemoteApiIntegrationTest;
import org.kie.tests.wb.base.methods.JmsIntegrationTestMethods;
import org.kie.tests.wb.base.methods.RestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunAsClient
@RunWith(Arquillian.class)
public class DamnIntegrationTest {

    @Deployment(testable = false, name = "kie-wb-eap")
    public static Archive<?> createWar() {
        return createTestWar("eap-6_1");
    }

    protected static final Logger logger = LoggerFactory.getLogger(AbstractRemoteApiIntegrationTest.class);
    
    private final RestIntegrationTestMethods restTests;
    private final JmsIntegrationTestMethods jmsTests;

    @ArquillianResource
    URL deploymentUrl;
   
    public DamnIntegrationTest() {
         restTests = new RestIntegrationTestMethods(KJAR_DEPLOYMENT_ID, MediaType.APPLICATION_XML_TYPE, false);
         jmsTests = new JmsIntegrationTestMethods(KJAR_DEPLOYMENT_ID);
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
   
    @InSequence(1)
    @Test
    public void deploy() throws Exception { 
        Assume.assumeTrue(false);
        restTests.urlsDeployModuleForOtherTests(deploymentUrl, MARY_USER, MARY_PASSWORD, false);
        
        logger.info( "SLEEP FOR 10!");
        Thread.sleep(10*1000);
    }
    
    @InSequence(2)
    @Test
    public void jmsExtraJaxb() throws Exception { 
        jmsTests.remoteApiExtraJaxbClasses(MARY_USER, MARY_PASSWORD);
    }
    
}
