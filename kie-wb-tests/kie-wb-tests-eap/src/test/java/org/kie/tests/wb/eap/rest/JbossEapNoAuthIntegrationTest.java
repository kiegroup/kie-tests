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
package org.kie.tests.wb.eap.rest;

import static org.kie.tests.wb.base.methods.TestConstants.VFS_DEPLOYMENT_ID;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.tests.wb.base.methods.RestIntegrationTestMethods;
import org.kie.tests.wb.base.setup.DatasourceServerSetupTask;
import org.kie.tests.wb.eap.base.KieServicesRemoteDeploy;

@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup({DatasourceServerSetupTask.class})
public class JbossEapNoAuthIntegrationTest extends KieServicesRemoteDeploy {

    @Deployment(testable = false)
    public static Archive<?> createWar() {
       return createWebArchive("rest-kie-services-remote-test");
    }

    @ArquillianResource
    URL deploymentUrl;

    private RestIntegrationTestMethods restTests = new RestIntegrationTestMethods(VFS_DEPLOYMENT_ID);
    
    @AfterClass
    public static void waitForTxOnServer() throws InterruptedException { 
        Thread.sleep(1000);
    }
   
    @Test
    public void testJsonAndXmlStartProcess() throws Exception { 
        ClientRequestFactory requestFactory = createNoAuthRequestFactory(deploymentUrl);
        restTests.jsonAndXmlStartProcess(deploymentUrl, requestFactory);
    }
    
    @Test
    public void testRestUrlStartHumanTaskProcess() throws Exception {
        ClientRequestFactory requestFactory = createNoAuthRequestFactory(deploymentUrl);
        restTests.urlStartHumanTaskProcessTest(deploymentUrl, requestFactory, requestFactory);
    }
    
    @Test
    public void testRestExecuteStartProcess() throws Exception { 
        ClientRequestFactory requestFactory = createNoAuthRequestFactory(deploymentUrl);
        restTests.executeStartProcess(deploymentUrl, requestFactory);
    }
    
    @Test
    public void testRestHistoryLogs() throws Exception {
        ClientRequestFactory requestFactory = createNoAuthRequestFactory(deploymentUrl);
        restTests.restHistoryLogs(deploymentUrl, requestFactory);
    }
}
