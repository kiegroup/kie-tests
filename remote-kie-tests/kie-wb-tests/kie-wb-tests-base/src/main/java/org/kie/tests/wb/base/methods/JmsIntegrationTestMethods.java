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
import static org.kie.tests.wb.base.methods.TestConstants.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.drools.core.command.runtime.process.GetProcessInstanceCommand;
import org.drools.core.command.runtime.process.StartProcessCommand;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQJMSConnectionFactory;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.services.task.commands.CompleteTaskCommand;
import org.jbpm.services.task.commands.GetTaskCommand;
import org.jbpm.services.task.commands.GetTasksByProcessInstanceIdCommand;
import org.jbpm.services.task.commands.GetTasksOwnedCommand;
import org.jbpm.services.task.commands.StartTaskCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.services.client.api.RemoteJmsRuntimeEngineFactory;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.services.client.api.command.RemoteRuntimeEngine;
import org.kie.services.client.api.command.exception.RemoteApiException;
import org.kie.services.client.serialization.JaxbSerializationProvider;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandsRequest;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandsResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbLongListResponse;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessInstanceResponse;
import org.kie.services.client.serialization.jaxb.impl.task.JaxbTaskResponse;
import org.kie.services.client.serialization.jaxb.impl.task.JaxbTaskSummaryListResponse;

public class JmsIntegrationTestMethods extends AbstractIntegrationTestMethods {

    private static final String SSL_CONNECTION_FACTORY_NAME = "jms/SslRemoteConnectionFactory";
    private static final String CONNECTION_FACTORY_NAME = "jms/RemoteConnectionFactory";
    private boolean useSsl = false;
    
    private static final String KSESSION_QUEUE_NAME = "jms/queue/KIE.SESSION";
    private static final String TASK_QUEUE_NAME = "jms/queue/KIE.TASK";
    private static final String RESPONSE_QUEUE_NAME = "jms/queue/KIE.RESPONSE";

    private String getConnectionFactoryName() { 
       if( useSsl ) { 
           return SSL_CONNECTION_FACTORY_NAME;
       } 
       return CONNECTION_FACTORY_NAME;
    }
    
    private static final long QUALITY_OF_SERVICE_THRESHOLD_MS = 5 * 1000;

    private final String deploymentId;
    private final InitialContext remoteInitialContext;
    private final JaxbSerializationProvider jaxbSerializationProvider = new JaxbSerializationProvider();

    public JmsIntegrationTestMethods(String deploymentId) {
       this(deploymentId, true, false);
    }
    
    public JmsIntegrationTestMethods(String deploymentId, boolean useSSL) {
       this(deploymentId, true, useSSL);
    }
    
