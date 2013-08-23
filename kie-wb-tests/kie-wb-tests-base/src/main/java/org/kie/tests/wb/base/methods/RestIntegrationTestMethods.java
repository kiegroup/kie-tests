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
package org.kie.tests.wb.base.methods;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.drools.core.command.runtime.process.StartProcessCommand;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;
import org.jbpm.process.audit.xml.AbstractJaxbHistoryObject;
import org.jbpm.process.audit.xml.JaxbVariableInstanceLog;
import org.jbpm.services.task.commands.CompleteTaskCommand;
import org.jbpm.services.task.commands.GetTasksByProcessInstanceIdCommand;
import org.jbpm.services.task.commands.StartTaskCommand;
import org.jbpm.services.task.impl.model.xml.JaxbTask;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.services.client.api.RemoteRestSessionFactory;
import org.kie.services.client.serialization.jaxb.JaxbCommandsRequest;
import org.kie.services.client.serialization.jaxb.JaxbCommandsResponse;
import org.kie.services.client.serialization.jaxb.JaxbSerializationProvider;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbHistoryLogList;
import org.kie.services.client.serialization.jaxb.impl.JaxbLongListResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbProcessInstanceResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbTaskSummaryListResponse;
import org.kie.services.client.serialization.jaxb.rest.JaxbGenericResponse;
import org.kie.tests.wb.base.services.data.JaxbProcessInstanceSummary;

public class RestIntegrationTestMethods extends AbstractIntegrationTestMethods {

    private static final String taskUserId = "salaboy";
    
    private final String deploymentId;
    
    public RestIntegrationTestMethods(String deploymentId) { 
        this.deploymentId = deploymentId;
    }
    
    /**
     * Helper methods
     */
    
    private ClientResponse<?> checkResponse(ClientResponse<?> responseObj) throws Exception {
        responseObj.resetStream();
        int status = responseObj.getStatus(); 
        if( status != 200 ) { 
            System.out.println("Response with exception:\n" + responseObj.getEntity(String.class));
            assertEquals( "Status OK", 200, status);
        } 
        return responseObj;
    }
    
    /**
     * Test methods
     */
    
