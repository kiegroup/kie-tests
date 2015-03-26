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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.findTaskIdByProcessInstanceId;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.logger;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runHumanTaskGroupIdTest;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runHumanTaskGroupVarAssignTest;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runRemoteApiGroupAssignmentEngineeringTest;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runRuleTaskProcess;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.testExtraJaxbClassSerialization;
import static org.kie.tests.wb.base.util.TestConstants.CLIENT_KEYSTORE_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.CLIENT_KEY_TRUSTSTORE_LOCATION;
import static org.kie.tests.wb.base.util.TestConstants.GROUP_ASSSIGNMENT_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.HUMAN_TASK_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.JOHN_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.JOHN_USER;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.KRIS_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.KRIS_USER;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.base.util.TestConstants.OBJECT_VARIABLE_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.SALA_USER;
import static org.kie.tests.wb.base.util.TestConstants.SCRIPT_TASK_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.SINGLE_HUMAN_TASK_PROCESS_ID;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQJMSConnectionFactory;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.audit.ProcessInstanceLog;
import org.kie.api.runtime.manager.audit.VariableInstanceLog;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.remote.client.api.RemoteJmsRuntimeEngineBuilder;
import org.kie.remote.client.api.RemoteJmsRuntimeEngineFactory;
import org.kie.remote.client.jaxb.ClientJaxbSerializationProvider;
import org.kie.remote.client.jaxb.JaxbCommandsRequest;
import org.kie.remote.client.jaxb.JaxbCommandsResponse;
import org.kie.remote.client.jaxb.JaxbTaskResponse;
import org.kie.remote.client.jaxb.JaxbTaskSummaryListResponse;
import org.kie.remote.jaxb.gen.CompleteTaskCommand;
import org.kie.remote.jaxb.gen.GetProcessInstanceCommand;
import org.kie.remote.jaxb.gen.GetTaskCommand;
import org.kie.remote.jaxb.gen.GetTasksByProcessInstanceIdCommand;
import org.kie.remote.jaxb.gen.GetTasksOwnedCommand;
import org.kie.remote.jaxb.gen.StartProcessCommand;
import org.kie.remote.jaxb.gen.StartTaskCommand;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.services.client.api.command.exception.RemoteApiException;
import org.kie.services.client.serialization.JaxbSerializationProvider;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbLongListResponse;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessInstanceResponse;

public class KieWbJmsIntegrationTestMethods {

    private static final String CONNECTION_FACTORY_NAME = "jms/RemoteConnectionFactory";
    private boolean useSsl = false;
    
    private static final String KSESSION_QUEUE_NAME = "jms/queue/KIE.SESSION";
    private static final String TASK_QUEUE_NAME = "jms/queue/KIE.TASK";
    private static final String RESPONSE_QUEUE_NAME = "jms/queue/KIE.RESPONSE";

    private static final long QUALITY_OF_SERVICE_THRESHOLD_MS = 5 * 1000;

    private final String deploymentId;
    private final InitialContext remoteInitialContext;
    private final JaxbSerializationProvider jaxbSerializationProvider = ClientJaxbSerializationProvider.newInstance();

    public KieWbJmsIntegrationTestMethods(String deploymentId) {
       this(deploymentId, true, false);
    }
    
    public KieWbJmsIntegrationTestMethods(String deploymentId, boolean useSSL) {
       this(deploymentId, true, useSSL);
    }
    
    public KieWbJmsIntegrationTestMethods(String deploymentId, boolean useSSL, Properties initialContextProps) {
       this(deploymentId, true, useSSL, initialContextProps);
    }
    
    public KieWbJmsIntegrationTestMethods(String deploymentId, boolean remote, boolean useSSL) {
       this(deploymentId, remote, useSSL, null); 
    }
    
    public KieWbJmsIntegrationTestMethods(String deploymentId, boolean remote, boolean useSSL, Properties initialContextProps) {
        this.deploymentId = deploymentId;
        this.useSsl = useSSL;
        if( remote ) { 
            if( initialContextProps == null ) { 
                this.remoteInitialContext = getJbossRemoteInitialContext(MARY_USER, MARY_PASSWORD);
            } else { 
                this.remoteInitialContext = getRemoteInitialContext(MARY_USER, MARY_PASSWORD, initialContextProps);
            }
        } else { 
            this.remoteInitialContext = null;
        }
    }