    public JmsIntegrationTestMethods(String deploymentId, boolean remote, boolean useSSL) {
        this.deploymentId = deploymentId;
        this.useSsl = useSSL;
        if( remote ) { 
            this.remoteInitialContext = getRemoteInitialContext(MARY_USER, MARY_PASSWORD);
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
    private static InitialContext getRemoteInitialContext(String user, String password) {
        Properties initialProps = new Properties();
        initialProps.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        initialProps.setProperty(InitialContext.PROVIDER_URL, "remote://localhost:4447");
        initialProps.setProperty(InitialContext.SECURITY_PRINCIPAL, user);
        initialProps.setProperty(InitialContext.SECURITY_CREDENTIALS, password);

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
            BytesMessage msg = session.createBytesMessage();
            msg.setJMSCorrelationID(corrId);
            msg.setIntProperty("serialization", JaxbSerializationProvider.JMS_SERIALIZATION_TYPE);
            msg.setStringProperty("username", MARY_USER);
            msg.setStringProperty("password", MARY_PASSWORD);
            String xmlStr = jaxbSerializationProvider.serialize(req);
            msg.writeUTF(xmlStr);

            // send
            producer.send(msg);

            // receive
            Message response = consumer.receive(QUALITY_OF_SERVICE_THRESHOLD_MS);

            // check
            assertNotNull("Response is empty.", response);
            assertEquals("Correlation id not equal to request msg id.", corrId, response.getJMSCorrelationID());
            assertNotNull("Response from MDB was null!", response);
            xmlStr = ((BytesMessage) response).readUTF();
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

    public void commandsSimpleStartProcess(String user, String password) throws Exception {
        // send cmd: start process
        Command<?> cmd = new StartProcessCommand(SCRIPT_TASK_PROCESS_ID);
        JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
        sendJmsJaxbCommandsRequest(KSESSION_QUEUE_NAME, req, user, password);
    }
    
    public void commandsStartProcess(String user, String password) throws Exception {
        // send cmd: start process
        Command<?> cmd = new StartProcessCommand(HUMAN_TASK_PROCESS_ID);
        JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
        JaxbCommandsResponse response = sendJmsJaxbCommandsRequest(KSESSION_QUEUE_NAME, req, user, password);

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
        cmd = new GetTasksByProcessInstanceIdCommand(procInstId);
        req = new JaxbCommandsRequest(deploymentId, cmd);
        response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);

        // check response
        assertNotNull("response was null.", response);
        assertTrue("response did not contain any command responses", response.getResponses() != null
                && response.getResponses().size() > 0);
        cmdResponse = response.getResponses().get(0);
        assertTrue("response is not the proper class type : " + cmdResponse.getClass().getSimpleName(),
                cmdResponse instanceof JaxbLongListResponse);
        long taskId = ((JaxbLongListResponse) cmdResponse).getResult().get(0);

        // send cmd
        cmd = new StartTaskCommand(taskId, SALA_USER);
        req = new JaxbCommandsRequest(deploymentId, cmd);
        req.getCommands().add(new CompleteTaskCommand(taskId, SALA_USER, null));
        response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);

        // check response
        assertNotNull("response was null.", response);
        assertTrue("response list was not empty", response.getResponses().size() == 0);

        // send cmd
        cmd = new GetTasksOwnedCommand(SALA_USER, "en-UK");
        req = new JaxbCommandsRequest(deploymentId, cmd);
        req.getCommands().add(new GetTasksOwnedCommand("bob", "fr-CA"));
        req.getCommands().add(new GetProcessInstanceCommand(procInstId));
        response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);

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
        RemoteJmsRuntimeEngineFactory remoteSessionFactory = new RemoteJmsRuntimeEngineFactory(deploymentId, remoteInitialContext,
                user, password);

        // create JMS request
        RuntimeEngine engine = remoteSessionFactory.newRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        assertNotNull( "Null process instance!", processInstance);

        logger.debug("Started process instance: " + processInstance + " "
                + (processInstance == null ? "" : processInstance.getId()));

        TaskService taskService = engine.getTaskService();
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(SALA_USER, "en-UK");
        long taskId = findTaskId(processInstance.getId(), tasks);

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
        RemoteJmsRuntimeEngineFactory remoteSessionFactory = new RemoteJmsRuntimeEngineFactory("non-existing-deployment",
                remoteInitialContext, user, password);

        // create JMS request
        RuntimeEngine engine = remoteSessionFactory.newRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        try {
            ksession.startProcess(HUMAN_TASK_PROCESS_ID);
            fail("startProcess should fail!");
        } catch (RemoteApiException rae) {
            String errMsg = rae.getMessage();
            assertTrue("Incorrect error message: " + errMsg, errMsg.contains("DeploymentNotFoundException"));
        }
    }

    public void remoteApiNoProcessInstanceFound(String user, String password) throws Exception {
        RemoteJmsRuntimeEngineFactory remoteSessionFactory = new RemoteJmsRuntimeEngineFactory(deploymentId, remoteInitialContext,
                user, password);

        // create JMS request
        RuntimeEngine engine = remoteSessionFactory.newRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess(SCRIPT_TASK_PROCESS_ID);
        assertNotNull( "Null process instance!", processInstance);
        
        logger.debug("Started process instance: " + processInstance + " "
                + (processInstance == null ? "" : processInstance.getId()));

        Command<?> cmd = new GetProcessInstanceCommand(processInstance.getId());
        JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
        JaxbCommandsResponse response = sendJmsJaxbCommandsRequest(KSESSION_QUEUE_NAME, req, user, password);
        assertNotNull("Response should not be null.", response);
        assertEquals("Size of response list", response.getResponses().size(), 0);
    }

