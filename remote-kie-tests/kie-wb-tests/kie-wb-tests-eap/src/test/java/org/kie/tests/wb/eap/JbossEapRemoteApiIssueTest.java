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

import static org.junit.Assert.*;
import static org.kie.tests.wb.base.util.TestConstants.HUMAN_TASK_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.*;
import static org.kie.tests.wb.eap.KieWbWarJbossEapDeploy.createTestWar;

import java.net.URL;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jbpm.services.task.impl.model.xml.JaxbTask;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.remote.client.api.RemoteRestRuntimeEngineBuilder;
import org.kie.remote.client.jaxb.JaxbTaskSummaryListResponse;
import org.kie.remote.tests.base.RestUtil;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.tests.wb.base.methods.RepositoryDeploymentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        String userName = MARY_USER;
        String password = MARY_PASSWORD;
        
        // deploy

        RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, userName, password, 5);
        deployUtil.setStrategy(RuntimeStrategy.SINGLETON);

        String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
        String repositoryName = "tests";
        String project = "integration-tests";
        String deploymentId = KJAR_DEPLOYMENT_ID;
        String orgUnit = UUID.randomUUID().toString();
        deployUtil.createRepositoryAndDeployProject(repoUrl, repositoryName, project, deploymentId, orgUnit);

        int sleep = 2;
        logger.info("Waiting {} more seconds to make sure deploy is done..", sleep);
        Thread.sleep(sleep * 1000); 
        
        // Remote API setup
        // @formatter:off
        RemoteRestRuntimeEngineBuilder builder = RemoteRuntimeEngineFactory.newRestBuilder()
                .addDeploymentId(deploymentId)
                .addUrl(deploymentUrl)
                .addUserName(userName)
                .addPassword(password);
        // @formatter:on
        RuntimeEngine engine = builder.build();
        KieSession ksession = engine.getKieSession();
        
        // 1. start process
        ProcessInstance processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        assertNotNull("Null ProcessInstance!", processInstance);
        long procInstId = processInstance.getId();

        // @formatter:off
        TaskService taskService = RemoteRuntimeEngineFactory.newRestBuilder()
                .addUrl(deploymentUrl)
                .addUserName(userName)
                .addDeploymentId(KJAR_DEPLOYMENT_ID)
                .addPassword(password)
                .build().getTaskService();
        // @formatter:on
     
        // 2. find task (without deployment id)
        long sameTaskId;
        { 
            List<Long> taskIds = taskService.getTasksByProcessInstanceId(procInstId);
            assertEquals("Incorrect number of tasks for started process: ", 1, taskIds.size());
            sameTaskId = taskIds.get(0);
        }
        
        // 2b. Get the task instance itself
        Task task = taskService.getTaskById(sameTaskId);
        String actualOwnerId = task.getTaskData().getActualOwner().getId();
        assertNotNull( "Null actual owner on actual task!", actualOwnerId );
        assertEquals( "Incorrect task status ", Status.Reserved, task.getTaskData().getStatus() );
        
        JaxbTask jaxbTask = RestUtil.get(deploymentUrl, 
                "rest/task/" + task.getId(), MediaType.APPLICATION_XML,
                200, userName, password,
                JaxbTask.class);
        assertNotNull( "Null actual owner on actual task!", jaxbTask.getTaskData().getActualOwner() );
        assertEquals( "Incorrect actual owner on JAXB task!", actualOwnerId, jaxbTask.getTaskData().getActualOwner().getId() );
        assertEquals( "Incorrect task status ", Status.Reserved, task.getTaskData().getStatus() );

        TaskSummary taskSum = getTaskSummary(SALA_USER, SALA_PASSWORD, task.getId());
        assertNotNull( "Empty actual owner user in task summary", taskSum.getActualOwner() );
        assertEquals( "Incorrect actual owner user in task summary", actualOwnerId, taskSum.getActualOwner().getId() );

        // 3. Start the task
        taskService.start(sameTaskId, userName);
    }
    
    private TaskSummary getTaskSummary( String user, String password, long taskId) throws Exception {
        JaxbTaskSummaryListResponse taskSumListResp = RestUtil.get(deploymentUrl, 
                "rest/task/query?taskId=" + taskId, MediaType.APPLICATION_XML,
                200, user, password,
                JaxbTaskSummaryListResponse.class);
        List<TaskSummary> taskSumList = taskSumListResp.getResult();
        assertEquals("No task found with task id [" + taskId + "]", 1, taskSumList.size());
        return taskSumList.get(0);
    }
}