    // Helper methods ------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a (remote) IntialContext instance.
     * 
     * @return a remote {@link InitialContext} instance
     */
    private static InitialContext getJbossRemoteInitialContext(String user, String password) {
        Properties initialProps = new Properties();
        initialProps.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        initialProps.setProperty(InitialContext.PROVIDER_URL, "remote://localhost:4447");
        initialProps.setProperty(InitialContext.SECURITY_PRINCIPAL, user);
        initialProps.setProperty(InitialContext.SECURITY_CREDENTIALS, password);
        return getRemoteInitialContext(user, password, initialProps);
    }

    public static InitialContext getRemoteInitialContext(String user, String password, Properties initialProps) {
        for (Object keyObj : initialProps.keySet()) {
            String key = (String) keyObj;
            System.setProperty(key, (String) initialProps.get(key));
        }
        try {
            return new InitialContext(initialProps);
        } catch (NamingException e) {
            throw new RuntimeException("Unable to create " + InitialContext.class.getSimpleName(), e);
        }
    }

    private JaxbCommandsResponse sendJmsJaxbCommandsRequest(String sendQueueName, JaxbCommandsRequest req, String USER,
            String PASSWORD) throws Exception {
        ConnectionFactory factory;
        if( ! useSsl ) { 
            factory = (ConnectionFactory) remoteInitialContext.lookup(CONNECTION_FACTORY_NAME);
        } else { 
            Map<String, Object> connParams = new HashMap<String, Object>();  
            connParams.put(TransportConstants.PORT_PROP_NAME, 5446);  
            connParams.put(TransportConstants.HOST_PROP_NAME, "127.0.0.1");  
            // SSL
            connParams.put(org.hornetq.core.remoting.impl.netty.TransportConstants.SSL_ENABLED_PROP_NAME, true);  
            connParams.put(TransportConstants.KEYSTORE_PASSWORD_PROP_NAME, "CLIENT_KEYSTORE_PASSWORD");  
            connParams.put(TransportConstants.KEYSTORE_PATH_PROP_NAME, "ssl/client_keystore.jks");  
      
            factory = new HornetQJMSConnectionFactory(false, 
                    new TransportConfiguration(NettyConnectorFactory.class.getName(), connParams));
        }
        Queue jbpmQueue = (Queue) remoteInitialContext.lookup(sendQueueName);
        Queue responseQueue = (Queue) remoteInitialContext.lookup(RESPONSE_QUEUE_NAME);

        Connection connection = null;
        Session session = null;
        JaxbCommandsResponse cmdResponse = null;
        try {
            // setup
            connection = factory.createConnection(USER, PASSWORD);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageProducer producer = session.createProducer(jbpmQueue);
            String corrId = UUID.randomUUID().toString();
            String selector = "JMSCorrelationID = '" + corrId + "'";
            MessageConsumer consumer = session.createConsumer(responseQueue, selector);

            connection.start();

            // Create msg
            String xmlStr = jaxbSerializationProvider.serialize(req);
            TextMessage msg = session.createTextMessage(xmlStr);
            msg.setJMSCorrelationID(corrId);
            msg.setIntProperty("serialization", JaxbSerializationProvider.JMS_SERIALIZATION_TYPE);
            msg.setStringProperty("username", MARY_USER);
            msg.setStringProperty("password", MARY_PASSWORD);

            // send
            producer.send(msg);

            // receive
            Message response = consumer.receive(QUALITY_OF_SERVICE_THRESHOLD_MS);

            // check
            assertNotNull("Response is empty.", response);
            assertEquals("Correlation id not equal to request msg id.", corrId, response.getJMSCorrelationID());
            assertNotNull("Response from MDB was null!", response);
            xmlStr = ((TextMessage) response).getText();
            cmdResponse = (JaxbCommandsResponse) jaxbSerializationProvider.deserialize(xmlStr);
            assertNotNull("Jaxb Cmd Response was null!", cmdResponse);
        } finally {
            if (connection != null) {
                connection.close();
                if( session != null ) { 
                    session.close();
                }
            }
        }
        return cmdResponse;
    }