    public void remoteApiAndCommandsCompleteSimpleHumanTask(String user, String password) throws Exception {
        // Via the remote api

        // setup
        RemoteJmsRuntimeEngineFactory remoteSessionFactory = new RemoteJmsRuntimeEngineFactory(deploymentId, remoteInitialContext,
                user, password);
        RuntimeEngine engine = remoteSessionFactory.newRuntimeEngine();
        KieSession ksession = engine.getKieSession();

        // start process
        ProcessInstance processInstance = ksession.startProcess(SINGLE_HUMAN_TASK_PROCESS_ID);
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
        Command<?> cmd = new StartProcessCommand(SINGLE_HUMAN_TASK_PROCESS_ID);
        JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, cmd);
        JaxbCommandsResponse response = sendJmsJaxbCommandsRequest(KSESSION_QUEUE_NAME, req, user, password);

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
        cmd = new GetTasksByProcessInstanceIdCommand(procInstId);
        req = new JaxbCommandsRequest(deploymentId, cmd);
        response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);

        // check response
        assertNotNull("response was null.", response);
        assertTrue("response did not contain any command responses", response.getResponses() != null
                && response.getResponses().size() > 0);
        cmdResponse = response.getResponses().get(0);
        assertTrue("response is not the proper class type : " + cmdResponse.getClass().getSimpleName(),
                cmdResponse instanceof JaxbLongListResponse);
        taskId = ((JaxbLongListResponse) cmdResponse).getResult().get(0);

        // send cmd
        cmd = new GetTaskCommand(taskId);
        req = new JaxbCommandsRequest(deploymentId, cmd);
        response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);

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
        cmd = new StartTaskCommand(taskId, userId);
        req = new JaxbCommandsRequest(deploymentId, cmd);
        req.getCommands().add(new CompleteTaskCommand(taskId, userId, null));
        response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);

        // check response
        assertNotNull("response was null.", response);

        // send cmd
        cmd = new GetProcessInstanceCommand(procInstId);
        req = new JaxbCommandsRequest(deploymentId, cmd);
        response = sendJmsJaxbCommandsRequest(TASK_QUEUE_NAME, req, user, password);

        // check response
        assertEquals("Process instance did not complete..", 0, response.getResponses().size());
    }

    public void remoteApiExtraJaxbClasses(String user, String password) throws Exception {
        // Remote API setup
        RemoteJmsRuntimeEngineFactory remoteSessionFactory = new RemoteJmsRuntimeEngineFactory(deploymentId, remoteInitialContext,
                user, password);
        RemoteRuntimeEngine engine = remoteSessionFactory.newRuntimeEngine();

        testExtraJaxbClassSerialization(engine);
    }

    public void remoteApiRuleTaskProcess(String user, String password) {
        // setup
        RemoteJmsRuntimeEngineFactory remoteSessionFactory = new RemoteJmsRuntimeEngineFactory(deploymentId, remoteInitialContext,
                user, password);
        RemoteRuntimeEngine runtimeEngine = remoteSessionFactory.newRuntimeEngine();

        runRuleTaskProcess(runtimeEngine.getKieSession(), runtimeEngine.getAuditLogService());
    }

    public void remoteApiInitiatorIdentityTest(String user, String password) {
        // setup
        RemoteJmsRuntimeEngineFactory remoteSessionFactory = new RemoteJmsRuntimeEngineFactory(deploymentId, remoteInitialContext,
                user, password);
        RemoteRuntimeEngine runtimeEngine = remoteSessionFactory.newRuntimeEngine();

        KieSession ksession = runtimeEngine.getKieSession();
        ProcessInstance procInst = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        long procId = procInst.getId();

        List<ProcessInstanceLog> procLogs = runtimeEngine.getAuditLogService().findActiveProcessInstances(HUMAN_TASK_PROCESS_ID);
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

    public void remoteApiRunEvaluationProcess() throws Exception {
        String processId = "com.sample.evaluation";
        KieSession ksession = null;
        ProcessInstance processInstance = null;

        // Kris
        {
            RemoteJmsRuntimeEngineFactory jmsSessionFactory = new RemoteJmsRuntimeEngineFactory(deploymentId, remoteInitialContext,
                    KRIS_USER, KRIS_PASSWORD);
            RuntimeEngine engine = jmsSessionFactory.newRuntimeEngine();
            ksession = engine.getKieSession();
            TaskService taskService = engine.getTaskService();

            // start a new process instance
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("employee", KRIS_USER);
            params.put("reason", "Yearly performance evaluation");
            processInstance = ksession.startProcess(processId, params);
            long procInstId = processInstance.getId();
            logger.debug("Process '" + processId + "' started [instance: " + procInstId + "]");

            // complete Self Evaluation
            List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(KRIS_USER, "en-UK");
            assertEquals(1, tasks.size());
            TaskSummary task = tasks.get(0);
            logger.debug("'" + KRIS_USER + "' completing task " + task.getName() + ": " + task.getDescription());
            taskService.start(task.getId(), KRIS_USER);
            Map<String, Object> results = new HashMap<String, Object>();
            results.put("performance", "exceeding");
            taskService.complete(task.getId(), KRIS_USER, results);
        }

        // john from HR
        {
            InitialContext johnRemoteInitialContext = getRemoteInitialContext(JOHN_USER, JOHN_PASSWORD);
            RemoteJmsRuntimeEngineFactory remoteJmsFactory = new RemoteJmsRuntimeEngineFactory(deploymentId,
                    johnRemoteInitialContext, JOHN_USER, JOHN_PASSWORD);
            RuntimeEngine engine = remoteJmsFactory.newRuntimeEngine();
            ksession = engine.getKieSession();
            TaskService taskService = engine.getTaskService();

            List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(JOHN_USER, "en-UK");
            assertEquals(1, tasks.size());
            TaskSummary task = tasks.get(0);

            logger.debug("'" + JOHN_USER + "' completing task " + task.getName() + " (" + task.getStatus() + "): "
                    + task.getDescription());
            // taskService.claim(task.getId(), JOHN_USER); // NOT NEEDED!
            taskService.start(task.getId(), JOHN_USER);
            Map<String, Object> results = new HashMap<String, Object>();
            results.put("performance", "acceptable");
            taskService.complete(task.getId(), JOHN_USER, results);
        }

        // mary from PM
        {
            InitialContext maryRemoteInitialContext = getRemoteInitialContext(MARY_USER, MARY_PASSWORD);
            RemoteJmsRuntimeEngineFactory remoteJmsFactory = new RemoteJmsRuntimeEngineFactory(deploymentId,
                    maryRemoteInitialContext, MARY_USER, MARY_PASSWORD);
            RuntimeEngine engine = remoteJmsFactory.newRuntimeEngine();
            ksession = engine.getKieSession();
            TaskService taskService = engine.getTaskService();

            List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(MARY_USER, "en-UK");
            assertEquals(1, tasks.size());
            TaskSummary task = tasks.get(0);

            logger.debug("'" + MARY_USER + "' completing task " + task.getName() + ": " + task.getDescription());
            // taskService.claim(task.getId(), MARY_USER); // NOT NEEDED!
            taskService.start(task.getId(), MARY_USER);
            Map<String, Object> results = new HashMap<String, Object>();
            results.put("performance", "outstanding");
            taskService.complete(task.getId(), MARY_USER, results);
        }

        long procId = processInstance.getId();
        processInstance = ksession.getProcessInstance(procId);
        assertTrue("Process instance " + procId + " has not completed!", processInstance == null
                || processInstance.getState() == ProcessInstance.STATE_COMPLETED);
        logger.debug("'" + processId + "' process instance [" + procId + "] completed!");
    }

    public void remoteApiStartScriptProcess(String user, String password) {
        // setup
        RemoteJmsRuntimeEngineFactory remoteSessionFactory = new RemoteJmsRuntimeEngineFactory(deploymentId, remoteInitialContext,
                user, password);
        RemoteRuntimeEngine runtimeEngine = remoteSessionFactory.newRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();

        // start process
        ProcessInstance procInst = ksession.startProcess(SCRIPT_TASK_PROCESS_ID);
        int procStatus = procInst.getState();

        assertEquals("Incorrect process status: " + procStatus, ProcessInstance.STATE_COMPLETED, procStatus);
    }

    public void remoteApiHumanTaskGroupIdTest(URL deploymentUrl) {
        InitialContext krisContext = getRemoteInitialContext(KRIS_USER, KRIS_PASSWORD);
        RemoteRuntimeEngineFactory krisRemoteEngineFactory = new RemoteJmsRuntimeEngineFactory(deploymentId, krisContext,
                KRIS_USER, KRIS_PASSWORD);
        InitialContext maryContext = getRemoteInitialContext(MARY_USER, MARY_PASSWORD);
        RemoteRuntimeEngineFactory maryRemoteEngineFactory = new RemoteJmsRuntimeEngineFactory(deploymentId, maryContext,
                MARY_USER, MARY_PASSWORD);
        InitialContext johnContext = getRemoteInitialContext(JOHN_USER, JOHN_PASSWORD);
        RemoteRuntimeEngineFactory johnRemoteEngineFactory = new RemoteJmsRuntimeEngineFactory(deploymentId, johnContext,
                JOHN_USER, JOHN_PASSWORD);
        runHumanTaskGroupIdTest(krisRemoteEngineFactory, johnRemoteEngineFactory, maryRemoteEngineFactory);
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
}
