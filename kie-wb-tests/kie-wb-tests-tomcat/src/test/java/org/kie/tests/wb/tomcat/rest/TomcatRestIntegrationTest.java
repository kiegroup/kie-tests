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
package org.kie.tests.wb.tomcat.rest;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.MediaType;

import org.drools.core.command.runtime.process.StartProcessCommand;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jbpm.services.task.commands.CompleteTaskCommand;
import org.jbpm.services.task.commands.GetTasksByProcessInstanceIdCommand;
import org.jbpm.services.task.commands.StartTaskCommand;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.services.client.api.RemoteJmsRuntimeEngineFactory;
import org.kie.services.client.api.RemoteRestSessionFactory;
import org.kie.services.client.serialization.jaxb.JaxbCommandsRequest;
import org.kie.services.client.serialization.jaxb.JaxbCommandsResponse;
import org.kie.services.client.serialization.jaxb.JaxbSerializationProvider;
import org.kie.services.client.serialization.jaxb.impl.JaxbProcessInstanceResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbTaskSummaryListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunAsClient
@RunWith(Arquillian.class)
public class TomcatRestIntegrationTest {

    private static Logger logger = LoggerFactory.getLogger(TomcatRestIntegrationTest.class);
    
    private static final String CONNECTION_FACTORY_NAME = "jms/RemoteConnectionFactory";
    private static final String TASK_QUEUE_NAME = "jms/queue/KIE.TASK";
    private static final String RESPONSE_QUEUE_NAME = "jms/queue/KIE.RESPONSE";
    
    protected final static String projectVersion;
    static { 
        Properties testProps = new Properties();
        try {
            testProps.load(TomcatRestIntegrationTest.class.getResourceAsStream("/test.properties"));
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize projectVersion property: " + e.getMessage(), e);
        }
        projectVersion = testProps.getProperty("project.version");
    }
    
    @Deployment(testable = false)
    public static WebArchive createWar() {
        File [] warFile = 
                Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("org.kie:kie-wb-distribution-wars:war:tomcat7:" + projectVersion )
                .withoutTransitivity()
                .asFile();
        
        ZipImporter war = ShrinkWrap.create(ZipImporter.class, "arquillian-test.war").importFrom(warFile[0]);
        
        return war.as(WebArchive.class);
    }
    
    @ArquillianResource
    URL deploymentUrl;

    private static final long QUALITY_OF_SERVICE_THRESHOLD_MS = 5 * 1000;
    
    @AfterClass
    public static void waitForTxOnServer() throws InterruptedException { 
        Thread.sleep(1000);
    }
   
    private long findTaskId(long procInstId, List<TaskSummary> taskSumList) { 
        long taskId = -1;
        for( TaskSummary task : taskSumList ) { 
            if( task.getProcessInstanceId() == procInstId ) {
                taskId = task.getId();
            }
        }
        assertNotEquals("Could not determine taskId!", -1, taskId);
        return taskId;
    }
    
