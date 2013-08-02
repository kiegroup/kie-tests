/*
 * JBoss, Home of Professional Open Source
 * 
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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
package org.kie.tests.wb.jboss.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
import org.kie.internal.runtime.manager.InternalRuntimeManager;
import org.kie.services.client.api.RemoteConfiguration.AuthenticationType;
import org.kie.services.client.api.RemoteRestSessionFactory;
import org.kie.tests.wb.jboss.JbossIntegrationTestBase;
import org.kie.tests.wb.jboss.setup.ArquillianJbossServerSetupTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(ArquillianJbossServerSetupTask.class)
public class JbossAsRestIntegrationTest extends JbossIntegrationTestBase {

    private static Logger logger = LoggerFactory.getLogger(JbossAsRestIntegrationTest.class);
    
    @ArquillianResource
    URL deploymentUrl;

    @AfterClass
    public static void waitForTxOnServer() throws InterruptedException { 
        Thread.sleep(1000);
    }
   
    @Test
    public void testRestRemoteApiHumanTaskProcess() throws Exception {
        // create REST request
        RuntimeEngine engine = new RemoteRestSessionFactory(DEPLOYMENT_ID, deploymentUrl.toExternalForm(), AuthenticationType.FORM, USER, PASSWORD).newRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        
        ProcessInstance processInstance = ksession.startProcess(PROCESS_ID);
        
        logger.debug("Started process instance: " + processInstance + " " + (processInstance == null? "" : processInstance.getId()));
        
        TaskService taskService = engine.getTaskService();
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("salaboy", "en-UK");
        long taskId = findTaskId(processInstance.getId(), tasks);
        
        logger.debug("Found task " + taskId);
        Task task = taskService.getTaskById(taskId);
        logger.debug("Got task " + taskId + ": " + task );
        taskService.start(taskId, "salaboy");
        taskService.complete(taskId, "salaboy", null);
        
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        List<TaskSummary> taskIds = taskService.getTasksByStatusByProcessInstanceId(processInstance.getId(), statuses, "en-UK");
        assertEquals("Expected 2 tasks.", 2, taskIds.size());
    }
    
}
