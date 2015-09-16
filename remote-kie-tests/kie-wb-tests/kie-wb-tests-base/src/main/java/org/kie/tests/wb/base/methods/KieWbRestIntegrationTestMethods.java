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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kie.remote.tests.base.RestUtil.postEntity;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.PARAM_SERIALIZATION_PARAM_NAME;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.findTaskIdByProcessInstanceId;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.findTaskSummaryByProcessInstanceId;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runHumanTaskGroupIdTest;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runHumanTaskGroupVarAssignTest;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runRemoteApiCorrelationKeyTest;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runRemoteApiFunnyCharactersTest;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runRemoteApiGroupAssignmentEngineeringTest;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runRemoteApiHistoryVariablesTest;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runRemoteApiHumanTaskCommentTest;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runRemoteApiHumanTaskOwnTypeTest;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runRemoteApiProcessInstances;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runRuleTaskProcess;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.sdf;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.testClassSerialization;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.testParamSerialization;
import static org.kie.tests.wb.base.util.TestConstants.ARTIFACT_ID;
import static org.kie.tests.wb.base.util.TestConstants.CLASSPATH_ARTIFACT_ID;
import static org.kie.tests.wb.base.util.TestConstants.GROUP_ASSSIGNMENT_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.GROUP_ID;
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
import static org.kie.tests.wb.base.util.TestConstants.VERSION;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.Is;
import org.jboss.errai.common.client.util.Base64Util;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.audit.AuditService;
import org.kie.api.runtime.manager.audit.ProcessInstanceLog;
import org.kie.api.runtime.manager.audit.VariableInstanceLog;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
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
import org.kie.remote.jaxb.gen.CompleteTaskCommand;
import org.kie.remote.jaxb.gen.Content;
import org.kie.remote.jaxb.gen.GetTasksByProcessInstanceIdCommand;
import org.kie.remote.jaxb.gen.JaxbStringObjectPairArray;
import org.kie.remote.jaxb.gen.StartProcessCommand;
import org.kie.remote.jaxb.gen.StartTaskCommand;
import org.kie.remote.tests.base.RestUtil;
import org.kie.services.client.api.RemoteRestRuntimeEngineFactory;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.services.client.api.command.exception.RemoteApiException;
import org.kie.services.client.serialization.JaxbSerializationProvider;
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
import org.kie.services.client.serialization.jaxb.impl.process.JaxbWorkItemResponse;
import org.kie.services.client.serialization.jaxb.impl.query.JaxbQueryProcessInstanceInfo;
import org.kie.services.client.serialization.jaxb.impl.query.JaxbQueryProcessInstanceResult;
import org.kie.services.client.serialization.jaxb.impl.query.JaxbVariableInfo;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbArray;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbBoolean;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbByte;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbCharacter;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbDouble;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbFloat;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbInteger;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbList;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbLong;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbMap;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbSet;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbShort;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbString;
import org.kie.services.client.serialization.jaxb.impl.type.JaxbType;
import org.kie.services.client.serialization.jaxb.rest.JaxbExceptionResponse;
import org.kie.services.client.serialization.jaxb.rest.JaxbGenericResponse;
import org.kie.tests.MyBinaryType;
import org.kie.tests.MyType;
import org.kie.tests.wb.base.util.TestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class KieWbRestIntegrationTestMethods implements IntegrationTestMethods {

    private static Logger logger = LoggerFactory.getLogger(KieWbRestIntegrationTestMethods.class);

    private static final String taskUserId = "salaboy";
    private static final String DEPLOY_FLAG_FILE_NAME = ".deployed";

    private final String deploymentId;
    private final KModuleDeploymentUnit deploymentUnit;
    private RuntimeStrategy strategy = RuntimeStrategy.SINGLETON;

    private String contentType;
    private String user;
    private String password;
    private URL deploymentUrl;

    private int timeoutInMillisecs;
    private static final int DEFAULT_TIMEOUT = 10;

    private static Random random = new Random();

    static {
        System.setProperty("org.kie.xml.encode", "true");
    }

    private KieWbRestIntegrationTestMethods(String deploymentId, String mediaType, int timeoutInSeconds, RuntimeStrategy strategy) {
        if( mediaType == null ) {
            mediaType = MediaType.APPLICATION_XML;
        } else if( !MediaType.APPLICATION_JSON.equals(mediaType) && !MediaType.APPLICATION_XML.equals(mediaType) ) {
            throw new IllegalStateException("Unknown content type: '" + mediaType);
        }
        if( strategy != null ) {
            this.strategy = strategy;
        }

        this.deploymentId = deploymentId;
        this.deploymentUnit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION);
        assertEquals("Deployment unit information", deploymentId, deploymentUnit.getIdentifier());
        this.contentType = mediaType;
        this.timeoutInMillisecs = timeoutInSeconds*1000;
    }

    public static Builder newBuilderInstance() {
        return new Builder();
    }

    public static class Builder {

        private String deploymentId = null;
        private RuntimeStrategy strategy = RuntimeStrategy.SINGLETON;
        private String mediaType = MediaType.APPLICATION_XML;
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

        public Builder setMediaType( String mediaType ) {
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

    private long restCallDurationLimit = 2;

    /**
     * Helper methods
     */

    @Override
    public RuntimeEngine getRemoteRuntimeEngine( URL deploymentUrl, String user, String password ) {
        // @formatter:off
        RemoteRestRuntimeEngineBuilder builder = RemoteRuntimeEngineFactory.newRestBuilder()
                .addDeploymentId(deploymentId)
                .addUrl(deploymentUrl)
                .addUserName(user)
                .addPassword(password);
        // @formatter:on
        return builder.build();
    }

    private void setRestInfo(URL deploymentUrl, String user, String password) {
       setRestInfo(deploymentUrl, user, password, contentType);
    }

    private void setRestInfo(URL deploymentUrl, String user, String password, String contentType) {
        this.deploymentUrl = deploymentUrl;
        this.user = user;
        this.password = password;
        this.contentType = contentType;
    }

    private <T> T post(String relativeUrl, int status, Class<T>... returnType) {
        return post(relativeUrl, user, password, status, returnType);
    }

    private <T> T post(String relativeUrl, String user, String password, int status, Class<T>... returnType) {
        return RestUtil.post(deploymentUrl, "rest/" + relativeUrl, contentType,
                status, user, password,
                returnType);
    }

    private <T> T post(String relativeUrl, int status, Object entity, Class<T> returnType) {
        return RestUtil.postEntity(deploymentUrl, "rest/" + relativeUrl, contentType,
                status, user, password,
                entity, returnType);
    }

    private <T> T get(String relativeUrl, int status, Class<T> returnType) {
        return RestUtil.get(deploymentUrl, "rest/" + relativeUrl, contentType,
                status, user, password,
                returnType);
    }


    private <T> T get(String relativeUrl, int status, Map<String, String> queryParams, Class<T> returnType) {
        return RestUtil.getQuery(deploymentUrl, "rest/" + relativeUrl, contentType,
                status, user, password,
                queryParams,
                returnType);
    }

    private JaxbCommandResponse<?> executeCommand( URL appUrl, String user, String password, String deploymentId, Command<?>... command )
        throws Exception {
        List<JaxbCommandResponse<?>> responses = executeCommands(appUrl, user, password, deploymentId, null, command) ;
        return responses.get(0);
    }

    private List<JaxbCommandResponse<?>> executeCommands( URL appUrl, String user, String password, String deploymentId, Long processInstanceId,
            Command<?>... command )
            throws Exception {
        JaxbCommandsRequest req = new JaxbCommandsRequest(deploymentId, command[0]);
        req.setProcessInstanceId(processInstanceId);
        for( int i = 1; i < command.length; ++i ) {
            req.getCommands().add(command[i]);
        }

        JaxbCommandsResponse resp = implSpecificSendCommandRequest(req, user, password);

        return resp.getResponses();
    }

    public JaxbCommandsResponse implSpecificSendCommandRequest(JaxbCommandsRequest req, String user, String password) throws Exception {
       return implSpecificSendCommandRequest(req, user, password, true);
    }

    @Override
    public JaxbCommandsResponse implSpecificSendCommandRequest(JaxbCommandsRequest req, String user, String password, boolean noop)
        throws Exception {
        assertNotNull("Commands are null!", req.getCommands());
        assertTrue("Commands are empty!", req.getCommands().size() > 0);

        JaxbCommandsResponse cmdsResp = postEntity(deploymentUrl,
                "rest/execute", MediaType.APPLICATION_XML,
                200, user, password,
                req, JaxbCommandsResponse.class);
        assertNotNull("Null commands response", cmdsResp);

        return cmdsResp;
    }

    private void checkReturnedTask( Task task, long taskId ) {
        assertNotNull("Could not retrietve task " + taskId, task);
        assertEquals("Incorrect task retrieved", taskId, task.getId().longValue());
        TaskData taskData = task.getTaskData();
        assertNotNull(taskData);

        assertNotNull("Null actual owner", taskData.getActualOwner());
        String actualOwner = taskData.getActualOwner().getId();
        assertNotNull("Null actual owner id", actualOwner);
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
    public void urlsDeployModuleForOtherTests( URL deploymentUrl, String user, String password) throws Exception {

        RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, user, password, timeoutInMillisecs/1000);

        String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
        String repositoryName = "tests";
        String project = "integration-tests";
        String deploymentId = "org.test:kjar:1.0";
        String orgUnit = UUID.randomUUID().toString();
        orgUnit = orgUnit.substring(0, orgUnit.indexOf("-"));
        deployUtil.createRepositoryAndDeployProject(repoUrl, repositoryName, project, deploymentId, orgUnit);

        int sleep = 5;
        logger.info("Waiting {} more seconds to make sure deploy is done..", sleep);
        Thread.sleep(sleep * 1000);
    }

    public void urlsHumanTask( URL deploymentUrl, String user, String password ) throws Exception {
        setRestInfo(deploymentUrl, user, password);

        // Start process
        String startProcessUrl = "rest/runtime/" + deploymentId + "/process/" + HUMAN_TASK_VAR_PROCESS_ID + "/start";

        Map<String, String> formParams = new HashMap<String, String>(1);
        formParams.put("map_userName", "John");
        JaxbProcessInstanceResponse processInstance = RestUtil.postForm(deploymentUrl,
                startProcessUrl, contentType,
                 200, user, password,
                 formParams,
                 JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        String getProcInsturl = "rest/runtime/" + deploymentId + "/process/" + HUMAN_TASK_VAR_PROCESS_ID + "/";

        processInstance = RestUtil.get(deploymentUrl,
                                       getProcInsturl, contentType,
                                       200, user, password,
                                       JaxbProcessInstanceResponse.class);

        assertNotNull( "Null process instance using GET operation", processInstance );
        assertEquals( "Process instance id", procInstId, processInstance.getId() );

        // query tasks for associated task Id
        Map<String, String> queryparams = new HashMap<String, String>();
        queryparams.put("processInstanceId", String.valueOf(procInstId));
        JaxbTaskSummaryListResponse taskSumlistResponse = get( "task/query", 200, queryparams, JaxbTaskSummaryListResponse.class);

        TaskSummary taskSum = findTaskSummaryByProcessInstanceId(procInstId, taskSumlistResponse.getResult());
        long taskId = taskSum.getId();

        // get task info
        org.kie.remote.jaxb.gen.Task task = get("task/" + taskId, 200, org.kie.remote.jaxb.gen.Task.class);
        assertEquals("Incorrect task id", taskId, task.getId().longValue());

        // start task
        JaxbGenericResponse resp = post("task/" + taskId + "/start", 200, JaxbGenericResponse.class);
        assertNotNull("Response from task start operation is null.", resp);

        // check task status
        task = get("task/" + taskId, 200, org.kie.remote.jaxb.gen.Task.class);
        assertNotNull("Response from task start operation is null.", resp);
        logger.debug("Task {}: status [{}] / owner [{}]", taskId, task.getTaskData().getStatus().toString(), task.getTaskData()
                .getActualOwner());

        // complete task
        String georgeVal = "George";
        formParams.clear();
        formParams.put("map_outUserName", georgeVal);
        resp = post("task/" + taskId + "/complete", 200, JaxbGenericResponse.class);

        JaxbHistoryLogList histResp = get("history/instance/" + procInstId + "/variable/userName", 200, JaxbHistoryLogList.class);
        List<AbstractJaxbHistoryObject> histList = histResp.getHistoryLogList();
        boolean georgeFound = false;
        for( AbstractJaxbHistoryObject<VariableInstanceLog> absVarLog : histList ) {
            VariableInstanceLog varLog = ((JaxbVariableInstanceLog) absVarLog).getResult();
            if( "userName".equals(varLog.getVariableId()) && georgeVal.equals(varLog.getValue()) ) {
                georgeFound = true;
            }
        }
        assertTrue("'userName' var with value '" + georgeVal + "' not found!", georgeFound);

        // get task content
        Content content = get("task/" + taskId + "/content", 200, Content.class);
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
        org.kie.remote.jaxb.gen.Task jsonTask = RestUtil.get(deploymentUrl,
                "task/" + taskId, MediaType.APPLICATION_JSON,
                200, user, password,
                org.kie.remote.jaxb.gen.Task.class);

        assertNotNull("No task retrieved!", jsonTask);
        assertEquals("task id", taskId, jsonTask.getId().intValue());

        // DOCS 3
        String signalProcessUrl = "rest/runtime/" + deploymentId + "/process/instance/" + procInstId + "/signal";

        formParams.clear();
        formParams.put("signal", "MySignal");
        formParams.put("event", "MySignal");
        RestUtil.postForm(deploymentUrl,
                signalProcessUrl, contentType,
                200, user, password,
                formParams,
                JaxbGenericResponse.class);

        // DOCS 2
        processInstance = get("runtime/" + deploymentId + "/process/instance/" + procInstId, 200, JaxbProcessInstanceResponse.class);
    }

    public void urlsHumanTaskGroupAssignment( URL deploymentUrl ) throws Exception {
        setRestInfo(deploymentUrl, user, password);

        // DOCS 1
        JaxbProcessInstanceResponse procInstResp = RestUtil.post(deploymentUrl,
                "rest/runtime/" + deploymentId + "/process/" + GROUP_ASSSIGNMENT_PROCESS_ID + "/start", contentType,
                200, MARY_USER, MARY_PASSWORD,
                JaxbProcessInstanceResponse.class);
        assertEquals(ProcessInstance.STATE_ACTIVE, procInstResp.getState());
        long procInstId = procInstResp.getId();

        // assert the task
        TaskSummary taskSummary = getTaskSummary(MARY_USER, MARY_PASSWORD, procInstId, Status.Ready);
        long taskId = taskSummary.getId();
        assertNull(taskSummary.getActualOwner());
        assertNull(taskSummary.getPotentialOwners());
        assertEquals("Task 1", taskSummary.getName());

        // complete 'Task 1' as mary
        post("task/" + taskId + "/claim", MARY_USER, MARY_PASSWORD, 200);
        post("task/" + taskId + "/start", MARY_USER, MARY_PASSWORD, 200);
        post("task/" + taskId + "/complete", MARY_USER, MARY_PASSWORD, 200);

        // now make sure that the next task has been assigned to the
        // correct person. it should be mary.
        taskSummary = getTaskSummary(MARY_USER, MARY_PASSWORD, procInstId, Status.Reserved);
        assertEquals("Task 2", taskSummary.getName());
        assertEquals(MARY_USER, taskSummary.getActualOwner().getId());
        taskId = taskSummary.getId();

        // complete 'Task 2' as john
        post("task/" + taskId + "/release", MARY_USER, MARY_PASSWORD, 200);
        post("task/" + taskId + "/start", JOHN_USER, JOHN_PASSWORD, 200);
        post("task/" + taskId + "/complete", JOHN_USER, JOHN_PASSWORD, 200);

        // now make sure that the next task has been assigned to the
        // correct person. it should be john.
        taskSummary = getTaskSummary(JOHN_USER, JOHN_PASSWORD, procInstId, Status.Reserved);
        assertEquals("Task 3", taskSummary.getName());
        assertEquals(JOHN_USER, taskSummary.getActualOwner().getId());
        taskId = taskSummary.getId();

        // complete 'Task 3' as john
        post("task/" + taskId + "/start", JOHN_USER, JOHN_PASSWORD, 200);
        post("task/" + taskId + "/complete", JOHN_USER, JOHN_PASSWORD, 200);

        // assert process finished
        JaxbProcessInstanceLog jaxbProcInstLog = RestUtil.get(deploymentUrl, "rest/history/instance/" + procInstId, contentType,
                200, MARY_USER, MARY_PASSWORD,
                JaxbProcessInstanceLog.class);
        ProcessInstanceLog procInstLog = jaxbProcInstLog.getResult();
        assertEquals("Process instance has not completed!", ProcessInstance.STATE_COMPLETED, procInstLog.getStatus().intValue());
    }

    private TaskSummary getTaskSummary( String user, String password, long processInstanceId, Status status ) throws Exception {
        JaxbTaskSummaryListResponse taskSumListResp = RestUtil.get(deploymentUrl,
                "rest/task/query?processInstanceId=" + processInstanceId + "&status=" + status.toString(), contentType,
                200, user, password,
                JaxbTaskSummaryListResponse.class);
        List<TaskSummary> taskSumList = taskSumListResp.getResult();
        assertEquals(1, taskSumList.size());
        return taskSumList.get(0);
    }

    public void urlsCommandsTaskCommands( URL deploymentUrl, String user, String password ) throws Exception {
        setRestInfo(deploymentUrl, user, password);

        RuntimeEngine runtimeEngine = getRemoteRuntimeEngine(deploymentUrl, user, password);
        KieSession ksession = runtimeEngine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);

        long processInstanceId = processInstance.getId();
        GetTasksByProcessInstanceIdCommand cmd = new GetTasksByProcessInstanceIdCommand();
        cmd.setProcessInstanceId(processInstanceId);
        cmd.setUserId(user);
        JaxbCommandResponse<?> response = executeCommand(deploymentUrl, user, password, deploymentId, cmd);

        long taskId = ((JaxbLongListResponse) response).getResult().get(0);
        assertTrue("task id is less than 0", taskId > 0);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", taskUserId);
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
        String originalType = this.contentType;
        setRestInfo(deploymentUrl, user, password, MediaType.APPLICATION_XML);

        // Start process
        JaxbCommandsRequest req = null;
        {
            StartProcessCommand cmd = new StartProcessCommand();
            cmd.setProcessId(HUMAN_TASK_PROCESS_ID);
            req = new JaxbCommandsRequest(deploymentId, cmd);
        }
        JaxbCommandsResponse cmdResponse = post("execute", 200, req, JaxbCommandsResponse.class);
        assertFalse("Exception received!", cmdResponse.getResponses().get(0) instanceof JaxbExceptionResponse);
        long procInstId = ((ProcessInstance) cmdResponse.getResponses().get(0)).getId();

        // query tasks
        {
            GetTasksByProcessInstanceIdCommand cmd = new GetTasksByProcessInstanceIdCommand();
            cmd.setProcessInstanceId(procInstId);
            req = new JaxbCommandsRequest(deploymentId, cmd);
        }
        cmdResponse = implSpecificSendCommandRequest(req, user, password);
        List<?> list = (List<?>) cmdResponse.getResponses().get(0).getResult();
        long taskId = (Long) list.get(0);

        // start task
        {
            StartTaskCommand cmd = new StartTaskCommand();
            cmd.setTaskId(taskId);
            cmd.setUserId(taskUserId);
            req = new JaxbCommandsRequest(deploymentId, cmd);
        }
        cmdResponse = RestUtil.postEntity(deploymentUrl, "rest/execute", contentType,
                200, user, password,
                req, JaxbCommandsResponse.class);
        assertTrue( "Expected empty response", cmdResponse.getResponses() == null || cmdResponse.getResponses().isEmpty() );

        // complete task
        Map<String, Object> results = new HashMap<String, Object>();
        results.put("myType", new MyType("serialization", 3224950));
        {
            CompleteTaskCommand cmd = new CompleteTaskCommand();
            cmd.setTaskId(taskId);
            cmd.setUserId(taskUserId);
            JaxbStringObjectPairArray arrayMap = ConversionUtil.convertMapToJaxbStringObjectPairArray(results);
            cmd.setData(arrayMap);
            req = new JaxbCommandsRequest(deploymentId, cmd);
        }
        cmdResponse = RestUtil.postEntity(deploymentUrl, "rest/execute", contentType,
                200, user, password,
                req, JaxbCommandsResponse.class);

        assertNotNull("Response is null", cmdResponse);

        // TODO: check that above has completed?
        this.contentType = originalType;
    }

    public void urlsHistoryLogs( URL deploymentUrl, String user, String password ) throws Exception {
        setRestInfo(deploymentUrl, user, password);

        // Start process
        // DOCS 1
        JaxbProcessInstanceResponse processInstance = post(
                "runtime/" + deploymentId + "/process/" + SCRIPT_TASK_VAR_PROCESS_ID + "/start?map_x=initVal",
                200,
                JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        // instances/
        {
            JaxbHistoryLogList historyResult = get("history/instances", 200, JaxbHistoryLogList.class);
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
        JaxbHistoryLogList jaxbHistoryLogList = get("history/instance/" + procInstId + "/variable/x", 200, JaxbHistoryLogList.class);
        List<AbstractJaxbHistoryObject> historyVarLogList = jaxbHistoryLogList.getHistoryLogList();
        assertTrue("Incorrect number of variable logs: " + historyVarLogList.size(), 4 <= historyVarLogList.size());

        // process/{procDefId}

        // variable/{varId}

        // variable/{varId}/{value}

        // history/variable/{varId}/instances

        // history/variable/{varId}/value/{val}/instances

    }

    public void urlsJsonJaxbStartProcess( URL deploymentUrl, String user, String password ) throws Exception {
        // DOCS 1
        String startProcessOper = "rest/runtime/" + deploymentId + "/process/" + HUMAN_TASK_PROCESS_ID + "/start";

        // XML
        RestUtil.post(deploymentUrl,
                startProcessOper, MediaType.APPLICATION_XML,
                200, user, password,
                JaxbProcessInstanceResponse.class);

        // JSON
        RestUtil.post(deploymentUrl,
                startProcessOper, MediaType.APPLICATION_JSON,
                200, user, password,
                JaxbProcessInstanceResponse.class);


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

    public void urlsStartScriptProcess( URL deploymentUrl, String user, String password ) throws Exception {
        setRestInfo(deploymentUrl, user, password);
        // Remote API setup

        String startProcessUrl = "runtime/" + deploymentId + "/process/" + SCRIPT_TASK_PROCESS_ID + "/start";

        // Start process
        JaxbProcessInstanceResponse jaxbProcInstResp = post(startProcessUrl, 200, JaxbProcessInstanceResponse.class);
        ProcessInstance procInst = jaxbProcInstResp.getResult();

        int procStatus = procInst.getState();
        assertEquals("Incorrect process status: " + procStatus, ProcessInstance.STATE_COMPLETED, procStatus);
    }

    public void urlsVariableHistory( URL deploymentUrl, String user, String password ) throws Exception {
        setRestInfo(deploymentUrl, user, password);

        // Remote API setup
        String varId = "myobject";
        String varVal = UUID.randomUUID().toString();

        // proc log
        JaxbHistoryLogList jhll = get("history/variable/" + varId + "/instances", 200, JaxbHistoryLogList.class);
        int initHistSize = jhll.getResult().size();

        // start process
        JaxbProcessInstanceResponse procInstResp = post(
                "runtime/" + deploymentId + "/process/" + OBJECT_VARIABLE_PROCESS_ID + "/start?map_" + varId + "=" + varVal,
                200,
                JaxbProcessInstanceResponse.class);
        long procInstId = procInstResp.getResult().getId();

        // var log
        jhll = get("history/variable/" + varId, 200, JaxbHistoryLogList.class);
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
        jhll = get("history/variable/" + varId + "/instances", 200, JaxbHistoryLogList.class);

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
        setRestInfo(deploymentUrl, user, password);
        // test with normal RequestCreator

        JaxbDeploymentUnitList depList = get("deployment/", 200, JaxbDeploymentUnitList.class);
        assertNotNull("Null answer!", depList);
        assertNotNull("Null deployment list!", depList.getDeploymentUnitList());
        assertTrue("Empty deployment list!", depList.getDeploymentUnitList().size() > 0);

        String deploymentId = depList.getDeploymentUnitList().get(0).getIdentifier();
        JaxbDeploymentUnit dep = get("deployment/" + deploymentId, 200, JaxbDeploymentUnit.class);
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

    public void urlsGetRealProcessVariable( URL deploymentUrl, String user, String password ) throws Exception {
        setRestInfo(deploymentUrl, user, password);
        // Setup
        RuntimeEngine engine = getRemoteRuntimeEngine(deploymentUrl, user, password);

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

        JaxbProcessInstanceResponse jaxbProcInstResp = get(
                "runtime/" + deploymentId + "/process/instance/" + procInstId,
                200, JaxbProcessInstanceResponse.class);
        ProcessInstance procInst = jaxbProcInstResp.getResult();
        assertNotNull(procInst);
        assertEquals("Unequal process instance id.", procInstId, procInst.getId());

        MyType retrievedVar = get(
                "runtime/" + deploymentId + "/process/instance/" + procInstId + "/variable/" + varName,
                200, MyType.class);

        assertNotNull("Expected filled variable.", retrievedVar);
        assertEquals("Data integer doesn't match: ", retrievedVar.getData(), param.getData());
        assertEquals("Text string doesn't match: ", retrievedVar.getText(), param.getText());
    }

    public void urlsByteArrayProcessVariable( URL deploymentUrl, String user, String password ) throws Exception {
        setRestInfo(deploymentUrl, user, password);
        // Setup
        RuntimeEngine engine = getRemoteRuntimeEngine(deploymentUrl, user, password);

        // Start process
        byte [] bytes = "This is a short byte array".getBytes();
        MyBinaryType param = new MyBinaryType("wordperfect doc", bytes);
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
        String procInstVar = thisProcInstVarLog.getValue();
        assertNotNull("Null process instance variable!", procInstVar);

        JaxbProcessInstanceResponse jaxbProcInstResp = get(
                "runtime/" + deploymentId + "/process/instance/" + procInstId,
                200, JaxbProcessInstanceResponse.class);
        ProcessInstance procInst = jaxbProcInstResp.getResult();
        assertNotNull(procInst);
        assertEquals("Unequal process instance id.", procInstId, procInst.getId());

        MyBinaryType retrievedVar = get(
                "runtime/" + deploymentId + "/process/instance/" + procInstId + "/variable/" + varName,
                200, MyBinaryType.class);

        assertNotNull("Expected filled variable.", retrievedVar);
        assertEquals("Name from var doesn't match: ", param.getName(), retrievedVar.getName());
        String origStr = new String(param.getData());
        String retrievedStr = new String(retrievedVar.getData());
        assertEquals("Byte [] from var doesn't match: ", origStr, retrievedStr);
    }

    public void urlsCreateMemoryLeakOnTomcat( URL deploymentUrl, String user, String password, long timeout ) throws Exception {
        setRestInfo(deploymentUrl, user, password);
        long origCallDurationLimit = this.restCallDurationLimit;
        this.restCallDurationLimit = timeout;

        // Remote API setup
        try {
            for( int i = 0; i < 20; ++i ) {
                logger.info(i + " process started.");
                startProcessWithUserDefinedClass();
            }
        } finally {
            this.restCallDurationLimit = origCallDurationLimit;
        }
    }

    private void startProcessWithUserDefinedClass() throws Exception {
        setRestInfo(deploymentUrl, user, password);

        String varId = "myobject";
        JaxbProcessInstanceResponse procInstResp = post(
                "runtime/" + deploymentId + "/process/" + OBJECT_VARIABLE_PROCESS_ID + "/start?map_" + varId + "=10",
                200,
                JaxbProcessInstanceResponse.class);
        long procInstId = procInstResp.getResult().getId();

        assertTrue("Process instance should be larger than 0: " + procInstId, procInstId > 0);
    }

    public void urlsGetProcessDefinitions( URL deploymentUrl, String user, String password ) throws Exception {
        setRestInfo(deploymentUrl, user, password);

        // Start process
        JaxbProcessDefinitionList jaxbProcDefList = get("deployment/processes/", 200, JaxbProcessDefinitionList.class);

        assertNotNull( "Null return object",  jaxbProcDefList );
        assertNotNull( "Null proc def list", jaxbProcDefList.getProcessDefinitionList() );
        assertFalse( "Empty process definition list!",jaxbProcDefList.getProcessDefinitionList().isEmpty() );
        List<JaxbProcessDefinition> procDefList = jaxbProcDefList.getProcessDefinitionList();
        for( JaxbProcessDefinition jaxbProcDef : procDefList ) {
            validateProcessDefinition(jaxbProcDef);
        }

    }

    private void validateProcessDefinition( JaxbProcessDefinition procDef ) {
        String id = procDef.getId();
        assertFalse("Process def " + id + ": null deployment id", procDef.getDeploymentId() == null
                || procDef.getDeploymentId().isEmpty());
        assertFalse("Process def " + id + ": null name", procDef.getName() == null );
        assertFalse("Process def " + id + ": null pkg name", procDef.getPackageName() == null || procDef.getPackageName().isEmpty());
        assertFalse("Process def " + id + ": null version", procDef.getVersion() != null );
    }

    public void urlsDeploymentProcessDefinitions( URL deploymentUrl, String user, String password ) throws Exception {
        setRestInfo(deploymentUrl, user, password);
        JaxbProcessDefinitionList jaxbProcDefList = get("/deployment/processes", 200, JaxbProcessDefinitionList.class);

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

    public void urlsProcessQueryOperations( URL deploymentUrl, String user, String password ) throws Exception  {
        Map<String, String> queryParams = new HashMap<String, String>(1);
        queryParams.put("params", null);

        RestUtil.getQuery(deploymentUrl, "query/runtime/task", contentType, 400, user, password, queryParams);

        KieSession ksession = getRemoteRuntimeEngine(deploymentUrl, user, password).getKieSession();

        String val = UUID.randomUUID().toString();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "x", val );
        ProcessInstance procInst = ksession.startProcess(SCRIPT_TASK_VAR_PROCESS_ID, map);
        long procInstId = procInst.getId();

        queryParams.clear();
        queryParams.put("piid", "" + procInstId);

        JaxbQueryProcessInstanceResult queryResult = get("/query/runtime/process", 200, queryParams, JaxbQueryProcessInstanceResult.class);

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

    public void urlsQueryProcessInstancesNotFiltering( URL deploymentUrl, String user, String password ) throws Exception  {
        KieSession ksession = getRemoteRuntimeEngine(deploymentUrl, user, password).getKieSession();

        String prefix = random.nextInt(Integer.MAX_VALUE) + "-";
        long pids [] = runObjectVarProcess(ksession, prefix);

        Map<String, String> queryParams = new HashMap<String, String>(2);
        queryParams.put( "processinstancestatus", "1");
        queryParams.put( "varregex_myobject", prefix + "Hello.*");

        JaxbQueryProcessInstanceResult result = RestUtil.getQuery(
                deploymentUrl, "rest/query/runtime/process",
                MediaType.APPLICATION_XML, 200,
                user, password,
                queryParams, JaxbQueryProcessInstanceResult.class);

        assertNotNull( "Null result", result );
        assertFalse( "Empty result (all)", result.getProcessInstanceInfoList().isEmpty() );
        assertEquals( "Process instance info results", 2, result.getProcessInstanceInfoList().size() );
        for( JaxbQueryProcessInstanceInfo queryInfo : result.getProcessInstanceInfoList() ) {
           assertNotNull( "No process instance info!", queryInfo.getProcessInstance() );
           assertEquals( "No variable info!", 1, queryInfo.getVariables().size() );
           JaxbVariableInfo varInfo = queryInfo.getVariables().get(0);
           assertNotNull( "No variable info!", varInfo );
           String varValue = (String) varInfo.getValue();
           assertNotNull( "No variable value!", varValue );
           assertTrue( "Incorrect variable value", varValue.startsWith(prefix + "Hello"));
        }
    }

    protected long[] runObjectVarProcess( KieSession ksession, String prefix) {
        long[] pids = new long[3];

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("myobject", prefix + "Hello World!");

        ProcessInstance pi = ksession.startProcess(OBJECT_VARIABLE_PROCESS_ID, params); // completed
        pids[0] = pi.getId();
        params.put("myobject", prefix + "Hello Ivo!");
        pi = ksession.startProcess(OBJECT_VARIABLE_PROCESS_ID, params); // completed
        pids[1] = pi.getId();
        params.put("myobject", prefix + "Bye Ivo!");
        pi = ksession.startProcess(OBJECT_VARIABLE_PROCESS_ID, params); // completed
        pids[2] = pi.getId();

        return pids;
    }

    public void urlsAndRemoteApiDeploymentRedeployClassPath( URL deploymentUrl, String user, String password ) throws Exception {
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
            deployUtil.createRepositoryAndDeployProject(repoUrl, repositoryName, project, classpathDeploymentId, orgUnit);
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

        runRemoteApiClassPathProcessTest(runtimeEngine);

        // undeploy..
        deployUtil.undeploy(kDepUnit.getIdentifier());

        // .. and (re)deploy
        deployUtil.deploy(kDepUnit.getIdentifier());

        logger.info("Rerunning test.. is there a CNFE?");
        // Rerun process
        runRemoteApiClassPathProcessTest(runtimeEngine);
    }

    public void remoteApiStartScriptProcess(URL deploymentUrl, String user, String password) {
        // setup
        RuntimeEngine runtimeEngine = getRemoteRuntimeEngine(deploymentUrl, user, password);
        KieSession ksession = runtimeEngine.getKieSession();

        // start process
        ProcessInstance procInst = ksession.startProcess(SCRIPT_TASK_PROCESS_ID);
        int procStatus = procInst.getState();

        assertEquals("Incorrect process status: " + procStatus, ProcessInstance.STATE_COMPLETED, procStatus);
    }

    public void remoteApiExceptionNoDeployment(URL deploymentUrl, String user, String password) throws Exception {
        RuntimeEngine engine = RemoteRuntimeEngineFactory.newRestBuilder()
                .addDeploymentId("non-existing-deployment")
                .addUrl(deploymentUrl)
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

    public void remoteApiHistoryVariables(URL deploymentUrl) {
        RuntimeEngine runtimeEngine = getRemoteRuntimeEngine(deploymentUrl, JOHN_USER, JOHN_PASSWORD);

        runRemoteApiHistoryVariablesTest(runtimeEngine);
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
        setRestInfo(deploymentUrl, user, password);

        // Remote API setup
        // @formatter:off
        RemoteRestRuntimeEngineBuilder builder = RemoteRuntimeEngineFactory.newRestBuilder()
                .addDeploymentId(deploymentId)
                .addUrl(deploymentUrl)
                .addUserName(user)
                .addPassword(password);
        // @formatter:on
        RuntimeEngine engine = builder.build();
        KieSession ksession = engine.getKieSession();

        // 1. start process
        ProcessInstance processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        assertNotNull("Null ProcessInstance!", processInstance);
        long procInstId = processInstance.getId();


        JaxbLongListResponse response = get("runtime/" + deploymentId + "/workitem/", 200, JaxbLongListResponse.class);

        long workItemId = response.getResult().get(0);
        JaxbWorkItemResponse workItemResp = get("runtime/" + deploymentId + "/workitem/" + workItemId, 200, JaxbWorkItemResponse.class);
        assertNotNull( "Null response", workItemResp );
        WorkItem workItem = workItemResp.getResult();
        assertNotNull( "Null work item result", workItemResp.getResult() );

        // @formatter:off
        TaskService nullDepIdTaskService = RemoteRuntimeEngineFactory.newRestBuilder()
                .addUrl(deploymentUrl)
                .addUserName(user)
                .addDeploymentId(null)
                .addPassword(password)
                .build().getTaskService();
        // @formatter:on

        // @formatter:off
        TaskService emptyDepIdTaskService = RemoteRuntimeEngineFactory.newRestBuilder()
                .addUrl(deploymentUrl)
                .addUserName(user)
                .addDeploymentId("")
                .addPassword(password)
                .build().getTaskService();
        // @formatter:on

        // 2c. Another way to find the task
        // (testing TaskQueryWhereCommand based operations. )
        List<TaskSummary> tasks = nullDepIdTaskService.getTasksAssignedAsPotentialOwnerByProcessId(taskUserId, HUMAN_TASK_PROCESS_ID);
        long taskId = findTaskIdByProcessInstanceId(procInstId, tasks);

        // 2. find task (without deployment id)
        long sameTaskId;
        {
            List<Long> taskIds = nullDepIdTaskService.getTasksByProcessInstanceId(procInstId);
            assertEquals("Incorrect number of tasks for started process: ", 1, taskIds.size());
            sameTaskId = taskIds.get(0);
        }
        assertEquals( "Did not find the same task!", taskId, sameTaskId );

        // 2b. Get the task instance itself
        Task task = nullDepIdTaskService.getTaskById(taskId);
        checkReturnedTask(task, taskId);
        String actualOwnerId = task.getTaskData().getActualOwner().getId();

        TaskSummary taskSum = getTaskSummary(user, password, procInstId, task.getTaskData().getStatus());
        assertNotNull( "Empty actual owner user in task summary", taskSum.getActualOwner() );
        assertEquals( "Incorrect actual owner user in task summary", actualOwnerId, taskSum.getActualOwner().getId() );

        // 3. Start the task
        emptyDepIdTaskService.start(taskId, taskUserId);

        // 4. configure remote api client with deployment id
        // @formatter:off
        TaskService depIdTaskService = RemoteRuntimeEngineFactory.newRestBuilder()
                .addUrl(deploymentUrl)
                .addUserName(user)
                .addDeploymentId(deploymentId) // set new deployment id in task
                .addPassword(password).build().getTaskService();
        // @formatter:on

        // 5. complete task with TaskService instance that *has a deployment id*
        emptyDepIdTaskService.complete(taskId, taskUserId, null);

        // the second time should fail!
        try {
            depIdTaskService.complete(taskId, taskUserId, null);
            fail("Should not be able to complete task " + taskId + " a second time.");
        } catch( Throwable t ) {
            logger.info("The above exception was an expected part of the test.");
            // do nothing
        }

        Map<String, Object> contentMap = nullDepIdTaskService.getTaskContent(taskId);
        assertFalse( "Empty content map", contentMap == null || contentMap.isEmpty() );

        org.kie.api.task.model.Content content = nullDepIdTaskService.getContentById(task.getTaskData().getDocumentContentId());
        if( content != null && content.getContent() != null ) {
            Object contentMapObj = ContentMarshallerHelper.unmarshall(content.getContent(), null);
            contentMap = (Map<String, Object>) contentMapObj;
            assertFalse( "Empty content map", contentMap == null || contentMap.isEmpty() );
        } else  {
            assertNotNull("No task content found" , content);
        }

        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        List<TaskSummary> taskSums = nullDepIdTaskService.getTasksByStatusByProcessInstanceId(procInstId, statuses, "en-UK");
        assertEquals("Expected 2 tasks.", 2, taskSums.size());
    }

    public void remoteApiHumanTaskGroupId( URL deploymentUrl ) {
        RemoteRestRuntimeEngineBuilder runtimeEngineBuilder = RemoteRestRuntimeEngineFactory.newBuilder()
                .addDeploymentId(deploymentId).addUrl(deploymentUrl);

        RuntimeEngine krisRemoteEngine = runtimeEngineBuilder.addUserName(KRIS_USER).addPassword(KRIS_PASSWORD).build();
        RuntimeEngine maryRemoteEngine = runtimeEngineBuilder.addUserName(MARY_USER).addPassword(MARY_PASSWORD).build();
        RuntimeEngine johnRemoteEngine = runtimeEngineBuilder.addUserName(JOHN_USER).addPassword(JOHN_PASSWORD).build();

        runHumanTaskGroupIdTest(krisRemoteEngine, johnRemoteEngine, maryRemoteEngine);
    }

    public void remoteApiHumanTaskGroupVarAssign( URL deploymentUrl ) {
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

    public void remoteApiHumanTaskOwnType( URL deploymentUrl ) {
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

    public void remoteApiGroupAssignmentEngineering( URL deploymentUrl ) throws Exception {
        RuntimeEngine runtimeEngine
            = RemoteRuntimeEngineFactory.newRestBuilder()
            .addDeploymentId(deploymentId)
            .addUserName(MARY_USER)
            .addPassword(MARY_PASSWORD)
            .addUrl(deploymentUrl)
            .build();

        runRemoteApiGroupAssignmentEngineeringTest(runtimeEngine, runtimeEngine);
    }

    public void remoteApiProcessInstances( URL deploymentUrl, String user, String password ) throws Exception {
        // setup
        RuntimeEngine engine = getRemoteRuntimeEngine(deploymentUrl, user, password);

        runRemoteApiProcessInstances(engine);
    }

    public void remoteApiExtraJaxbClasses( URL deploymentUrl, String user, String password ) throws Exception {
        RuntimeEngine engine = getRemoteRuntimeEngine(deploymentUrl, user, password);

        testClassSerialization(deploymentUrl, engine, user, password, this);
    }

    private static final Map<Class, Class> objectWrapperClassMap = new HashMap<Class, Class>(12);
    static {
        objectWrapperClassMap.put(Boolean.class, JaxbBoolean.class);
        objectWrapperClassMap.put(Byte.class, JaxbByte.class);
        objectWrapperClassMap.put(Character.class, JaxbCharacter.class);
        objectWrapperClassMap.put(Short.class, JaxbShort.class);
        objectWrapperClassMap.put(Integer.class, JaxbInteger.class);
        objectWrapperClassMap.put(Long.class, JaxbLong.class);
        objectWrapperClassMap.put(Double.class, JaxbDouble.class);
        objectWrapperClassMap.put(Float.class, JaxbFloat.class);
        objectWrapperClassMap.put(String.class, JaxbString.class);
        objectWrapperClassMap.put(ArrayList.class, JaxbList.class);
        objectWrapperClassMap.put(HashSet.class, JaxbSet.class);
        objectWrapperClassMap.put(HashMap.class, JaxbMap.class);
    }

    @Override
    public void implSpecificTestParamSerialization(URL deploymentUrl, String user, String password, RuntimeEngine engine, Object obj) {
        setRestInfo(deploymentUrl, user, password);

        long procInstId = testParamSerialization(engine, obj);

        Class objClass = obj.getClass();
        Class wrapperClass =  objClass;

        boolean useJson = false;

        String origContentType = this.contentType;
        if( useJson ) {
            this.contentType = MediaType.APPLICATION_JSON.toString();
        } else {
            wrapperClass = objectWrapperClassMap.get(objClass);
            if( wrapperClass == null ) {
                wrapperClass = objClass;
                if( objClass.isArray() ) {
                    wrapperClass = JaxbArray.class;
                }
            }
        }

        Object retrievedObj = get(
                "runtime/" + deploymentId + "/process/instance/" + procInstId + "/variable/" + PARAM_SERIALIZATION_PARAM_NAME,
                200, wrapperClass);

        assertNotNull("Expected filled wrapper.", retrievedObj);
        if( retrievedObj instanceof JaxbType ) {
            JaxbType wrapper = (JaxbType) retrievedObj;
            assertNotNull("Expected filled variable.", wrapper.getValue());
            retrievedObj = wrapper.getValue();
        }
        assertThat( "Object: " + obj.getClass().getSimpleName(), retrievedObj, Is.is(obj));

        this.contentType = origContentType;
    }


    public void remoteApiRuleTaskProcess( URL deploymentUrl, String user, String password ) {
        // Remote API setup
        RuntimeEngine runtimeEngine = getRemoteRuntimeEngine(deploymentUrl, user, password);

        // runTest
        runRuleTaskProcess(runtimeEngine.getKieSession(), runtimeEngine.getAuditService());
    }


    public void remoteApiFunnyCharacters( URL deploymentUrl, String user, String password ) throws Exception  {
        RuntimeEngine runtimeEngine = getRemoteRuntimeEngine(deploymentUrl, user, password);

        runRemoteApiFunnyCharactersTest(runtimeEngine);
    }

    protected void runRemoteApiClassPathProcessTest( RuntimeEngine runtimeEngine ) {
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

    public void remoteApiCorrelationKeyOperations( URL deploymentUrl, String user, String password ) throws Exception {
        // setup
        setRestInfo(deploymentUrl, user, password);

        runRemoteApiCorrelationKeyTest(deploymentUrl, user, password, this);
    }

    @Override
    public RuntimeEngine getCorrelationPropertiesRemoteEngine(URL deploymentUrl, String deploymentId, String user, String password, String... businessKeys) {
        // @formatter:off
        RemoteRestRuntimeEngineBuilder builder = RemoteRuntimeEngineFactory.newRestBuilder()
                .addUrl(deploymentUrl)
                .addDeploymentId(deploymentId)
                // Add correlation key property to builder!
                .addCorrelationProperties(businessKeys)
                .addUserName(user)
                .addPassword(password);
        // @formatter:on

        return builder.build();
    }

    public void remoteApiHumanTaskComment( URL deploymentUrl, String user, String password ) throws Exception {
        // setup
        setRestInfo(deploymentUrl, user, password);
        RuntimeEngine runtimeEngine = getRemoteRuntimeEngine(deploymentUrl, user, password);

        runRemoteApiHumanTaskCommentTest(runtimeEngine);
    }

}