    public void urlStartHumanTaskProcessTest(URL deploymentUrl, ClientRequestFactory requestFactory, ClientRequestFactory taskRequestFactory) throws Exception { 
        // Start process
        String urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/runtime/"+ deploymentId + "/process/org.jbpm.humantask/start").toExternalForm();
        ClientRequest restRequest = requestFactory.createRequest(urlString);
        logger.debug( ">> " + urlString);
        ClientResponse<?> responseObj = checkResponse(restRequest.post());
        JaxbProcessInstanceResponse processInstance = (JaxbProcessInstanceResponse) responseObj.getEntity(JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        // query tasks for associated task Id
        urlString = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/task/query?taskOwner=" + taskUserId).toExternalForm();
        restRequest = requestFactory.createRequest(urlString);
        logger.debug( ">> " + urlString);
        responseObj = checkResponse(restRequest.get());
        
        JaxbTaskSummaryListResponse taskSumlistResponse = (JaxbTaskSummaryListResponse) responseObj.getEntity(JaxbTaskSummaryListResponse.class);
        long taskId = findTaskId(procInstId, taskSumlistResponse.getResult());
        
        // get task info
        urlString = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/task/" + taskId).toExternalForm();
        restRequest = requestFactory.createRequest(urlString);
        responseObj = checkResponse(restRequest.get());
        JaxbTask task = (JaxbTask) responseObj.getEntity(JaxbTask.class);
        assertEquals( "Incorrect task id", taskId, task.getId().longValue() );

        // start task
        urlString = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/task/" + taskId + "/start").toExternalForm();
        restRequest = taskRequestFactory.createRequest(urlString);
        responseObj = checkResponse(restRequest.post());
        JaxbGenericResponse resp = (JaxbGenericResponse) responseObj.getEntity(JaxbGenericResponse.class);
        
        // get task info
        urlString = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/task/" + taskId).toExternalForm();
        restRequest = requestFactory.createRequest(urlString);
        responseObj = checkResponse(restRequest.get());
    }
    
    public void executeStartProcess(URL deploymentUrl, ClientRequestFactory requestFactory) throws Exception { 
        // Start process
        String urlString = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/runtime/" + deploymentId + "/execute").toExternalForm();
        
        ClientRequest restRequest = requestFactory.createRequest(urlString);
        JaxbCommandsRequest commandMessage = new JaxbCommandsRequest(deploymentId, new StartProcessCommand("org.jbpm.humantask"));
        String body = JaxbSerializationProvider.convertJaxbObjectToString(commandMessage);
        restRequest.body(MediaType.APPLICATION_XML, body);

        logger.debug( ">> [startProcess] " + urlString );
        ClientResponse<?> responseObj = checkResponse(restRequest.post());

        JaxbCommandsResponse cmdsResp = (JaxbCommandsResponse) responseObj.getEntity(JaxbCommandsResponse.class);
        long procInstId = ((ProcessInstance) cmdsResp.getResponses().get(0)).getId();

        // query tasks
        restRequest = requestFactory.createRequest(urlString);
        commandMessage = new JaxbCommandsRequest(deploymentId, new GetTasksByProcessInstanceIdCommand(procInstId));
        body = JaxbSerializationProvider.convertJaxbObjectToString(commandMessage);
        restRequest.body(MediaType.APPLICATION_XML, body);

        logger.debug( ">> [getTasksByProcessInstanceId] " + urlString );
        responseObj = checkResponse(restRequest.post());
        JaxbCommandsResponse cmdResponse = (JaxbCommandsResponse) responseObj.getEntity(JaxbCommandsResponse.class);
        List<?> list = (List<?>) cmdResponse.getResponses().get(0).getResult();
        long taskId = (Long) list.get(0);
        
        // start task
        
        logger.debug( ">> [startTask] " + urlString );
        restRequest = requestFactory.createRequest(urlString);
        commandMessage = new JaxbCommandsRequest(new StartTaskCommand(taskId, taskUserId));
        body = JaxbSerializationProvider.convertJaxbObjectToString(commandMessage);
        restRequest.body(MediaType.APPLICATION_XML, commandMessage);

        // Get response
        responseObj = checkResponse(restRequest.post());
        responseObj.releaseConnection();

        urlString = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/task/execute").toExternalForm();
        
        restRequest = requestFactory.createRequest(urlString);
        commandMessage = new JaxbCommandsRequest(new CompleteTaskCommand(taskId, taskUserId, null));
        body = JaxbSerializationProvider.convertJaxbObjectToString(commandMessage);
        restRequest.body(MediaType.APPLICATION_XML, commandMessage);

        // Get response
        logger.debug( ">> [completeTask] " + urlString );
        checkResponse(restRequest.post());
        
        // TODO: check that above has completed?
    }

    public void remoteApiHumanTaskProcess(URL deploymentUrl, String user, String password) throws Exception {
        // create REST request
        RemoteRestSessionFactory restSessionFactory 
            = new RemoteRestSessionFactory(deploymentId, deploymentUrl.toExternalForm(), user, password);
        RuntimeEngine engine = restSessionFactory.newRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess("org.jbpm.humantask");
        
        logger.debug("Started process instance: " + processInstance + " " + (processInstance == null? "" : processInstance.getId()));
        
        TaskService taskService = engine.getTaskService();
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(taskUserId, "en-UK");
        long taskId = findTaskId(processInstance.getId(), tasks);
        
        logger.debug("Found task " + taskId);
        Task task = taskService.getTaskById(taskId);
        logger.debug("Got task " + taskId + ": " + task );
        taskService.start(taskId, taskUserId);
        taskService.complete(taskId, taskUserId, null);
        
        logger.debug("Now expecting failure");
        try {
        	taskService.complete(taskId, taskUserId, null);
        	fail( "Should not be able to complete task " + taskId + " a second time.");
        } catch (Throwable t) {
            logger.info("The above exception was an expected part of the test.");
            // do nothing
        }
        
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        List<TaskSummary> taskIds = taskService.getTasksByStatusByProcessInstanceId(processInstance.getId(), statuses, "en-UK");
        assertEquals("Expected 2 tasks.", 2, taskIds.size());
    }
    
    public void executeTaskCommands(URL deploymentUrl, ClientRequestFactory requestFactory, String user, String password) throws Exception {
        RuntimeEngine runtimeEngine = new RemoteRestSessionFactory(deploymentId, deploymentUrl.toExternalForm(), user, password).newRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess("org.jbpm.humantask");
        
        long processInstanceId = processInstance.getId();
        JaxbCommandResponse<?> response = executeTaskCommand(deploymentUrl, requestFactory, deploymentId, new GetTasksByProcessInstanceIdCommand(processInstanceId));
        
        long taskId = ((JaxbLongListResponse) response).getResult().get(0);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", taskUserId);
    }
    
    private JaxbCommandResponse<?> executeTaskCommand(URL deploymentUrl, ClientRequestFactory requestFactory, String deploymentId, Command<?> command) throws Exception {
        List<Command<?>> commands = new ArrayList<Command<?>>();
        commands.add(command);
        
        String urlString = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/runtime/" + deploymentId + "/execute").toExternalForm();
        logger.info("Client request to: " + urlString);
        ClientRequest restRequest = requestFactory.createRequest(urlString);
        
        JaxbCommandsRequest commandMessage = new JaxbCommandsRequest(commands);
        assertNotNull( "Commands are null!", commandMessage.getCommands() );
        assertTrue( "Commands are empty!", commandMessage.getCommands().size() > 0 );
        
        String body = JaxbSerializationProvider.convertJaxbObjectToString(commandMessage);
        restRequest.body(MediaType.APPLICATION_XML, body);

        ClientResponse<JaxbCommandsResponse> responseObj = restRequest.post(JaxbCommandsResponse.class);
        checkResponse(responseObj);
        
        JaxbCommandsResponse cmdsResp = responseObj.getEntity();
        return cmdsResp.getResponses().get(0);
    }
    
    public void restHistoryLogs(URL deploymentUrl, ClientRequestFactory requestFactory) throws Exception {
        String urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/runtime/" + deploymentId + "/process/var-proc/start?map_x=initVal").toExternalForm();
        ClientRequest restRequest = requestFactory.createRequest(urlString);

        // Get and check response
        logger.debug( ">> " + urlString );
        ClientResponse<?> responseObj = checkResponse(restRequest.post());
        JaxbProcessInstanceResponse processInstance = (JaxbProcessInstanceResponse) responseObj.getEntity(JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        urlString = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/runtime/" + deploymentId + "/history/instance/" + procInstId + "/variable/x").toExternalForm();
        restRequest = requestFactory.createRequest(urlString);
        logger.debug( ">> [history/variables]" + urlString );
        responseObj = checkResponse(restRequest.get());
        JaxbHistoryLogList logList = (JaxbHistoryLogList) responseObj.getEntity(JaxbHistoryLogList.class);
        List<AbstractJaxbHistoryObject> varLogList = logList.getHistoryLogList();
        assertEquals("Incorrect number of variable logs", 4, varLogList.size());
        
        for( AbstractJaxbHistoryObject<?> log : logList.getHistoryLogList() ) {
           JaxbVariableInstanceLog varLog = (JaxbVariableInstanceLog) log;
           assertEquals( "Incorrect variable id", "x", varLog.getVariableId() );
           assertEquals( "Incorrect process id", "var-proc", varLog.getProcessId() );
           assertEquals( "Incorrect process instance id", "var-proc", varLog.getProcessId() );
        }
    }
 
    public void restDataServiceCoupling(URL deploymentUrl, ClientRequestFactory requestFactory, String user) throws Exception {
        String urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/runtime/" + deploymentId + "/process/var-proc/start?map_x=initVal").toExternalForm();
        ClientRequest restRequest = requestFactory.createRequest(urlString);

        // Get and check response
        logger.debug( ">> " + urlString );
        ClientResponse<?> responseObj = checkResponse(restRequest.post());
        JaxbProcessInstanceResponse processInstance = (JaxbProcessInstanceResponse) responseObj.getEntity(JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        urlString = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/data/process/instance/" + procInstId ).toExternalForm();
        restRequest = requestFactory.createRequest(urlString);
        logger.debug( ">> [data/process instance]" + urlString );
        responseObj = checkResponse(restRequest.get());
        JaxbProcessInstanceSummary summary = (JaxbProcessInstanceSummary) responseObj.getEntity(JaxbProcessInstanceSummary.class);
        assertEquals("Incorrect initiator.", user, summary.getInitiator());
    }
 
    public void restEscalationEmailSnafu(URL deploymentUrl, ClientRequestFactory requestFactory, String user, String body, String adminUser) throws Exception {
        String urlString = new URL(deploymentUrl,  
                deploymentUrl.getPath() + "rest/runtime/" + deploymentId 
                + "/process/escalation"
                + "/start"
                + "?map_actor=" + user
                + "&map_escalation=" + body
                + "&map_reassignTo=" + adminUser
                + "&map_escUser=" + adminUser
       ).toExternalForm();
        ClientRequest restRequest = requestFactory.createRequest(urlString);
        logger.debug( ">> " + urlString );
        ClientResponse<?> responseObj = checkResponse(restRequest.post());
        JaxbProcessInstanceResponse processInstance = (JaxbProcessInstanceResponse) responseObj.getEntity(JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();
    }
}