    // Tests ----------------------------------------------------------------------------------------------------------------------

    public void sendEmptyRequest(String user, String password) throws Exception {
        JaxbCommandsRequest req = new JaxbCommandsRequest();
        req.setDeploymentId(deploymentId);
        sendJmsJaxbCommandsRequest(KSESSION_QUEUE_NAME, req, user, password);
    }
    
    public void commandsSimpleStartProcess(String user, String password) throws Exception {
        // send cmd: start process
        StartProcessCommand cmd = new StartProcessCommand();
        cmd.setProcessId(SCRIPT_TASK_PROCESS_ID);
        JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
        sendJmsJaxbCommandsRequest(KSESSION_QUEUE_NAME, req, user, password);
    }
    
    public void commandsStartProcess(String user, String password) throws Exception {
        // send cmd: start process
        JaxbCommandsResponse response;
        {
            StartProcessCommand cmd = new StartProcessCommand();
            cmd.setProcessId(HUMAN_TASK_PROCESS_ID);
            JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
            response = sendJmsJaxbCommandsRequest(KSESSION_QUEUE_NAME, req, user, password);
        }

        // check response
        assertNotNull("response was null.", response);
        assertTrue("response did not contain any command responses", response.getResponses() != null
                && response.getResponses().size() > 0);
        JaxbCommandResponse<?> cmdResponse = response.getResponses().get(0);
        assertTrue("response is not the proper class type : " + cmdResponse.getClass().getSimpleName(),
                cmdResponse instanceof JaxbProcessInstanceResponse);
        ProcessInstance procInst = (ProcessInstance) cmdResponse;
        long procInstId = procInst.getId();

        // send cmd
        {
            GetTasksByProcessInstanceIdCommand cmd = new GetTasksByProcessInstanceIdCommand();
            cmd.setProcessInstanceId(procInstId);
            JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
            response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);
        }

        // check response
        assertNotNull("response was null.", response);
        assertTrue("response did not contain any command responses", response.getResponses() != null
                && response.getResponses().size() > 0);
        cmdResponse = response.getResponses().get(0);
        assertTrue("response is not the proper class type : " + cmdResponse.getClass().getSimpleName(),
                cmdResponse instanceof JaxbLongListResponse);
        long taskId = ((JaxbLongListResponse) cmdResponse).getResult().get(0);

