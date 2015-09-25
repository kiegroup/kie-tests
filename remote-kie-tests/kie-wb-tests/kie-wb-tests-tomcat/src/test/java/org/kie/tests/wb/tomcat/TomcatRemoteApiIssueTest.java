/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
package org.kie.tests.wb.tomcat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.base.util.TestConstants.REASSIGNMENT_PROCESS_ID;
import static org.kie.tests.wb.tomcat.KieWbWarTomcatDeploy.createTestWar;

import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.kie.tests.wb.base.methods.RepositoryDeploymentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunAsClient
@RunWith(Arquillian.class)
public class TomcatRemoteApiIssueTest {

    @Deployment(testable = false, name = "kie-wb-tomcat")
    public static Archive<?> createWar() {
        return createTestWar();
    }

    private static final Logger logger = LoggerFactory.getLogger(TomcatRemoteApiIssueTest.class);

    @ArquillianResource
    URL deploymentUrl;


    private static final String USER_ID = MARY_USER;
    private static final String PASSWORD = MARY_PASSWORD;

    private static final AtomicBoolean deploymentDeployed = new AtomicBoolean(false);

    protected void printTestName() {
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println( "-=> " + testName );
    }

    @Before
    public void deployTestDeployment() throws Exception {
        if( deploymentDeployed.compareAndSet(false, true) ) {
            // deploy
            RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, USER_ID, PASSWORD, 5);
            deployUtil.setStrategy(RuntimeStrategy.PER_PROCESS_INSTANCE);

            String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
            String repositoryName = "tests";
            String project = "integration-tests";
            String deploymentId = KJAR_DEPLOYMENT_ID;
            String orgUnit = UUID.randomUUID().toString();
            orgUnit = orgUnit.substring(0, orgUnit.indexOf("-"));
            deployUtil.createRepositoryAndDeployProject(repoUrl, repositoryName, project, deploymentId, orgUnit);

            int sleep = 2;
            logger.info("Waiting {} more seconds to make sure deploy is done..", sleep);
            Thread.sleep(sleep * 1000);
        }
    }


    @Test
    public void issueTest() throws Exception {
        printTestName();

        RuntimeEngine runtimeEngine = RemoteRuntimeEngineFactory.newRestBuilder()
                .addDeploymentId(KJAR_DEPLOYMENT_ID)
                .addUrl(deploymentUrl)
                .addUserName(MARY_USER)
                .addPassword(MARY_PASSWORD)
                .build();

        KieSession ksession = runtimeEngine.getKieSession();
        TaskService taskService = runtimeEngine.getTaskService();

        // test
        ProcessInstance procInst = ksession.startProcess(REASSIGNMENT_PROCESS_ID);
        long startTime = System.currentTimeMillis();

        assertNotNull( "Null process instance", procInst );
        assertEquals( "Process instance state", ProcessInstance.STATE_ACTIVE, procInst.getState() );
        long procInstId = procInst.getId();

        List<Long> taskIds = taskService.getTasksByProcessInstanceId(procInstId);
        assertFalse( "No task ids found", taskIds.isEmpty() );
        assertEquals( "Task ids for this process instance", 1,  taskIds.size() );

        long taskId = taskIds.get(0);

        taskService.start(taskId, MARY_USER);
        taskService.complete(taskId, MARY_USER, null);

        float timePassedSecs = ((float) (System.currentTimeMillis()-startTime))/1000;
        System.out.println( "Time passed: " + timePassedSecs);
        long waitPeriods = 8;
        long sleepPeriod = 2;
        while( timePassedSecs > sleepPeriod ) {
           timePassedSecs -= sleepPeriod;
           waitPeriods--;
        }

        int i = 0;
        float firstSleep = ((sleepPeriod*1000)-(timePassedSecs*1000))/1000;
        System.out.println("Sleeping " + firstSleep + " secs");
        Thread.currentThread().sleep((long) (firstSleep*1000));
        i++;

        for( ; i < waitPeriods; ++i ) {
            Thread.currentThread().sleep(sleepPeriod*1000);
            System.out.println( ".. " + (i+1)*sleepPeriod );
        }

        procInst = ksession.getProcessInstance(procInstId);
        assertTrue( "Process instance has not completed!", procInst == null || procInst.getState() == ProcessInstance.STATE_COMPLETED );

    }
}
