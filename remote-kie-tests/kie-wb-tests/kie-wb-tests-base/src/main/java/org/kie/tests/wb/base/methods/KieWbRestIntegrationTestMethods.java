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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.findTaskId;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.findTaskSummary;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runHumanTaskGroupIdTest;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runRemoteApiGroupAssignmentEngineeringTest;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runRuleTaskProcess;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.testExtraJaxbClassSerialization;
import static org.kie.tests.wb.base.util.TestConstants.ARTIFACT_ID;
import static org.kie.tests.wb.base.util.TestConstants.CLASSPATH_ARTIFACT_ID;
import static org.kie.tests.wb.base.util.TestConstants.GROUP_ASSSIGNMENT_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.GROUP_ASSSIGN_VAR_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.GROUP_ID;
import static org.kie.tests.wb.base.util.TestConstants.HUMAN_TASK_OWN_TYPE_ID;
import static org.kie.tests.wb.base.util.TestConstants.HUMAN_TASK_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.HUMAN_TASK_VAR_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.JOHN_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.JOHN_USER;
import static org.kie.tests.wb.base.util.TestConstants.KRIS_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.KRIS_USER;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.base.util.TestConstants.OBJECT_VARIABLE_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.SCRIPT_TASK_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.SCRIPT_TASK_VAR_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.TASK_CONTENT_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.VERSION;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.drools.core.xml.jaxb.util.JaxbUnknownAdapter;
import org.jboss.errai.common.client.util.Base64Util;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.junit.Assume;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.audit.AuditService;
import org.kie.api.runtime.manager.audit.ProcessInstanceLog;
import org.kie.api.runtime.manager.audit.VariableInstanceLog;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskData;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.remote.client.api.RemoteRestRuntimeEngineBuilder;
import org.kie.remote.client.jaxb.ClientJaxbSerializationProvider;
import org.kie.remote.client.jaxb.ConversionUtil;
import org.kie.remote.client.jaxb.JaxbCommandsRequest;
import org.kie.remote.client.jaxb.JaxbCommandsResponse;
import org.kie.remote.client.jaxb.JaxbTaskSummaryListResponse;
import org.kie.remote.common.rest.KieRemoteHttpRequest;
import org.kie.remote.common.rest.KieRemoteHttpResponse;
import org.kie.remote.jaxb.gen.CompleteTaskCommand;
import org.kie.remote.jaxb.gen.Content;
import org.kie.remote.jaxb.gen.GetProcessIdsCommand;
import org.kie.remote.jaxb.gen.GetTaskCommand;
import org.kie.remote.jaxb.gen.GetTasksByProcessInstanceIdCommand;
import org.kie.remote.jaxb.gen.JaxbStringObjectPairArray;
import org.kie.remote.jaxb.gen.StartProcessCommand;
import org.kie.remote.jaxb.gen.StartTaskCommand;
import org.kie.remote.tests.base.AbstractKieRemoteRestMethods;
import org.kie.services.client.api.RemoteRestRuntimeEngineFactory;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.services.client.serialization.JaxbSerializationProvider;
import org.kie.services.client.serialization.JsonSerializationProvider;
import org.kie.services.client.serialization.SerializationException;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbLongListResponse;
import org.kie.services.client.serialization.jaxb.impl.audit.AbstractJaxbHistoryObject;
import org.kie.services.client.serialization.jaxb.impl.audit.JaxbHistoryLogList;
import org.kie.services.client.serialization.jaxb.impl.audit.JaxbProcessInstanceLog;
import org.kie.services.client.serialization.jaxb.impl.audit.JaxbVariableInstanceLog;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentUnit;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentUnit.JaxbDeploymentStatus;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentUnitList;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessDefinition;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessDefinitionList;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessInstanceResponse;
import org.kie.services.client.serialization.jaxb.impl.query.JaxbQueryProcessInstanceInfo;
import org.kie.services.client.serialization.jaxb.impl.query.JaxbQueryProcessInstanceResult;
import org.kie.services.client.serialization.jaxb.impl.query.JaxbVariableInfo;
import org.kie.services.client.serialization.jaxb.rest.JaxbExceptionResponse;
import org.kie.services.client.serialization.jaxb.rest.JaxbGenericResponse;
import org.kie.services.shared.ServicesVersion;
import org.kie.tests.MyType;
import org.kie.tests.wb.base.util.TestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbRestIntegrationTestMethods extends AbstractKieRemoteRestMethods {

    private static Logger logger = LoggerFactory.getLogger(KieWbRestIntegrationTestMethods.class);

    private static final String taskUserId = "salaboy";
    private static final String DEPLOY_FLAG_FILE_NAME = ".deployed";

    private final String deploymentId;
    private final KModuleDeploymentUnit deploymentUnit;
    private RuntimeStrategy strategy = RuntimeStrategy.SINGLETON;

    private MediaType mediaType;
    private int timeoutInMillisecs;
    private static final int DEFAULT_TIMEOUT = 10;

    static { 
        System.setProperty("org.kie.xml.encode", "true");
    }
    
    private KieWbRestIntegrationTestMethods(String deploymentId, MediaType mediaType, int timeoutInSeconds, RuntimeStrategy strategy) {
        if( mediaType == null ) {
            mediaType = MediaType.APPLICATION_XML_TYPE;
        } else if( !MediaType.APPLICATION_JSON_TYPE.equals(mediaType) && !MediaType.APPLICATION_XML_TYPE.equals(mediaType) ) {
            throw new IllegalStateException("Unknown media type: '" + mediaType.getType() + "/" + mediaType.getSubtype() + "'");
        }
        if( strategy != null ) {
            this.strategy = strategy;
        }

        this.deploymentId = deploymentId;
        this.deploymentUnit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION);
        assertEquals("Deployment unit information", deploymentId, deploymentUnit.getIdentifier());
        this.mediaType = mediaType;
        this.timeoutInMillisecs = timeoutInSeconds*1000;
    }

    public static Builder newBuilderInstance() {
        return new Builder();
    }

    public static class Builder {

        private String deploymentId = null;
        private KModuleDeploymentUnit deploymentUnit = null;
        private RuntimeStrategy strategy = RuntimeStrategy.SINGLETON;
        private MediaType mediaType = MediaType.APPLICATION_XML_TYPE;
        private int timeout = DEFAULT_TIMEOUT;

        private Builder() {
            // default constructor
        }

        public Builder setDeploymentId( String deploymentId ) {
            this.deploymentId = deploymentId;
            return this;
        }

        public Builder setStrategy( RuntimeStrategy strategy ) {
            this.strategy = strategy;
            return this;
        }

        public Builder setMediaType( MediaType mediaType ) {
            this.mediaType = mediaType;
            return this;
        }

        public Builder setTimeoutInSecs( int timeout ) {
            this.timeout = timeout;
            return this;
        }

        public KieWbRestIntegrationTestMethods build() {
            if( this.deploymentId == null ) {
                throw new IllegalStateException("The deployment id must be set to create the test methods instance!");
            }
            return new KieWbRestIntegrationTestMethods(deploymentId, mediaType, timeout, strategy);
        }
    }

    private JaxbSerializationProvider jaxbSerializationProvider;
    {
        Class<?> [] classes = { MyType.class };
        jaxbSerializationProvider = ClientJaxbSerializationProvider.newInstance(Arrays.asList(classes));
    }
    private JsonSerializationProvider jsonSerializationProvider = new JsonSerializationProvider();

    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    private long restCallDurationLimit = 2;

    /**
     * Helper methods
     */

    @Override
    public String getContentType() {
        return mediaType.toString();
    }

    @Override
    public <T> T deserializeXml( String xmlStr, Class<T> entityClass ) {
        return (T) jaxbSerializationProvider.deserialize(xmlStr);
    }

    @Override
    public <T> T deserializeJson( String jsonStr, Class<T> entityClass ) {
        T result = null;
        try {
            result = jsonSerializationProvider.deserialize(jsonStr, entityClass);
        } catch( Exception e ) {
            logger.error("Unable to deserialize {} instance:\n{}", entityClass.getSimpleName(), jsonStr, e);
            fail("Unable to deserialize JSON string, see log.");
        }
        return result;
    }

    @Override
    protected String serializeToXml( Object entity ) {
        return jaxbSerializationProvider.serialize(entity);
    }

    @Override
    protected String serializeToJson( Object entity ) {
        return jsonSerializationProvider.serialize(entity);
    }

    private RuntimeEngine getRemoteRuntimeEngine( URL deploymentUrl, String user, String password ) {
        return getRemoteRuntimeEngine(deploymentUrl, user, password, null);
    }

    private RuntimeEngine getRemoteRuntimeEngine( URL deploymentUrl, String user, String password,
            Class... extraClasses ) {
        // @formatter:off
        RemoteRestRuntimeEngineBuilder builder = RemoteRuntimeEngineFactory.newRestBuilder()
                .addDeploymentId(deploymentId)
                .addUrl(deploymentUrl)
                .addUserName(user)
                .addPassword(password);
        // @formatter:on
        if( extraClasses != null && extraClasses.length > 0 ) {
            builder.addExtraJaxbClasses(extraClasses);
        }

        return builder.build();
    }

    /**
     * Test methods
     */

    public static boolean checkDeployFlagFile() throws Exception {
        Properties props = new Properties();
        props.load(KieWbRestIntegrationTestMethods.class.getResourceAsStream("/test.properties"));
        String buildDir = (String) props.get("build.dir");

        String fileNameLocation = buildDir + "/" + DEPLOY_FLAG_FILE_NAME;
        File deployFlag = new File(fileNameLocation);
        if( !deployFlag.exists() ) {
            PrintWriter output = null;
            try {
                output = new PrintWriter(fileNameLocation);
                String date = sdf.format(new Date());
                output.println(date);
            } finally {
                if( output != null ) {
                    output.close();
                }
            }
            return false;
        } else {
            String string = FileUtils.readFileToString(deployFlag);
            logger.debug("Deployed on " + string);
            return true;
        }
    }

    /**
     * Tests deploy and undeploy methods..
     * 
     * @param deploymentUrl
     * @param user
     * @param password
     * @param mediaType
     * @param undeploy Whether or not to test the undeploy operation
     * @throws Exception if anything goes wrong
     */
    public void urlsDeployModuleForOtherTests( URL deploymentUrl, String user, String password, boolean check ) throws Exception {
        if( check ) {
//            Assume.assumeFalse(checkDeployFlagFile());
        }

        RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, user, password, timeoutInMillisecs/1000);

        String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
        String repositoryName = "tests";
        String project = "integration-tests";
        String deploymentId = "org.test:kjar:1.0";
        String orgUnit = "integration tests user";
        deployUtil.createRepositoryAndDeployProject(repoUrl, repositoryName, project, deploymentId, orgUnit, user);

        int sleep = 5;
        logger.info("Waiting {} more seconds to make sure deploy is done..", sleep);
        Thread.sleep(sleep * 1000);
    }

    /**
     * Test human task REST operations (pure REST API)
     * 
     * @param deploymentUrl
     * @param user
     * @param password
     * @throws Exception
     */
    public void urlsStartHumanTaskProcess( URL deploymentUrl, String user, String password ) throws Exception {
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);
        RequestCreator queryRequestCreator = new RequestCreator(deploymentUrl, JOHN_USER, JOHN_PASSWORD, mediaType);

        // Start process
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("runtime/" + deploymentId + "/process/"
                + HUMAN_TASK_PROCESS_ID + "/start");
        KieRemoteHttpResponse response = httpRequest.accept(getContentType()).post().response();
        JaxbProcessInstanceResponse processInstance = deserialize(response, JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        // query tasks for associated task Id
        httpRequest = queryRequestCreator.createRequest("task/query?processInstanceId=" + procInstId);
        response = httpRequest.accept(getContentType()).get().response();
        JaxbTaskSummaryListResponse taskSumlistResponse = deserialize(response, JaxbTaskSummaryListResponse.class);

        TaskSummary taskSum = findTaskSummary(procInstId, taskSumlistResponse.getResult());
        long taskId = taskSum.getId();
        assertNotNull("Null actual owner", taskSum.getActualOwner());

        // get task info
        httpRequest = requestCreator.createRequest("task/" + taskId);
        org.kie.remote.jaxb.gen.Task task = get(httpRequest, org.kie.remote.jaxb.gen.Task.class);
        assertEquals("Incorrect task id", taskId, task.getId().longValue());

        // start task
        httpRequest = requestCreator.createRequest("task/" + taskId + "/start");
        JaxbGenericResponse resp = post(httpRequest, 200, JaxbGenericResponse.class);
        assertNotNull("Response from task start is null.", resp);

        // get task info
        httpRequest = requestCreator.createRequest("task/" + taskId);
        org.kie.remote.jaxb.gen.Task jaxbTask = get(httpRequest, org.kie.remote.jaxb.gen.Task.class);
        assertNotNull("Task response is null.", jaxbTask);
        assertEquals("Task id is incorrect: ", taskId, jaxbTask.getId().longValue());
    }

    /**
     * Test the /execute and command objects when starting processes and managing tasks
     * 
     * @param deploymentUrl
     * @param user
     * @param password
     * @throws Exception
     */
    public void urlsCommandsStartProcess( URL deploymentUrl, String user, String password ) throws Exception {
        MediaType originalType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;

        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        // Start process
        String executeOp = "execute";
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest(executeOp);
        {
            StartProcessCommand cmd = new StartProcessCommand();
            cmd.setProcessId(HUMAN_TASK_PROCESS_ID);
            JaxbCommandsRequest commandMessage = new JaxbCommandsRequest(deploymentId, cmd);
            commandMessage.setVersion(ServicesVersion.VERSION);
            addToRequestBody(httpRequest, commandMessage);
        }

        logger.debug(">> [startProcess] " + httpRequest.getUri());
        JaxbCommandsResponse cmdsResp = post(httpRequest, 200, JaxbCommandsResponse.class);
        assertFalse("Exception received!", cmdsResp.getResponses().get(0) instanceof JaxbExceptionResponse);
        long procInstId = ((ProcessInstance) cmdsResp.getResponses().get(0)).getId();

        // query tasks
        httpRequest = requestCreator.createRequest(executeOp);
        {
            GetTasksByProcessInstanceIdCommand cmd = new GetTasksByProcessInstanceIdCommand();
            cmd.setProcessInstanceId(procInstId);
            JaxbCommandsRequest commandMessage = new JaxbCommandsRequest(deploymentId, cmd);
            commandMessage.setVersion(ServicesVersion.VERSION);
            addToRequestBody(httpRequest, commandMessage);
        }

        logger.debug(">> [getTasksByProcessInstanceId] " + httpRequest.getUri());
        JaxbCommandsResponse cmdResponse = post(httpRequest, 200, JaxbCommandsResponse.class);
        List<?> list = (List<?>) cmdResponse.getResponses().get(0).getResult();
        long taskId = (Long) list.get(0);

        // start task
        logger.debug(">> [startTask] " + httpRequest.getUri());
        httpRequest = requestCreator.createRequest(executeOp);
        {
            StartTaskCommand cmd = new StartTaskCommand();
            cmd.setTaskId(taskId);
            cmd.setUserId(taskUserId);
            JaxbCommandsRequest commandMessage = new JaxbCommandsRequest();
            commandMessage.getCommands().add(cmd);
            commandMessage.setVersion(ServicesVersion.VERSION);
            addToRequestBody(httpRequest, commandMessage);
        }

        // Get response
        post(httpRequest, 200);

        httpRequest = requestCreator.createRequest("execute");
        Map<String, Object> results = new HashMap<String, Object>();
        results.put("myType", new MyType("serialization", 3224950));
        {
            CompleteTaskCommand cmd = new CompleteTaskCommand();
            cmd.setTaskId(taskId);
            cmd.setUserId(taskUserId);
            JaxbStringObjectPairArray arrayMap = ConversionUtil.convertMapToJaxbStringObjectPairArray(results);
            cmd.setData(arrayMap);
            JaxbCommandsRequest commandMessage = new JaxbCommandsRequest(deploymentId, cmd);
            commandMessage.setVersion(ServicesVersion.VERSION);
            addToRequestBody(httpRequest, commandMessage);
        }

        // Get response
        logger.debug(">> [completeTask] " + httpRequest.getUri());
        JaxbCommandsResponse jaxbResp = post(httpRequest, 200, JaxbCommandsResponse.class);
        assertNotNull("Response is null", jaxbResp);

        // TODO: check that above has completed?
        this.mediaType = originalType;
    }

    /**
     * Test Java Remote API for starting processes and managing tasks
     * 
     * @param deploymentUrl
     * @param user
     * @param password
     * @throws Exception
     */
    public void remoteApiHumanTaskProcess( URL deploymentUrl, String user, String password ) throws Exception {
        // create REST request
        RuntimeEngine engine = getRemoteRuntimeEngine(deploymentUrl, user, password);
        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        assertNotNull("Null ProcessInstance!", processInstance);
        long procInstId = processInstance.getId();

        logger.debug("Started process instance: " + processInstance + " " + procInstId);

        TaskService taskService = engine.getTaskService();
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(taskUserId, "en-UK");
        long taskId = findTaskId(procInstId, tasks);

        logger.debug("Found task " + taskId);
        Task task = taskService.getTaskById(taskId);
        logger.debug("Got task " + taskId + ": " + task);
        taskService.start(taskId, taskUserId);
        taskService.complete(taskId, taskUserId, null);

        logger.debug("Now expecting failure");
        try {
            taskService.complete(taskId, taskUserId, null);
            fail("Should not be able to complete task " + taskId + " a second time.");
        } catch( Throwable t ) {
            logger.info("The above exception was an expected part of the test.");
            // do nothing
        }

        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        List<TaskSummary> taskIds = taskService.getTasksByStatusByProcessInstanceId(procInstId, statuses, "en-UK");
        assertEquals("Expected 2 tasks.", 2, taskIds.size());
    }

    public void urlsCommandsTaskCommands( URL deploymentUrl, String user, String password ) throws Exception {
        MediaType origType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;

        RuntimeEngine runtimeEngine = getRemoteRuntimeEngine(deploymentUrl, user, password);
        KieSession ksession = runtimeEngine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);

        long processInstanceId = processInstance.getId();
        GetTasksByProcessInstanceIdCommand cmd = new GetTasksByProcessInstanceIdCommand();
        cmd.setProcessInstanceId(processInstanceId);
        JaxbCommandResponse<?> response = executeCommand(deploymentUrl, user, password, deploymentId, cmd);

        long taskId = ((JaxbLongListResponse) response).getResult().get(0);
        assertTrue("task id is less than 0", taskId > 0);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", taskUserId);

        this.mediaType = origType;
    }

    private JaxbCommandResponse<?> executeCommand( URL appUrl, String user, String password, String deploymentId, Command<?> command )
            throws Exception {
        MediaType originalMediaType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;

        RequestCreator requestCreator = new RequestCreator(appUrl, user, password, mediaType);
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("execute");

        JaxbCommandsRequest commandMessage = new JaxbCommandsRequest(command);
        assertNotNull("Commands are null!", commandMessage.getCommands());
        assertTrue("Commands are empty!", commandMessage.getCommands().size() > 0);

        addToRequestBody(httpRequest, commandMessage);

        logger.debug(">> [" + command.getClass().getSimpleName() + "] " + httpRequest.getUri());
        JaxbCommandsResponse cmdsResp = post(httpRequest, 200, JaxbCommandsResponse.class);

        this.mediaType = originalMediaType;
        return cmdsResp.getResponses().get(0);
    }

    public void urlsHistoryLogs( URL deploymentUrl, String user, String password ) throws Exception {
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        // Start process
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("runtime/" + deploymentId + "/process/"
                + SCRIPT_TASK_VAR_PROCESS_ID + "/start?map_x=initVal");
        logger.debug(">> " + httpRequest.getUri());
        JaxbProcessInstanceResponse processInstance = post(httpRequest, 200, JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        // instances/
        {
            httpRequest = requestCreator.createRequest("history/instances");
            logger.debug(">> [runtime] " + httpRequest.getUri());
            JaxbHistoryLogList historyResult = get(httpRequest, JaxbHistoryLogList.class);
            List<Object> historyLogList = historyResult.getResult();

            for( Object event : historyLogList ) {
                assertTrue("ProcessInstanceLog", event instanceof ProcessInstanceLog);
                ProcessInstanceLog procLog = (ProcessInstanceLog) event;
                Object[][] out = { { procLog.getDuration(), "duration" }, { procLog.getEnd(), "end date" },
                        { procLog.getExternalId(), "externalId" }, { procLog.getIdentity(), "identity" },
                        { procLog.getOutcome(), "outcome" }, { procLog.getParentProcessInstanceId(), "parent proc id" },
                        { procLog.getProcessId(), "process id" }, { procLog.getProcessInstanceId(), "process instance id" },
                        { procLog.getProcessName(), "process name" }, { procLog.getProcessVersion(), "process version" },
                        { procLog.getStart(), "start date" }, { procLog.getStatus(), "status" } };
                for( int i = 0; i < out.length; ++i ) {
                    // System.out.println(out[i][1] + ": " + out[i][0]);
                }
            }
        }
        // instance/{procInstId}

        // instance/{procInstId}/child

        // instance/{procInstId}/node

        // instance/{procInstId}/variable

        // instance/{procInstId}/node/{nodeId}

        // instance/{procInstId}/variable/{variable}
        httpRequest = requestCreator.createRequest("history/instance/" + procInstId + "/variable/x");
        logger.debug(">> [runtime]" + httpRequest.getUri());
        JaxbHistoryLogList historyLogList = get(httpRequest, JaxbHistoryLogList.class);
        List<AbstractJaxbHistoryObject> historyVarLogList = historyLogList.getHistoryLogList();

        httpRequest = requestCreator.createRequest("history/instance/" + procInstId + "/variable/x");
        logger.debug(">> [runtime]" + httpRequest.getUri());
        JaxbHistoryLogList runtimeLogList = get(httpRequest, JaxbHistoryLogList.class);
        List<AbstractJaxbHistoryObject> runtimeVarLogList = runtimeLogList.getHistoryLogList();
        assertTrue("Incorrect number of variable logs: " + runtimeVarLogList.size(), 4 <= runtimeVarLogList.size());

        assertEquals("history list size", historyVarLogList.size(), runtimeVarLogList.size());

        for( int i = 0; i < runtimeVarLogList.size(); ++i ) {
            JaxbVariableInstanceLog varLog = (JaxbVariableInstanceLog) runtimeVarLogList.get(i);
            JaxbVariableInstanceLog historyVarLog = (JaxbVariableInstanceLog) historyVarLogList.get(i);
            assertEquals(historyVarLog.getValue(), varLog.getValue());
            assertEquals("Incorrect variable id", "x", varLog.getVariableId());
            assertEquals("Incorrect process id", SCRIPT_TASK_VAR_PROCESS_ID, varLog.getProcessId());
            assertEquals("Incorrect process instance id", procInstId, varLog.getProcessInstanceId().longValue());
        }

        // process/{procDefId}

        // variable/{varId}

        // variable/{varId}/{value}

        // history/variable/{varId}/instances

        // history/variable/{varId}/value/{val}/instances

    }

    public void urlsJsonJaxbStartProcess( URL deploymentUrl, String user, String password ) throws Exception {
        MediaType origType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        // XML
        String startProcessOper = "runtime/" + deploymentId + "/process/" + HUMAN_TASK_PROCESS_ID + "/start";
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest(startProcessOper);

        String body = post(httpRequest, 200);
        assertTrue("Doesn't start like a JAXB string!", body.startsWith("<"));

        // JSON
        this.mediaType = MediaType.APPLICATION_JSON_TYPE;
        requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);
        httpRequest = requestCreator.createRequest(startProcessOper);
        logger.debug(">> " + httpRequest.getUri());
        String result = post(httpRequest, 200, this.mediaType.toString());
        if( !result.startsWith("{") ) {
            logger.error("Should be JSON:\n" + result);
            fail("Doesn't start like a JSON string!");
        }

        this.mediaType = origType;
    }
   
    private String post(KieRemoteHttpRequest httpRequest, int status, String contentType) { 
        logger.debug( "> [POST] " + httpRequest.getUri().toString() );
        httpRequest.accept(contentType).post();
        checkResponse(httpRequest, status);
        String result = httpRequest.response().body();
        httpRequest.disconnect();
        return result;
    }

    public void urlsHumanTaskWithVariableChangeFormParameters( URL deploymentUrl, String user, String password ) throws Exception {
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        // Start process
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("runtime/" + deploymentId + "/process/"
                + HUMAN_TASK_VAR_PROCESS_ID + "/start");
        httpRequest.form("map_userName", "John");
        JaxbProcessInstanceResponse processInstance = post(httpRequest, 200, JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        // query tasks for associated task Id
        httpRequest = requestCreator.createRequest("task/query");
        httpRequest.query("processInstanceId", String.valueOf(procInstId));
        JaxbTaskSummaryListResponse taskSumlistResponse = get(httpRequest, JaxbTaskSummaryListResponse.class);

        TaskSummary taskSum = findTaskSummary(procInstId, taskSumlistResponse.getResult());
        long taskId = taskSum.getId();

        // start task
        httpRequest = requestCreator.createRequest("task/" + taskId + "/start");
        JaxbGenericResponse resp = post(httpRequest, 200, JaxbGenericResponse.class);
        assertNotNull("Response from task start operation is null.", resp);

        // check task status
        httpRequest = requestCreator.createRequest("task/" + taskId);
        org.kie.remote.jaxb.gen.Task task = get(httpRequest, org.kie.remote.jaxb.gen.Task.class);
        assertNotNull("Response from task start operation is null.", resp);
        logger.debug("Task {}: status [{}] / owner [{}]", taskId, task.getTaskData().getStatus().toString(), task.getTaskData()
                .getActualOwner());

        // complete task
        String georgeVal = "George";
        httpRequest = requestCreator.createRequest("task/" + taskId + "/complete");
        httpRequest.form("map_outUserName", georgeVal);
        resp = post(httpRequest, 200, JaxbGenericResponse.class);

        httpRequest = requestCreator.createRequest("history/instance/" + procInstId + "/variable/userName");
        JaxbHistoryLogList histResp = get(httpRequest, JaxbHistoryLogList.class);
        List<AbstractJaxbHistoryObject> histList = histResp.getHistoryLogList();
        boolean georgeFound = false;
        for( AbstractJaxbHistoryObject<VariableInstanceLog> absVarLog : histList ) {
            VariableInstanceLog varLog = ((JaxbVariableInstanceLog) absVarLog).getResult();
            if( "userName".equals(varLog.getVariableId()) && georgeVal.equals(varLog.getValue()) ) {
                georgeFound = true;
            }
        }
        assertTrue("'userName' var with value '" + georgeVal + "' not found!", georgeFound);
    }

    public void urlsHttpURLConnectionAcceptHeaderIsFixed( URL deploymentUrl, String user, String password ) throws Exception {
        URL url = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/runtime/" + deploymentId + "/process/"
                + SCRIPT_TASK_PROCESS_ID + "/start");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        String authString = user + ":" + password;
        byte [] bytes = authString.getBytes();
        String authStringEnc = Base64Util.encode(bytes, 0, bytes.length);
        connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        connection.setRequestMethod("POST");

        logger.debug(">> [POST] " + url.toExternalForm());
        connection.connect();
        if( 200 != connection.getResponseCode() ) {
            logger.warn(connection.getContent().toString());
        }
        assertEquals(200, connection.getResponseCode());
    }

    public void remoteApiSerialization( URL deploymentUrl, String user, String password ) throws Exception {
        // setup
        RuntimeEngine engine = getRemoteRuntimeEngine(deploymentUrl, user, password);
        KieSession ksession = engine.getKieSession();

        // start process
        ProcessInstance procInst = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        
        Collection<ProcessInstance> processInstances = ksession.getProcessInstances();
        assertNotNull("Null process instance list!", processInstances);
    }

    public void remoteApiExtraJaxbClasses( URL deploymentUrl, String user, String password ) throws Exception {
        runRemoteApiExtraJaxbClassesTest(deploymentId, deploymentUrl, user, password);
    }

    private void runRemoteApiExtraJaxbClassesTest( String deploymentId, URL deploymentUrl, String user, String password )
            throws Exception {
        // Remote API setup
        RuntimeEngine engine = getRemoteRuntimeEngine(deploymentUrl, user, password);
        // test
        testExtraJaxbClassSerialization(engine);
    }

    public void remoteApiRuleTaskProcess( URL deploymentUrl, String user, String password ) {
        // Remote API setup
        RuntimeEngine runtimeEngine = getRemoteRuntimeEngine(deploymentUrl, user, password);

        // runTest
        runRuleTaskProcess(runtimeEngine.getKieSession(), runtimeEngine.getAuditService());
    }

    public void remoteApiGetTaskInstance( URL deploymentUrl, String user, String password ) throws Exception {
        MediaType origType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;

        // Remote API setup
        RuntimeEngine engine = getRemoteRuntimeEngine(deploymentUrl, user, password);

        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = null;
        try {
            processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        } catch( Exception e ) {
            fail("Unable to start process: " + e.getMessage());
        }

        assertNotNull("Null processInstance!", processInstance);
        long procInstId = processInstance.getId();

        TaskService taskService = engine.getTaskService();
        List<Long> tasks = taskService.getTasksByProcessInstanceId(procInstId);
        assertEquals("Incorrect number of tasks for started process: ", 1, tasks.size());
        long taskId = tasks.get(0);

        // Get it via the command
        GetTaskCommand cmd = new GetTaskCommand();
        cmd.setTaskId(taskId);
        JaxbCommandResponse<?> response = executeCommand(deploymentUrl, user, password, deploymentId, cmd);
        Task task = (Task) response.getResult();
        checkReturnedTask(task, taskId);

        // Get it via the URL
        this.mediaType = origType;
        KieRemoteHttpRequest httpRequest = new RequestCreator(deploymentUrl, user, password, mediaType).createRequest("task/"
                + taskId);
        org.kie.remote.jaxb.gen.Task jaxbTask = get(httpRequest, org.kie.remote.jaxb.gen.Task.class);
        checkReturnedTask(jaxbTask, taskId);

        // Get it via the remote API
        task = engine.getTaskService().getTaskById(taskId);
        checkReturnedTask(task, taskId);
    }

    private void checkReturnedTask( Task task, long taskId ) {
        assertNotNull("Could not retrietve task " + taskId, task);
        assertEquals("Incorrect task retrieved", taskId, task.getId().longValue());
        TaskData taskData = task.getTaskData();
        assertNotNull(taskData);
    }

    private void checkReturnedTask( org.kie.remote.jaxb.gen.Task task, long taskId ) {
        assertNotNull("Could not retrietve task " + taskId, task);
        assertEquals("Incorrect task retrieved", taskId, task.getId().longValue());
        org.kie.remote.jaxb.gen.TaskData taskData = task.getTaskData();
        assertNotNull(taskData);
    }

    public void urlsStartScriptProcess( URL deploymentUrl, String user, String password ) throws Exception {
        // Remote API setup
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("runtime/" + deploymentId + "/process/"
                + SCRIPT_TASK_PROCESS_ID + "/start");

        // Start process
        JaxbProcessInstanceResponse jaxbProcInstResp = post(httpRequest, 200, JaxbProcessInstanceResponse.class);
        ProcessInstance procInst = jaxbProcInstResp.getResult();

        int procStatus = procInst.getState();
        assertEquals("Incorrect process status: " + procStatus, ProcessInstance.STATE_COMPLETED, procStatus);
    }

    public void urlsGetTaskAndTaskContent( URL deploymentUrl, String user, String password ) throws Exception {
        // Remote API setup
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("runtime/" + deploymentId + "/process/"
                + TASK_CONTENT_PROCESS_ID + "/start");

        // Start process
        JaxbProcessInstanceResponse jaxbProcInstResp = post(httpRequest, 200, JaxbProcessInstanceResponse.class);
        ProcessInstance procInst = jaxbProcInstResp.getResult();

        int procStatus = procInst.getState();
        assertEquals("Incorrect process status: " + procStatus, ProcessInstance.STATE_ACTIVE, procStatus);

        // Get taskId
        httpRequest = requestCreator.createRequest("task/query?processInstanceId=" + procInst.getId());
        JaxbTaskSummaryListResponse taskSumList = get(httpRequest, JaxbTaskSummaryListResponse.class);
        assertFalse("No tasks found!", taskSumList.getResult().isEmpty());
        TaskSummary taskSum = taskSumList.getResult().get(0);
        long taskId = taskSum.getId();

        // get task content
        httpRequest = requestCreator.createRequest("task/" + taskId + "/content");
        Content content = get(httpRequest, Content.class);
        assertNotNull("No content retrieved!", content.getContentMap());
        String groupId = null;
        for( Entry<String, Object> entry : content.getContentMap().entrySet() ) { 
            if( entry.getKey().equals("GroupId") ) {
                groupId = new String((String) entry.getValue());
                break;
            }
        }
        assertEquals("reviewer", groupId);

        // get (JSON) task
        MediaType origType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_JSON_TYPE;
        httpRequest = new RequestCreator(deploymentUrl, user, password, mediaType).createRequest("task/" + taskId);
        org.kie.remote.jaxb.gen.Task jsonTask = get(httpRequest, org.kie.remote.jaxb.gen.Task.class);

        assertNotNull("No task retrieved!", jsonTask);
        assertEquals("task id", taskId, jsonTask.getId().intValue());
        this.mediaType = origType;
    }

    public void urlsVariableHistory( URL deploymentUrl, String user, String password ) throws Exception {
        // Remote API setup
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        String varId = "myobject";
        String varVal = UUID.randomUUID().toString();

        // proc log
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("history/variable/" + varId + "/instances");
        JaxbHistoryLogList jhll = get(httpRequest, JaxbHistoryLogList.class);
        int initHistSize = jhll.getResult().size();

        httpRequest = requestCreator.createRequest("runtime/" + deploymentId + "/process/" + OBJECT_VARIABLE_PROCESS_ID + "/start?map_" + varId + "=" + varVal);
        JaxbProcessInstanceResponse procInstResp = post(httpRequest, 200, JaxbProcessInstanceResponse.class);
        long procInstId = procInstResp.getResult().getId();

        // var log
        httpRequest = requestCreator.createRequest("history/variable/" + varId);
        jhll = get(httpRequest, JaxbHistoryLogList.class);
        List<VariableInstanceLog> viLogs = new ArrayList<VariableInstanceLog>();
        if( jhll != null ) {
            List<Object> history = jhll.getResult();
            for( Object ae : history ) {
                VariableInstanceLog viLog = (VariableInstanceLog) ae;
                if( viLog.getProcessInstanceId() == procInstId ) {
                    viLogs.add(viLog);
                }
            }
        }

        assertNotNull("Empty VariableInstanceLog list.", viLogs);
        assertEquals("VariableInstanceLog list size", 1, viLogs.size());
        VariableInstanceLog vil = viLogs.get(0);
        assertNotNull("Empty VariableInstanceLog instance.", vil);
        assertEquals("Process instance id", vil.getProcessInstanceId().longValue(), procInstId);
        assertEquals("Variable id", vil.getVariableId(), "myobject");
        assertEquals("Variable value", vil.getValue(), varVal);

        // proc log
        httpRequest = requestCreator.createRequest("history/variable/" + varId + "/instances");
        jhll = get(httpRequest, JaxbHistoryLogList.class);

        assertNotNull("Empty ProcesInstanceLog list", jhll);
        List<ProcessInstanceLog> piLogs = new ArrayList<ProcessInstanceLog>();
        if( jhll != null ) {
            List<Object> history = jhll.getResult();
            for( Object ae : history ) {
                piLogs.add((ProcessInstanceLog) ae);
            }
        }
        assertNotNull("Empty ProcesInstanceLog list", piLogs);
        assertEquals("ProcessInstanceLog list size", initHistSize + 1, piLogs.size() );
        ProcessInstanceLog pi = piLogs.get(0);
        assertNotNull(pi);
    }

    public void urlsGetDeployments( URL deploymentUrl, String user, String password ) throws Exception {
        // test with normal RequestCreator
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("deployment/");
        JaxbDeploymentUnitList depList = get(httpRequest, JaxbDeploymentUnitList.class);
        assertNotNull("Null answer!", depList);
        assertNotNull("Null deployment list!", depList.getDeploymentUnitList());
        assertTrue("Empty deployment list!", depList.getDeploymentUnitList().size() > 0);

        String deploymentId = depList.getDeploymentUnitList().get(0).getIdentifier();
        httpRequest = requestCreator.createRequest("deployment/" + deploymentId);
        JaxbDeploymentUnit dep = get(httpRequest, JaxbDeploymentUnit.class);
        assertNotNull("Null answer!", dep);
        assertNotNull("Null deployment list!", dep);
        assertEquals("Empty status!", JaxbDeploymentStatus.DEPLOYED, dep.getStatus());

        // test with HttpURLConnection
        URL url = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/deployment/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        String authString = user + ":" + password;
        byte [] authStrBytes = authString.getBytes();
        String authStringEnc = Base64Util.encode(authStrBytes, 0, authStrBytes.length);
        connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        connection.setRequestMethod("GET");

        logger.debug(">> [GET] " + url.toExternalForm());
        connection.connect();
        int respCode = connection.getResponseCode();
        if( 200 != respCode ) {
            logger.warn(connection.getContent().toString());
        }
        assertEquals(200, respCode);

        JaxbSerializationProvider jaxbSerializer = ClientJaxbSerializationProvider.newInstance();
        String xmlStrObj = getConnectionContent(connection.getContent());
        logger.info("Output: |" + xmlStrObj + "|");
        depList = (JaxbDeploymentUnitList) jaxbSerializer.deserialize(xmlStrObj);

        assertNotNull("Null answer!", depList);
        assertNotNull("Null deployment list!", depList.getDeploymentUnitList());
        assertTrue("Empty deployment list!", depList.getDeploymentUnitList().size() > 0);
    }

    private String getConnectionContent( Object content ) throws Exception {
        InputStreamReader in = new InputStreamReader((InputStream) content);
        BufferedReader buff = new BufferedReader(in);
        StringBuffer text = new StringBuffer();
        String line = buff.readLine();
        while( line != null ) {
            text.append(line);
            line = buff.readLine();
        }
        return text.toString();
    }

    public void urlsGetRealProcessVariable( URL deploymentUrl, String user, String password ) throws Exception {
        // Setup
        RuntimeEngine engine = getRemoteRuntimeEngine(deploymentUrl, user, password);
        logger.info("deployment url: " + deploymentUrl.toExternalForm());
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        // Start process
        MyType param = new MyType("variable", 29);
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("myobject", param);
        long procInstId = engine.getKieSession().startProcess(OBJECT_VARIABLE_PROCESS_ID, parameters).getId();

        /**
         * Check that MyType was correctly deserialized on server side
         */
        String varName = "myobject";
        List<VariableInstanceLog> varLogList = (List<VariableInstanceLog>) engine.getAuditService().findVariableInstancesByName(
                varName, false);
        VariableInstanceLog thisProcInstVarLog = null;
        for( VariableInstanceLog varLog : varLogList ) {
            if( varLog.getProcessInstanceId() == procInstId ) {
                thisProcInstVarLog = varLog;
                break;
            }
        }
        assertNotNull("No VariableInstanceLog found!", thisProcInstVarLog);
        assertEquals(varName, thisProcInstVarLog.getVariableId());
        Object procInstVar = thisProcInstVarLog.getValue();
        assertNotNull("Null process instance variable!", procInstVar);
        assertEquals("De/serialization of Kjar type did not work.", param.toString(), procInstVar);

        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("runtime/" + deploymentId + "/process/instance/"
                + procInstId);
        JaxbProcessInstanceResponse jaxbProcInstResp = get(httpRequest, JaxbProcessInstanceResponse.class);
        ProcessInstance procInst = jaxbProcInstResp.getResult();
        assertNotNull(procInst);
        assertEquals("Unequal process instance id.", procInstId, procInst.getId());

        httpRequest = requestCreator.createRequest("runtime/" + deploymentId + "/process/instance/" + procInstId + "/variable/"
                + varName);
        String xmlOrJsonStr = httpRequest.get().response().body();
        
        MyType retrievedVar;
        try {
            if( mediaType.equals(MediaType.APPLICATION_XML_TYPE) ) {
                ((ClientJaxbSerializationProvider) jaxbSerializationProvider).addJaxbClasses(MyType.class);
                retrievedVar  = (MyType) jaxbSerializationProvider.deserialize(xmlOrJsonStr);
            } else if( mediaType.equals(MediaType.APPLICATION_JSON_TYPE) ) {
                jsonSerializationProvider.setDeserializeOutputClass(MyType.class);
                retrievedVar = (MyType) jsonSerializationProvider.deserialize(xmlOrJsonStr);
            } else {
                throw new IllegalStateException("Unknown media type: " + mediaType.getType() + "/" + mediaType.getSubtype());
            }
        } catch( SerializationException se ) {
            logger.error("Could not deserialize string:\n{}", xmlOrJsonStr);
            throw se;
        }

        assertNotNull("Expected filled variable.", retrievedVar);
        assertEquals("Data integer doesn't match: ", retrievedVar.getData(), param.getData());
        assertEquals("Text string doesn't match: ", retrievedVar.getText(), param.getText());
    }

    public void remoteApiHumanTaskGroupIdTest( URL deploymentUrl ) {
        RemoteRestRuntimeEngineBuilder runtimeEngineBuilder = RemoteRestRuntimeEngineFactory.newBuilder()
                .addDeploymentId(deploymentId).addUrl(deploymentUrl);

        RuntimeEngine krisRemoteEngine = runtimeEngineBuilder.addUserName(KRIS_USER).addPassword(KRIS_PASSWORD).build();
        RuntimeEngine maryRemoteEngine = runtimeEngineBuilder.addUserName(MARY_USER).addPassword(MARY_PASSWORD).build();
        RuntimeEngine johnRemoteEngine = runtimeEngineBuilder.addUserName(JOHN_USER).addPassword(JOHN_PASSWORD).build();

        runHumanTaskGroupIdTest(krisRemoteEngine, johnRemoteEngine, maryRemoteEngine);
    }

    public void urlsGroupAssignmentTest( URL deploymentUrl ) throws Exception {
        RequestCreator maryRequestCreator = new RequestCreator(deploymentUrl, MARY_USER, MARY_PASSWORD, mediaType);
        RequestCreator johnRequestCreator = new RequestCreator(deploymentUrl, JOHN_USER, JOHN_PASSWORD, mediaType);

        KieRemoteHttpRequest httpRequest = maryRequestCreator.createRequest("runtime/" + deploymentId + "/process/"
                + GROUP_ASSSIGNMENT_PROCESS_ID + "/start");
        JaxbProcessInstanceResponse procInstResp = post(httpRequest, 200, JaxbProcessInstanceResponse.class);
        assertEquals(ProcessInstance.STATE_ACTIVE, procInstResp.getState());
        long procInstId = procInstResp.getId();

        // assert the task
        TaskSummary taskSummary = getTaskSummary(maryRequestCreator, procInstId, Status.Ready);
        long taskId = taskSummary.getId();
        assertNull(taskSummary.getActualOwner());
        assertNull(taskSummary.getPotentialOwners());
        assertEquals("Task 1", taskSummary.getName());

        // complete 'Task 1' as mary
        httpRequest = maryRequestCreator.createRequest("task/" + taskId + "/claim");
        post(httpRequest, 200);

        httpRequest = maryRequestCreator.createRequest("task/" + taskId + "/start");
        post(httpRequest, 200);
        httpRequest = maryRequestCreator.createRequest("task/" + taskId + "/complete");
        post(httpRequest, 200);

        // now make sure that the next task has been assigned to the
        // correct person. it should be mary.
        taskSummary = getTaskSummary(maryRequestCreator, procInstId, Status.Reserved);
        assertEquals("Task 2", taskSummary.getName());
        assertEquals(MARY_USER, taskSummary.getActualOwner().getId());
        taskId = taskSummary.getId();

        // complete 'Task 2' as john
        httpRequest = maryRequestCreator.createRequest("task/" + taskId + "/release");
        post(httpRequest, 200);
        httpRequest = johnRequestCreator.createRequest("task/" + taskId + "/start");
        post(httpRequest, 200);
        httpRequest = johnRequestCreator.createRequest("task/" + taskId + "/complete");
        post(httpRequest, 200);

        // now make sure that the next task has been assigned to the
        // correct person. it should be john.
        taskSummary = getTaskSummary(johnRequestCreator, procInstId, Status.Reserved);
        assertEquals("Task 3", taskSummary.getName());
        assertEquals(JOHN_USER, taskSummary.getActualOwner().getId());
        taskId = taskSummary.getId();

        // complete 'Task 3' as john
        httpRequest = johnRequestCreator.createRequest("task/" + taskId + "/start");
        post(httpRequest, 200);
        httpRequest = johnRequestCreator.createRequest("task/" + taskId + "/complete");
        post(httpRequest, 200);

        // assert process finished
        httpRequest = maryRequestCreator.createRequest("history/instance/" + procInstId);
        JaxbProcessInstanceLog jaxbProcInstLog = get(httpRequest, JaxbProcessInstanceLog.class);
        ProcessInstanceLog procInstLog = jaxbProcInstLog.getResult();
        assertEquals("Process instance has not completed!", ProcessInstance.STATE_COMPLETED, procInstLog.getStatus().intValue());
    }

    private TaskSummary getTaskSummary( RequestCreator requestCreator, long processInstanceId, Status status ) throws Exception {
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("task/query?processInstanceId=" + processInstanceId
                + "&status=" + status.toString());
        JaxbTaskSummaryListResponse taskSumListResp = get(httpRequest, JaxbTaskSummaryListResponse.class);
        List<TaskSummary> taskSumList = taskSumListResp.getResult();
        assertEquals(1, taskSumList.size());
        return taskSumList.get(0);
    }

    public void urlsWorkItemTest( URL deploymentUrl, String user, String password ) throws Exception {
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        // Start process
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("runtime/" + deploymentId + "/workitem/200");
        get(httpRequest);
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


        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("taskOwnerGroup", "HR");
        params.put("taskName", "Mary's Task");
        ProcessInstance pi = runtimeEngine.getKieSession().startProcess(GROUP_ASSSIGN_VAR_PROCESS_ID, params);
        assertNotNull("No ProcessInstance!", pi);
        long procInstId = pi.getId();

        List<Long> taskIds = runtimeEngine.getTaskService().getTasksByProcessInstanceId(procInstId);
        assertEquals(1, taskIds.size());

        List<String> processIds = (List<String>) runtimeEngine.getKieSession().execute(new GetProcessIdsCommand());
        assertTrue("No process ids returned.", !processIds.isEmpty() && processIds.size() > 5);
    }

    public void remoteApiHumanTaskOwnTypeTest( URL deploymentUrl ) {
        // @formatter:off
        RuntimeEngine runtimeEngine 
            = RemoteRuntimeEngineFactory.newRestBuilder()
                .addDeploymentId(deploymentId)
                .addUserName(JOHN_USER)
                .addPassword(JOHN_PASSWORD)
                .addUrl(deploymentUrl)
                .addExtraJaxbClasses(MyType.class)
                .build();
        // @formatter:on

        runRemoteApiHumanTaskOwnTypeTest(runtimeEngine, runtimeEngine.getAuditService());
    }

    public void runRemoteApiHumanTaskOwnTypeTest( RuntimeEngine runtimeEngine, AuditService auditLogService ) {
        MyType myType = new MyType("wacky", 123);

        ProcessInstance pi = runtimeEngine.getKieSession().startProcess(HUMAN_TASK_OWN_TYPE_ID);
        assertNotNull(pi);
        assertEquals(ProcessInstance.STATE_ACTIVE, pi.getState());

        TaskService taskService = runtimeEngine.getTaskService();
        List<Long> taskIds = taskService.getTasksByProcessInstanceId(pi.getId());
        assertFalse(taskIds.isEmpty());
        long taskId = taskIds.get(0);

        taskService.start(taskId, JOHN_USER);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("outMyObject", myType);
        taskService.complete(taskId, JOHN_USER, data);

        Task task = taskService.getTaskById(taskId);
        assertEquals(Status.Completed, task.getTaskData().getStatus());

        List<VariableInstanceLog> vill = (List<VariableInstanceLog>) auditLogService.findVariableInstances(pi.getId(), "myObject");
        assertNotNull(vill);
        assertFalse("Empty list of variable instance logs", vill.isEmpty());
        assertEquals(myType.toString(), vill.get(0).getValue());
    }

    public void urlsCreateMemoryLeakOnTomcat( URL deploymentUrl, String user, String password, long timeout ) throws Exception {
        long origCallDurationLimit = this.restCallDurationLimit;
        this.restCallDurationLimit = timeout;

        // Remote API setup
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);
        try {
            for( int i = 0; i < 20; ++i ) {
                logger.info(i + " process started.");
                startProcessWithUserDefinedClass(requestCreator);
            }
        } finally {
            this.restCallDurationLimit = origCallDurationLimit;
        }
    }

    private void startProcessWithUserDefinedClass( RequestCreator requestCreator ) throws Exception {
        String varId = "myobject";
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("runtime/" + deploymentId + "/process/"
                + OBJECT_VARIABLE_PROCESS_ID + "/start?map_" + varId + "=10");
        long after, before = System.currentTimeMillis();
        httpRequest.post();
        checkResponse(httpRequest, 200);
        after = System.currentTimeMillis();
        long duration = (after - before);
        assertTrue("Call took longer than " + restCallDurationLimit + " seconds: " + duration + "ms",
                duration < restCallDurationLimit * 1000);

        JaxbProcessInstanceResponse procInstResp = deserialize(httpRequest.response(), JaxbProcessInstanceResponse.class);
        long procInstId = procInstResp.getResult().getId();
        ;
        assertTrue("Process instance should be larger than 0: " + procInstId, procInstId > 0);
    }

    public void urlsGetProcessDefinitionInfo( URL deploymentUrl, String user, String password ) throws Exception {
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        // Start process
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("deployment/processes/");
        JaxbProcessDefinitionList jaxbProcDefList = get(httpRequest, JaxbProcessDefinitionList.class);

        List<JaxbProcessDefinition> procDefList = jaxbProcDefList.getProcessDefinitionList();
        for( JaxbProcessDefinition jaxbProcDef : procDefList ) {
            validateProcessDefinition(jaxbProcDef);
        }

    }

    private void validateProcessDefinition( JaxbProcessDefinition procDef ) {
        String id = procDef.getId();
        assertFalse("Process def " + id + ": null deployment id", procDef.getDeploymentId() == null
                || procDef.getDeploymentId().isEmpty());
        assertFalse("Process def " + id + ": null name", procDef.getName() == null || procDef.getName().isEmpty());
        assertFalse("Process def " + id + ": null pkg name", procDef.getPackageName() == null || procDef.getPackageName().isEmpty());
        assertFalse("Process def " + id + ": null variables", procDef.getVariables() == null || procDef.getVariables().isEmpty());
        assertFalse("Process def " + id + ": null version", procDef.getVersion() == null || procDef.getVersion().isEmpty());
    }

    public void remoteApiDeploymentRedeployClassPathTest( URL deploymentUrl, String user, String password ) throws Exception {
        // setup
        KModuleDeploymentUnit kDepUnit = new KModuleDeploymentUnit(GROUP_ID, CLASSPATH_ARTIFACT_ID, VERSION);
        String classpathDeploymentId = kDepUnit.getIdentifier();

        // create repo if not present
        RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, user, password, timeoutInMillisecs/1000);
        String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
        String repositoryName = "integration-tests";
        String project = "integration-tests-classpath";
        String orgUnit = "Classpath User";
      
        if( ! deployUtil.checkRepositoryExistence(repositoryName) ) { 
            deployUtil.createRepositoryAndDeployProject(repoUrl, repositoryName, project, classpathDeploymentId, orgUnit, user);
        } else { 
            deployUtil.deploy(classpathDeploymentId);
        }
      
        // test 1: deployment status changes after undeploy
        deployUtil.undeploy(classpathDeploymentId);
        JaxbDeploymentUnit depUnit = deployUtil.getDeploymentUnit(classpathDeploymentId);
        assertTrue( "Incorrect deployment unit status: " + depUnit.getStatus(), depUnit.getStatus().equals(JaxbDeploymentStatus.UNDEPLOYED));
      
        // test 2: deploy, test, undeploy, deploy, rerun test..
        // Deploy
        deployUtil.deploy(kDepUnit.getIdentifier());

        // Run process
        // @formatter:off
        RuntimeEngine runtimeEngine = RemoteRestRuntimeEngineFactory.newBuilder()
                .addDeploymentId(classpathDeploymentId)
                .addUrl(deploymentUrl)
                .addUserName(user)
                .addPassword(password)
                .build();
        // @formatter:on

        runClassPathProcessTest(runtimeEngine);

        // undeploy..
        deployUtil.undeploy(kDepUnit.getIdentifier());

        // .. and (re)deploy
        deployUtil.deploy(kDepUnit.getIdentifier());

        logger.info("Rerunning test.. is there a CNFE?");
        // Rerun process
        runClassPathProcessTest(runtimeEngine);
    }

    private void runClassPathProcessTest( RuntimeEngine runtimeEngine ) {
        KieSession ksession = runtimeEngine.getKieSession();

        Map<String, Object> params = new HashMap<String, Object>();
        String varId = "myobject";
        String text = UUID.randomUUID().toString();
        params.put(varId, new MyType(text, 10));
        ProcessInstance procInst = ksession.startProcess(TestConstants.CLASSPATH_OBJECT_PROCESS_ID, params);
        long processInstanceId = procInst.getId();

        AuditService auditLogService = runtimeEngine.getAuditService();
        List<VariableInstanceLog> varLogList = (List<VariableInstanceLog>) auditLogService.findVariableInstances(processInstanceId);

        assertNotNull("Null variable instance found.", varLogList);
        for( VariableInstanceLog varLog : varLogList ) {
            logger.debug(varLog.getVariableId() + " (" + varLog.getValue() + ") ");
        }

        List<VariableInstanceLog> varLogs = (List<VariableInstanceLog>) runtimeEngine.getAuditService()
                .findVariableInstancesByName(varId, false);
        assertTrue(varLogs.size() > 0);
        assertEquals(varId, varLogs.get(0).getVariableId());

        procInst = ksession.getProcessInstance(processInstanceId);
        assertNull(procInst);
    }

    public void urlsDeploymentProcessDefinitions( URL deploymentUrl, String user, String password ) throws Exception {
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("/deployment/processes");
        JaxbProcessDefinitionList jaxbProcDefList = get(httpRequest, JaxbProcessDefinitionList.class);

        assertTrue("Null response!", jaxbProcDefList != null);
        List<JaxbProcessDefinition> procDefList = jaxbProcDefList.getProcessDefinitionList();
        assertTrue("Empty response list!", jaxbProcDefList != null && !procDefList.isEmpty());

        JaxbProcessDefinition jaxbProcDef = procDefList.get(0);
        assertNotNull("Null deployment id", jaxbProcDef.getDeploymentId());
        assertNotNull("Null id", jaxbProcDef.getId());
        assertNotNull("Null name", jaxbProcDef.getName());
        assertNotNull("Null package name", jaxbProcDef.getPackageName());

        for( JaxbProcessDefinition procDef : procDefList ) {
            if( procDef.getVariables() != null ) {
                logger.info("{}/{} : {}", procDef.getDeploymentId(), procDef.getName(), procDef.getVariables().size());
            }
        }
    }
    
    public void remoteApiGroupAssignmentEngineeringTest( URL deploymentUrl ) throws Exception {
        RuntimeEngine runtimeEngine 
            = RemoteRestRuntimeEngineFactory.newBuilder()
            .addDeploymentId(deploymentId)
            .addUserName(MARY_USER)
            .addPassword(MARY_PASSWORD)
            .addUrl(deploymentUrl)
            .build();
        runRemoteApiGroupAssignmentEngineeringTest(runtimeEngine);
    }
    
    public void urlsProcessQueryOperationsTest( URL deploymentUrl, String user, String password ) throws Exception  { 
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        KieRemoteHttpRequest request = requestCreator.createRequest("/query/runtime/task");
        request.query("params", null);
        
        KieRemoteHttpResponse response = request.get().response();
        int statusCode = response.code();
        assertEquals( "Inccorect status code [" + statusCode + "] on bad request with 'params' query param", 400, statusCode);
        
        KieSession ksession = getRemoteRuntimeEngine(deploymentUrl, user, password).getKieSession();

        String val = UUID.randomUUID().toString();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "x", val );
        ProcessInstance procInst = ksession.startProcess(SCRIPT_TASK_VAR_PROCESS_ID, map);
        long procInstId = procInst.getId();
        
        request = requestCreator.createRequest("/query/runtime/process");
        request.query("piid", procInstId);
        
        JaxbQueryProcessInstanceResult queryResult = get(request, JaxbQueryProcessInstanceResult.class);
        assertNotNull("Null query result", queryResult);
        List<JaxbQueryProcessInstanceInfo> procInstInfoList = queryResult.getProcessInstanceInfoList();
        assertTrue("Empty query info list", procInstInfoList != null && ! procInstInfoList.isEmpty() );
        assertEquals( "Only queried for 1 process instance", 1, procInstInfoList.size() );
        JaxbQueryProcessInstanceInfo procInstInfo = procInstInfoList.get(0);
        assertEquals("Incorrect process id", SCRIPT_TASK_VAR_PROCESS_ID, procInstInfo.getProcessInstance().getProcessId());
        for( JaxbVariableInfo varInfo : procInstInfo.getVariables() ) { 
           System.out.println( varInfo.getName()
                   + "/" + varInfo.getModificationDate() 
                   + "/" + varInfo.getValue() );
        }
    }
   
    public void remoteApiFunnyCharacters( URL deploymentUrl, String user, String password ) throws Exception  { 
        // verify that property is set on client side
        Field field = JaxbUnknownAdapter.class.getDeclaredField("ENCODE_STRINGS");
        field.setAccessible(true);
        Object fieldObj = field.get(null);
        assertTrue( "ENCODE_STRINGS fiels is a " + fieldObj.getClass().getName(), fieldObj instanceof Boolean );
        Boolean encodeStringsBoolean = (Boolean) fieldObj;
        assertTrue( "ENCODE_STRINGS is '" + encodeStringsBoolean, encodeStringsBoolean );
        
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        RuntimeEngine runtimeEngine = getRemoteRuntimeEngine(deploymentUrl, user, password);
        KieSession ksession = runtimeEngine.getKieSession();
       
        String [] vals = { 
            "a long string containing spaces and other characters +ěš@#$%^*()_{}\\/.,",
            "Ampersand in the string &.",
            "\"quoted string\""
        };
        long [] procInstIds = new long[vals.length];
        for( int i = 0; i < vals.length; ++i ) { 
            procInstIds[i] = startScriptTaskVarProcess(ksession, vals[i]);
        }
       
        for( int i = 0; i < vals.length; ++i ) { 
            List<? extends VariableInstanceLog> varLogs = runtimeEngine.getAuditService().findVariableInstances(procInstIds[i]);
            for( VariableInstanceLog log : varLogs ) { 
               System.out.println( log.getVariableInstanceId() + ":"  + log.getVariableId() + ":["  + log.getValue() + "]" ); 
            }
        }
    }
    
    private static long startScriptTaskVarProcess(KieSession ksession, String val) { 
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "x", val );
        ProcessInstance procInst = ksession.startProcess(SCRIPT_TASK_VAR_PROCESS_ID, map);
        return procInst.getId();
    }
    
}