        // send cmd
        {
            StartTaskCommand cmd = new StartTaskCommand();
            cmd.setTaskId(taskId);
            cmd.setUserId(SALA_USER);
            JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
            CompleteTaskCommand cmd2 = new CompleteTaskCommand();
            cmd2.setTaskId(taskId);
            cmd2.setUserId(SALA_USER);
            req.getCommands().add(cmd2);
            response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);
        }

        // check response
        assertNotNull("response was null.", response);
        assertTrue("response list was not empty", response.getResponses().size() == 0);

        // send cmd
        {
            GetTasksOwnedCommand cmd = new GetTasksOwnedCommand();
            cmd.setUserId(SALA_USER);
            JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
            GetTasksOwnedCommand cmd2 = new GetTasksOwnedCommand();
            cmd2.setUserId("bob");
            req.getCommands().add(cmd2);
            GetProcessInstanceCommand cmd3 = new GetProcessInstanceCommand();
            cmd3.setProcessInstanceId(procInstId);
            req.getCommands().add(cmd3);
            response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);
        }

        assertNotNull("response was null.", response);
        assertTrue("response did not contain any command responses", response.getResponses() != null
                && response.getResponses().size() > 0);
        cmdResponse = response.getResponses().get(0);
        assertTrue("response is not the proper class type : " + cmdResponse.getClass().getSimpleName(),
                cmdResponse instanceof JaxbTaskSummaryListResponse);
        List<TaskSummary> taskSummaries = ((JaxbTaskSummaryListResponse) cmdResponse).getResult();
        assertTrue("task summary list is empty", taskSummaries.size() > 0);
        for (TaskSummary taskSum : taskSummaries) {
            if (taskSum.getId() == taskId) {
                assertTrue("Task " + taskId + " should have completed.", taskSum.getStatus().equals(Status.Completed));
            }
        }

        cmdResponse = response.getResponses().get(1);
        assertTrue("response is not the proper class type : " + cmdResponse.getClass().getSimpleName(),
                cmdResponse instanceof JaxbTaskSummaryListResponse);
        taskSummaries = ((JaxbTaskSummaryListResponse) cmdResponse).getResult();
        assertTrue("task summary list should be empty, but has " + taskSummaries.size() + " elements", taskSummaries.size() == 0);
        cmdResponse = response.getResponses().get(2);
        assertNotNull(cmdResponse);
    }

    public void remoteApiHumanTaskProcess(String user, String password) throws Exception {
        String queueName = KSESSION_QUEUE_NAME;
        Queue sessionQueue = (Queue) remoteInitialContext.lookup(queueName);
        queueName = TASK_QUEUE_NAME;
        Queue taskQueue = (Queue) remoteInitialContext.lookup(queueName);
        queueName = RESPONSE_QUEUE_NAME;
        Queue responseQueue = (Queue) remoteInitialContext.lookup(queueName);

        String connFactoryName = CONNECTION_FACTORY_NAME;
        ConnectionFactory connFact = (ConnectionFactory) remoteInitialContext.lookup(connFactoryName);
        
        RuntimeEngine engine = RemoteRuntimeEngineFactory.newJmsBuilder()
                .addDeploymentId(deploymentId)
                .addConnectionFactory(connFact)
                .addKieSessionQueue(sessionQueue)
                .addTaskServiceQueue(taskQueue)
                .addResponseQueue(responseQueue)
                .addUserName(user)
                .addPassword(password)
                .addHostName("localhost")
                .addJmsConnectorPort(5446)
                .useKeystoreAsTruststore()
                .addKeystoreLocation(CLIENT_KEY_TRUSTSTORE_LOCATION)
                .addKeystorePassword(CLIENT_KEYSTORE_PASSWORD)
                .build();

        // create JMS request
        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        assertNotNull( "Null process instance!", processInstance);

        logger.debug("Started process instance: " + processInstance + " "
                + (processInstance == null ? "" : processInstance.getId()));

        TaskService taskService = engine.getTaskService();
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(SALA_USER, "en-UK");
        long taskId = findTaskIdByProcessInstanceId(processInstance.getId(), tasks);

        logger.debug("Found task " + taskId);
        Task task = taskService.getTaskById(taskId);
        logger.debug("Got task " + taskId + ": " + task);
        taskService.start(taskId, SALA_USER);
        taskService.complete(taskId, SALA_USER, null);

        logger.debug("Now expecting failure");
        try {
            taskService.complete(taskId, SALA_USER, null);
            fail("Should not have been able to complete task " + taskId + " a second time.");
        } catch (Throwable t) {
            // do nothing
        }

        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        List<TaskSummary> taskIds = taskService.getTasksByStatusByProcessInstanceId(processInstance.getId(), statuses, "en-UK");
        assertEquals("Expected 2 tasks.", 2, taskIds.size());
    }

    public void remoteApiException(String user, String password) throws Exception {
        RuntimeEngine engine = RemoteRuntimeEngineFactory.newJmsBuilder()
                .addDeploymentId("non-existing-deployment")
                .addRemoteInitialContext(remoteInitialContext)
                .addUserName(user)
                .addPassword(password)
                .build();

        // create JMS request
        KieSession ksession = engine.getKieSession();
        try {
            ksession.startProcess(HUMAN_TASK_PROCESS_ID);
            fail("startProcess should fail!");
        } catch (RemoteApiException rae) {
            String errMsg = rae.getMessage();
            assertTrue("Incorrect error message: " + errMsg, errMsg.contains("No deployments"));
        }
    }

    public void remoteApiNoProcessInstanceFound(String user, String password) throws Exception {
        RuntimeEngine engine = RemoteRuntimeEngineFactory.newJmsBuilder()
                .addDeploymentId(deploymentId)
                .addRemoteInitialContext(remoteInitialContext)
                .addUserName(user)
                .addPassword(password)
                .build();

        // create JMS request
        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess(SCRIPT_TASK_PROCESS_ID);
        assertNotNull( "Null process instance!", processInstance);
        
        logger.debug("Started process instance: " + processInstance + " "
                + (processInstance == null ? "" : processInstance.getId()));

        GetProcessInstanceCommand cmd = new GetProcessInstanceCommand();
        cmd.setProcessInstanceId(processInstance.getId());
        JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
        JaxbCommandsResponse response = sendJmsJaxbCommandsRequest(KSESSION_QUEUE_NAME, req, user, password);
        assertNotNull("Response should not be null.", response);
        assertEquals("Size of response list", response.getResponses().size(), 0);
    }

    public void remoteApiAndCommandsCompleteSimpleHumanTask(String user, String password) throws Exception {
        // Via the remote api

        // setup
        RuntimeEngine engine = RemoteJmsRuntimeEngineFactory.newBuilder()
                .addDeploymentId(deploymentId)
                .addRemoteInitialContext(remoteInitialContext)
                .addUserName(user)
                .addPassword(password)
                .addHostName("localhost")
                .addJmsConnectorPort(5446)
                .addKeystoreLocation(CLIENT_KEY_TRUSTSTORE_LOCATION)
                .addKeystorePassword(CLIENT_KEYSTORE_PASSWORD)
                .addTruststoreLocation(CLIENT_KEY_TRUSTSTORE_LOCATION)
                .addTruststorePassword(CLIENT_KEYSTORE_PASSWORD)
                .build();
        
        KieSession ksession = engine.getKieSession();

        // start process
        ProcessInstance processInstance = ksession.startProcess(SINGLE_HUMAN_TASK_PROCESS_ID);
        assertNotNull( "Null process instance!", processInstance);
        long procInstId = processInstance.getId();

        TaskService taskService = engine.getTaskService();
        List<Long> tasks = taskService.getTasksByProcessInstanceId(procInstId);
        assertEquals("Only expected 1 task for this process instance", 1, tasks.size());
        long taskId = tasks.get(0);
        assertNotNull("Null task!", taskService.getTaskById(taskId));

        String userId = "admin";
        taskService.start(taskId, userId);
        taskService.complete(taskId, userId, null);

        processInstance = ksession.getProcessInstance(procInstId);
        assertNull(processInstance);

        // Via the JaxbCommandsRequest
        // send cmd
        JaxbCommandsResponse response;
        {
            StartProcessCommand cmd = new StartProcessCommand();
            cmd.setProcessId(SINGLE_HUMAN_TASK_PROCESS_ID);
            JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
            response = sendJmsJaxbCommandsRequest(KSESSION_QUEUE_NAME, req, user, password);
        }

        // check response
        assertNotNull("response was null.", response);
        assertTrue("response did not contain any command responses", response.getResponses() != null
                && response.getResponses().size() > 0);
        JaxbCommandResponse<?> cmdResponse = response.getResponses().get(0);
        assertTrue("response is not the proper class type : " + cmdResponse.getClass().getSimpleName(),
                cmdResponse instanceof JaxbProcessInstanceResponse);
        ProcessInstance procInst = (ProcessInstance) cmdResponse;
        procInstId = procInst.getId();

        // send cmd
        {
            GetTasksByProcessInstanceIdCommand cmd = new GetTasksByProcessInstanceIdCommand();
            cmd.setProcessInstanceId(procInstId);
            JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
            response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);
        }

        // check response
        assertNotNull("response was null.", response);
        assertTrue("response did not contain any command responses", response.getResponses() != null
                && response.getResponses().size() > 0);
        cmdResponse = response.getResponses().get(0);
        assertTrue("response is not the proper class type : " + cmdResponse.getClass().getSimpleName(),
                cmdResponse instanceof JaxbLongListResponse);
        taskId = ((JaxbLongListResponse) cmdResponse).getResult().get(0);

        // send cmd
        { 
            GetTaskCommand cmd = new GetTaskCommand();
            cmd.setTaskId(taskId);
            JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
            response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);
        }

        // check response
        assertNotNull("response was null.", response);
        assertTrue("response did not contain any command responses", response.getResponses() != null
                && response.getResponses().size() > 0);
        cmdResponse = response.getResponses().get(0);
        assertTrue("response is not the proper class type : " + cmdResponse.getClass().getSimpleName(),
                cmdResponse instanceof JaxbTaskResponse);
        Task task = ((JaxbTaskResponse) cmdResponse).getResult();
        assertNotNull("task was null.", task);

        // send cmd
        /**
         * cmd = new GetContentCommand(task.getTaskData().getDocumentContentId());
         * req = new JaxbCommandsRequest(deploymentId, cmd);
         * response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);
         * 
         * // check response
         * assertNotNull("response was null.", response);
         * assertTrue("response did not contain any command responses", response.getResponses() != null &&
         * response.getResponses().size() > 0);
         * cmdResponse = response.getResponses().get(0);
         * assertTrue( "response is not the proper class type : " + cmdResponse.getClass().getSimpleName(), cmdResponse instanceof
         * JaxbTaskResponse );
         * Task task = ((JaxbTaskResponse) cmdResponse).getResult();
         * assertNotNull("task was null.", task);
         **/

        // send cmd
        {
            StartTaskCommand cmd = new StartTaskCommand();
            cmd.setTaskId(taskId);
            cmd.setUserId(userId);
            JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
            CompleteTaskCommand cmd2 = new CompleteTaskCommand();
            cmd2.setTaskId(taskId);
            cmd2.setUserId(userId);
            req.getCommands().add(cmd2);
            response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);
        }

        // check response
        assertNotNull("response was null.", response);

        // send cmd
        {
            GetProcessInstanceCommand cmd = new GetProcessInstanceCommand();
            cmd.setProcessInstanceId(procInstId);
            JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
            response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);
        }

        // check response
        assertEquals("Process instance did not complete..", 0, response.getResponses().size());
    }

    public void remoteApiExtraJaxbClasses(String user, String password) throws Exception {
        // Remote API setup
        RuntimeEngine runtimeEngine = RemoteRuntimeEngineFactory.newJmsBuilder()
                .addDeploymentId(deploymentId)
                .addRemoteInitialContext(remoteInitialContext)
                .addUserName(user)
                .addPassword(password)
                .build();

        testExtraJaxbClassSerialization(runtimeEngine);
    }

    public void remoteApiRuleTaskProcess(String user, String password) {
        // setup
        RuntimeEngine runtimeEngine = RemoteRuntimeEngineFactory.newJmsBuilder()
                .addDeploymentId(deploymentId)
                .addRemoteInitialContext(remoteInitialContext)
                .addUserName(user)
                .addPassword(password)
                .build();

        runRuleTaskProcess(runtimeEngine.getKieSession(), runtimeEngine.getAuditService());
    }

    public void remoteApiInitiatorIdentityTest(String user, String password) {
        // setup
        RuntimeEngine runtimeEngine = RemoteRuntimeEngineFactory.newJmsBuilder()
                .addDeploymentId(deploymentId)
                .addRemoteInitialContext(remoteInitialContext)
                .addUserName(user)
                .addPassword(password)
                .build();


        KieSession ksession = runtimeEngine.getKieSession();
        ProcessInstance procInst = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        assertNotNull( "Null process instance!", procInst);
        long procId = procInst.getId();

        List<ProcessInstanceLog> procLogs = (List<ProcessInstanceLog>) runtimeEngine.getAuditService().findActiveProcessInstances(HUMAN_TASK_PROCESS_ID);
        boolean procLogFound = false;
        for (ProcessInstanceLog log : procLogs) {
            if (log == null) {
                continue;
            }
            if (log.getProcessInstanceId() == procId) {
                procLogFound = true;
                assertNotEquals("The identity should not be unknown!", "unknown", log.getIdentity());
            }
        }
        assertTrue("Process instance log could not be found.", procLogFound);
    }

    public void remoteApiStartScriptProcess(String user, String password) {
        // setup
        RemoteJmsRuntimeEngineFactory remoteSessionFactory 
            = RemoteJmsRuntimeEngineFactory.newBuilder()
            .addDeploymentId(deploymentId)
            .addRemoteInitialContext(remoteInitialContext)
            .addUserName(user)
            .addPassword(password)
            .buildFactory();

        RuntimeEngine runtimeEngine = remoteSessionFactory.newRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();

        // start process
        ProcessInstance procInst = ksession.startProcess(SCRIPT_TASK_PROCESS_ID);
        int procStatus = procInst.getState();

        assertEquals("Incorrect process status: " + procStatus, ProcessInstance.STATE_COMPLETED, procStatus);
    }

    public void remoteApiHumanTaskGroupIdTest(URL deploymentUrl) {
        RemoteJmsRuntimeEngineBuilder jreBuilder 
            = RemoteJmsRuntimeEngineFactory.newBuilder();

            jreBuilder
                .addDeploymentId(deploymentId)
                .useSsl(true)
                .addHostName("localhost")
                .addJmsConnectorPort(5446)
                .addKeystoreLocation("ssl/client_keystore.jks")
                .addKeystorePassword("CLIENT_KEYSTORE_PASSWORD")
                .useKeystoreAsTruststore();
               
        try { 
            jreBuilder
            .addTaskServiceQueue((Queue) remoteInitialContext.lookup(TASK_QUEUE_NAME))
            .addKieSessionQueue((Queue) remoteInitialContext.lookup(KSESSION_QUEUE_NAME))
            .addResponseQueue((Queue) remoteInitialContext.lookup(RESPONSE_QUEUE_NAME));
        } catch( Exception e ) { 
            String msg = "Unable to lookup queue instances: " + e.getMessage();
            logger.error(msg, e);
            fail(msg);
        }

        RuntimeEngine krisRuntimeEngine = jreBuilder
                .addUserName(KRIS_USER)
                .addPassword(KRIS_PASSWORD)
                .build();

        RuntimeEngine maryRuntimeEngine = jreBuilder
                .addUserName(MARY_USER)
                .addPassword(MARY_PASSWORD)
                .build();

        RuntimeEngine johnRuntimeEngine = jreBuilder
                .addUserName(JOHN_USER)
                .addPassword(JOHN_PASSWORD)
                .build();

        runHumanTaskGroupIdTest(krisRuntimeEngine, johnRuntimeEngine, maryRuntimeEngine);
    }

    public void remoteApiGroupAssignmentEngineeringTest(RuntimeEngine runtimeEngine) throws Exception {
        KieSession ksession = runtimeEngine.getKieSession();
        TaskService taskService = runtimeEngine.getTaskService();

        ProcessInstance pi = ksession.startProcess(GROUP_ASSSIGNMENT_PROCESS_ID, null);
        assertNotNull(pi);
        assertEquals(ProcessInstance.STATE_ACTIVE, pi.getState());

        // assert the task
        TaskSummary taskSummary = getTaskSummary(taskService, pi.getId(), Status.Ready);
        assertNull(taskSummary.getActualOwner());
        assertNull(taskSummary.getPotentialOwners());
        assertEquals("Task 1", taskSummary.getName());

        // complete 'Task 1' as mary
        taskService.claim(taskSummary.getId(), MARY_USER);
        taskService.start(taskSummary.getId(), MARY_USER);
        taskService.complete(taskSummary.getId(), MARY_USER, null);

        // now make sure that the next task has been assigned to the
        // correct person. it should be mary.
        taskSummary = getTaskSummary(taskService, pi.getId(), Status.Reserved);
        assertNotNull( "No task found for Mary", taskSummary);
        assertEquals("Task 2", taskSummary.getName());
        assertEquals(MARY_USER, taskSummary.getActualOwner().getId());

        // complete 'Task 2' as john
        taskService.release(taskSummary.getId(), MARY_USER);
        taskService.claim(taskSummary.getId(), JOHN_USER);
        taskService.start(taskSummary.getId(), JOHN_USER);
        taskService.complete(taskSummary.getId(), JOHN_USER, null);

        // now make sure that the next task has been assigned to the
        // correct person. it should be john.
        taskSummary = getTaskSummary(taskService, pi.getId(), Status.Reserved);
        assertEquals("Task 3", taskSummary.getName());
        assertEquals(JOHN_USER, taskSummary.getActualOwner().getId());

        // complete 'Task 3' as john
        taskService.start(taskSummary.getId(), JOHN_USER);
        taskService.complete(taskSummary.getId(), JOHN_USER, null);

        // assert process finished
        pi = ksession.getProcessInstance(pi.getId());
        assertNull(pi);
    }

    private TaskSummary getTaskSummary(TaskService taskService, long procInstId, Status status) {
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(status);
        List<TaskSummary> taskSumList = taskService.getTasksByStatusByProcessInstanceId(procInstId, statuses, "en-UK");
        TaskSummary result = null;
        for (TaskSummary krisTask : taskSumList) {
            if (krisTask.getProcessInstanceId() == procInstId) {
                result = krisTask;
            }
        }
        return result;
    }
    
    public void remoteApiHistoryVariablesTest(URL deploymentUrl) { 
        RemoteJmsRuntimeEngineBuilder jreBuilder = RemoteJmsRuntimeEngineFactory.newBuilder()
                .addDeploymentId(deploymentId)
                .addJbossServerHostName(deploymentUrl.getHost())
                .useSsl(false)
                .addUserName(JOHN_USER)
                .addPassword(JOHN_PASSWORD);
        
        RuntimeEngine runtimeEngine = jreBuilder.build();
      
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("myobject", 10l);
        runtimeEngine.getKieSession().startProcess(OBJECT_VARIABLE_PROCESS_ID, params);
        
        List<VariableInstanceLog> viLogs = (List<VariableInstanceLog>) runtimeEngine.getAuditService().findVariableInstancesByName("myobject", false);
        assertNotNull( "Null variable instance log list", viLogs);
        logger.info("vi logs: " + viLogs.size());
        assertTrue( "Variable instance log list is empty", ! viLogs.isEmpty() );
    }

    public void remoteApiGroupAssignmentEngineeringTest(URL deploymentUrl) throws Exception {
        RuntimeEngine runtimeEngine 
            = RemoteJmsRuntimeEngineFactory.newBuilder()
            .addJbossServerHostName(deploymentUrl.getHost())
            .addDeploymentId(KJAR_DEPLOYMENT_ID)
            .useSsl(true)
            .addHostName("localhost")
            .addJmsConnectorPort(5446)
            .addKeystoreLocation("ssl/client_keystore.jks")
            .addKeystorePassword("CLIENT_KEYSTORE_PASSWORD")
            .useKeystoreAsTruststore()
            .addUserName(MARY_USER)
            .addPassword(MARY_PASSWORD)
            .build();
        
        RuntimeEngine johnRuntimeEngine 
            = RemoteJmsRuntimeEngineFactory.newBuilder()
            .addJbossServerHostName(deploymentUrl.getHost())
            .addDeploymentId(KJAR_DEPLOYMENT_ID)
            .useSsl(true)
            .addHostName("localhost")
            .addJmsConnectorPort(5446)
            .addKeystoreLocation("ssl/client_keystore.jks")
            .addKeystorePassword("CLIENT_KEYSTORE_PASSWORD")
            .useKeystoreAsTruststore()
            .addUserName(JOHN_USER)
            .addPassword(JOHN_PASSWORD)
            .build();
        
        runRemoteApiGroupAssignmentEngineeringTest(runtimeEngine, johnRuntimeEngine);
    }
    
    public void remoteApiHumanTaskGroupVarAssignTest( URL deploymentUrl ) {
        // @formatter:off
        RuntimeEngine runtimeEngine 
            = RemoteRuntimeEngineFactory.newRestBuilder()
                .addDeploymentId(deploymentId)
                .addUserName(MARY_USER)
                .addPassword(MARY_PASSWORD)
                .addUrl(deploymentUrl)
                .build();
        // @formatter:on
        runHumanTaskGroupVarAssignTest(runtimeEngine, MARY_USER, "HR");
    }
}
