/*
t  * JBoss, Home of Professional Open Source
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

import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.eap.KieWbWarJbossEapDeploy.createTestWar;

import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.remote.client.api.RemoteRuntimeEngineFactory;
import org.kie.tests.wb.base.methods.RepositoryDeploymentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bpms.flood.model.Person;
import com.bpms.flood.model.Request;

@RunAsClient
@RunWith(Arquillian.class)
public class JbossEapRemoteApiIssueTest {

    @Deployment(testable = false, name = "kie-wb-eap")
    public static Archive<?> createWar() {
        return createTestWar();
    }
 
    private static final Logger logger = LoggerFactory.getLogger(JbossEapRemoteApiIssueTest.class);
    
    @ArquillianResource
    URL deploymentUrl;
   
    
    @AfterClass
    public static void waitForTxOnServer() throws InterruptedException {
        long sleep = 1000;
        logger.info("Waiting " + sleep / 1000 + " secs for tx's on server to close.");
        Thread.sleep(sleep);
    }

    protected void printTestName() { 
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println( "-=> " + testName );
    }
    
    @Test
    public void issueTest() throws Exception { 
        printTestName();
        String username = MARY_USER;
        String password = MARY_PASSWORD;
        
        // deploy
        RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, username, password, 5);

        String repoUrl = "git://git.app.eng.bos.redhat.com/bpms-assets.git";
        String repositoryName = "bpms-assets";
        String project = "bpms-perf";
        String deploymentId = "com.bpms.flood:bpms-perf:1.0.0.Final";
        String orgUnit = UUID.randomUUID().toString();
        deployUtil.createRepositoryAndDeployProject(repoUrl, repositoryName, project, deploymentId, orgUnit, username);
        
        // test
        Person person = new Person("bpmsAdmin", "Dluhoslav Chudobny");
        person.setAge(25); // >= 18
        Request request = new Request("1");
        request.setPersonId("bpmsAdmin");
        request.setAmount(500); // < 1000
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("request", request);
        params.put("person", person);
     
        Class [] classes = { Person.class, Request.class };
        // @formatter:off
        RuntimeEngine runtimeEngine = 
        RemoteRuntimeEngineFactory.newRestBuilder()
            .addUrl(deploymentUrl)
            .addUserName(username)
            .addPassword(password)
            .addDeploymentId(deploymentId)
            .addExtraJaxbClasses(Person.class, Request.class)
            .build();
        // @formatter:on
        
        runtimeEngine.getKieSession().startProcess("com.bpms.flood.RemoteRuleTask", params);
    }
}