    @Test
    @InSequence(1)
    public void testRestUrlStartHumanTaskProcess() throws Exception { 
        // create REST request
        String urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/runtime/test/process/org.jbpm.humantask/start").toExternalForm();
        ClientRequest restRequest = new ClientRequest(urlString);

        // Get and check response
        logger.debug( ">> [org.jbpm.humantask/start]" + urlString );
        ClientResponse responseObj = restRequest.post();
        assertEquals(200, responseObj.getStatus());
        JaxbProcessInstanceResponse processInstance = (JaxbProcessInstanceResponse) responseObj.getEntity(JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        urlString = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/task/query?taskOwner=salaboy").toExternalForm();
        restRequest = new ClientRequest(urlString);
        logger.debug( ">> [task/query]" + urlString );
        responseObj = restRequest.get();
        assertEquals(200, responseObj.getStatus());
        
        JaxbTaskSummaryListResponse taskSumlistResponse = (JaxbTaskSummaryListResponse) responseObj.getEntity(JaxbTaskSummaryListResponse.class);
        long taskId = findTaskId(procInstId, taskSumlistResponse.getResult());
        
        urlString = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/task/" + taskId + "/start?userId=salaboy").toExternalForm();
        restRequest = new ClientRequest(urlString);

        // Get response
        logger.debug( ">> [task/?/start] " + urlString );
        responseObj = restRequest.post();

        // Check response
        assertEquals(200, responseObj.getStatus());
//        result = responseObj.getEntity();
//        System.out.println(result);

    }
    
    @Test
    @InSequence(2)
    public void testRestExecuteStartProcess() throws Exception { 
        // Start process
        String urlString = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/runtime/test/execute").toExternalForm();
        
        ClientRequest restRequest = new ClientRequest(urlString);
        JaxbCommandsRequest commandMessage = new JaxbCommandsRequest("test", new StartProcessCommand("org.jbpm.humantask"));
        String body = JaxbSerializationProvider.convertJaxbObjectToString(commandMessage);
        restRequest.body(MediaType.APPLICATION_XML, body);

        logger.debug( ">> [startProcess] " + urlString );
        ClientResponse responseObj = restRequest.post();

        assertEquals(200, responseObj.getStatus());
        JaxbCommandsResponse cmdsResp = (JaxbCommandsResponse) responseObj.getEntity(JaxbCommandsResponse.class);
        long procInstId = ((ProcessInstance) cmdsResp.getResponses().get(0)).getId();

        // query tasks
        restRequest = new ClientRequest(urlString);
        commandMessage = new JaxbCommandsRequest("test", new GetTasksByProcessInstanceIdCommand(procInstId));
        body = JaxbSerializationProvider.convertJaxbObjectToString(commandMessage);
        restRequest.body(MediaType.APPLICATION_XML, body);

        logger.debug( ">> [getTasksByProcessInstanceId] " + urlString );
        responseObj = restRequest.post();
        assertEquals(200, responseObj.getStatus());
        JaxbCommandsResponse cmdResponse = (JaxbCommandsResponse) responseObj.getEntity(JaxbCommandsResponse.class);
        List list = (List) cmdResponse.getResponses().get(0).getResult();
        long taskId = (Long) list.get(0);
        
        // start task
        
        logger.debug( ">> [startTask] " + urlString );
        restRequest = new ClientRequest(urlString);
        commandMessage = new JaxbCommandsRequest(new StartTaskCommand(taskId, "salaboy"));
        body = JaxbSerializationProvider.convertJaxbObjectToString(commandMessage);
        restRequest.body(MediaType.APPLICATION_XML, commandMessage);

        // Get response
        responseObj = restRequest.post();

        // Check response
        assertEquals(200, responseObj.getStatus());
//        Object result = responseObj.getEntity();
//        System.out.println(result);

        urlString = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/task/execute").toExternalForm();
        
        restRequest = new ClientRequest(urlString);
        commandMessage = new JaxbCommandsRequest(new CompleteTaskCommand(taskId, "salaboy", null));
        body = JaxbSerializationProvider.convertJaxbObjectToString(commandMessage);
        restRequest.body(MediaType.APPLICATION_XML, commandMessage);

        // Get response
        logger.debug( ">> [completeTask] " + urlString );
        responseObj = restRequest.post();

        // Check response
        logger.debug("response status: " + responseObj.getStatus());
//        assertEquals(200, responseObj.getStatus());
//        Object result = responseObj.getEntity();
//        System.out.println(result);

//        System.out.println("Failure now ?");
//        
//        restRequest = new ClientRequest(urlString);
//        commandMessage = new JaxbCommandMessage(null, 1, 
//          new CompleteTaskCommand(1, "salaboy", null));
//        body = JaxbSerializationProvider.convertJaxbObjectToString(commandMessage);
//        System.out.println(body);
//        restRequest.body(MediaType.APPLICATION_XML, commandMessage);
//
//        // Get response
//        responseObj = restRequest.post();
//
//        // Check response
//        System.out.println(responseObj.getStatus());
//        assertEquals(200, responseObj.getStatus());
//        Object result = responseObj.getEntity();
//        System.out.println(result);

    }
    
    @Test
    @InSequence(3)
    public void testRestRemoteApiHumanTaskProcess() throws Exception {
        // create REST request
        RuntimeEngine engine = new RemoteRestSessionFactory(deploymentUrl.toExternalForm(), "test").newRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess("org.jbpm.humantask");
        
        logger.debug("Started process instance: " + processInstance + " " + (processInstance == null? "" : processInstance.getId()));
        
        TaskService taskService = engine.getTaskService();
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("salaboy", "en-UK");
        long taskId = findTaskId(processInstance.getId(), tasks);
        
        logger.debug("Found task " + taskId);
        Task task = taskService.getTaskById(taskId);
        logger.debug("Got task " + taskId + ": " + task );
        taskService.start(taskId, "salaboy");
        taskService.complete(taskId, "salaboy", null);
        
        logger.debug("Now expecting failure");
        try {
        	taskService.complete(taskId, "salaboy", null);
        	fail( "Should not be able to complete task " + taskId + " a second time.");
        } catch (Throwable t) {
            // do nothing
        }
        
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        List<TaskSummary> taskIds = taskService.getTasksByStatusByProcessInstanceId(processInstance.getId(), statuses, "en-UK");
        assertEquals("Expected 2 tasks.", 2, taskIds.size());
    }
    
}
