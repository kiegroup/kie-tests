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
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.MAX_TRIES;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.findTaskId;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.findTaskSummary;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runHumanTaskGroupIdTest;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.util.Base64;
import org.codehaus.jackson.map.ObjectMapper;
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
import org.kie.remote.client.jaxb.ConversionUtil;
import org.kie.remote.client.jaxb.JaxbCommandsRequest;
import org.kie.remote.client.jaxb.JaxbCommandsResponse;
import org.kie.remote.client.jaxb.JaxbTaskSummaryListResponse;
import org.kie.remote.client.rest.KieRemoteHttpRequest;
import org.kie.remote.client.rest.KieRemoteHttpResponse;
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
import org.kie.services.client.api.builder.RemoteRestRuntimeEngineBuilder;
import org.kie.services.client.api.command.RemoteRuntimeEngine;
import org.kie.services.client.serialization.JaxbSerializationProvider;
import org.kie.services.client.serialization.JsonSerializationProvider;
import org.kie.services.client.serialization.SerializationException;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbLongListResponse;
import org.kie.services.client.serialization.jaxb.impl.audit.AbstractJaxbHistoryObject;
import org.kie.services.client.serialization.jaxb.impl.audit.JaxbHistoryLogList;
import org.kie.services.client.serialization.jaxb.impl.audit.JaxbProcessInstanceLog;
import org.kie.services.client.serialization.jaxb.impl.audit.JaxbVariableInstanceLog;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentJobResult;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentUnit;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentUnit.JaxbDeploymentStatus;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentUnitList;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessDefinition;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessDefinitionList;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessInstanceResponse;
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
    private int timeoutInSecs;
    private static final int DEFAULT_TIMEOUT = 10;

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
        this.timeoutInSecs = timeoutInSeconds;
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

        public Builder setTimeout( int timeout ) {
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

    private class RequestCreator {

        private final URL baseUrl;
        private final String userName;
        private final String password;
        private final MediaType contentType;

        public RequestCreator(URL baseUrl, String user, String password, MediaType mediaType) {
            StringBuilder urlString = new StringBuilder(baseUrl.toString());
            if( !urlString.toString().endsWith("/") ) {
                urlString.append("/");
            }
            urlString.append("rest/");
            try {
                this.baseUrl = new URL(urlString.toString());
            } catch(Exception e) { 
                e.printStackTrace();
                throw new IllegalStateException("Invalid url: " +  urlString, e);
            }
            this.userName = user;
            this.password = password;
            this.contentType = mediaType;
        }

        public KieRemoteHttpRequest createRequest( String relativeUrl ) {
            KieRemoteHttpRequest request = KieRemoteHttpRequest.newRequest(baseUrl).basicAuthorization(userName, password)
                    .relativeRequest(relativeUrl).accept(contentType.toString());
            return request;
        }
    }

    private JaxbSerializationProvider jaxbSerializationProvider;
    {
        jaxbSerializationProvider = JaxbSerializationProvider.clientSideInstance(MyType.class);
    }
    private JsonSerializationProvider jsonSerializationProvider = new JsonSerializationProvider();

    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    private long restCallDurationLimit = 2;

    private long sleep = 15 * 1000;

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

    private RemoteRuntimeEngineFactory getRemoteRuntimeFactory( URL deploymentUrl, String user, String password ) {
        return getRemoteRuntimeFactory(deploymentUrl, user, password, null);
    }

    private RemoteRuntimeEngineFactory getRemoteRuntimeFactory( URL deploymentUrl, String user, String password,
            Class... extraClasses ) {
        // @formatter:off
        RemoteRestRuntimeEngineBuilder builder = RemoteRestRuntimeEngineFactory.newBuilder()
                .addDeploymentId(deploymentId)
                .addUrl(deploymentUrl)
                .addUserName(user)
                .addPassword(password);
        // @formatter:on
        if( extraClasses != null && extraClasses.length > 0 ) {
            builder.addExtraJaxbClasses(extraClasses);
        }

        return builder.buildFactory();
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
            Assume.assumeFalse(checkDeployFlagFile());
        }

        RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, user, password);

        String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
        String repositoryName = "playground";
        String project = "integration-tests";
        String deploymentId = "org.test:kjar:1.0";
        String orgUnit = "integTestUser";
        deployUtil.createAndDeployRepository(repoUrl, repositoryName, project, deploymentId, orgUnit, user, 5);

        int sleep = 5;
        logger.info("Waiting {} more seconds to make sure deploy is done..", sleep);
        Thread.sleep(sleep * 1000);
    }

    private JaxbDeploymentJobResult deploy( KModuleDeploymentUnit depUnit, String user, String password, URL appUrl )
            throws Exception {
        // This code has been refactored but is essentially the same as the org.jboss.qa.bpms.rest.wb.RestWorkbenchClient code

        // Create request
        String url = appUrl.toExternalForm() + "deployment/" + depUnit.getIdentifier() + "/deploy";
        if( strategy.equals(RuntimeStrategy.SINGLETON) ) {
            url += "?strategy=" + strategy.toString();
        }
        KieRemoteHttpRequest request = KieRemoteHttpRequest.newRequest(url).basicAuthorization(user, password)
                .followRedirects(true);

        // POST request
        JaxbDeploymentJobResult result = null;
        try {
            result = post(request, 202, JaxbDeploymentJobResult.class);
        } catch( Exception ex ) {
            logger.error("POST operation failed.", ex);
            fail("POST operation failed.");
        }

        // Retrieve request
        try {
            String contentType = request.response().contentType();
            if( !MediaType.APPLICATION_JSON.equals(contentType) && !MediaType.APPLICATION_XML.equals(contentType) ) {
                // logger.error("Response body: {}",
                // get response as string
                // response.getEntity(String.class)
                // improve HTML readability
                // .replaceAll("><", ">\n<"));
                fail("Unexpected content-type: " + contentType);
            }
        } catch( Exception ex ) {
            logger.error("Unmarshalling failed.", ex);
            fail("Unmarshalling entity failed: " + ex.getMessage());
        }

        assertNotNull("Null response!", result);
        assertTrue("The deployment unit was not created successfully.", result.isSuccess());

        // wait for deploy to succeed
        RequestCreator requestCreator = new RequestCreator(appUrl, user, password, mediaType);
        waitForDeploymentJobToSucceed(depUnit, true, appUrl, requestCreator);

        return result;
    }

    private void undeploy( KModuleDeploymentUnit kDepUnit, URL deploymentUrl, RequestCreator requestCreator ) throws Exception {
        logger.info("undeploy");
        // Exists, so undeploy
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("deployment/" + kDepUnit.getIdentifier() + "/undeploy");

        JaxbDeploymentJobResult jaxbJobResult = postCheckTime(httpRequest, 202, JaxbDeploymentJobResult.class);

        assertEquals("Undeploy operation", jaxbJobResult.getOperation(), "UNDEPLOY");
        logger.info("UNDEPLOY : [" + jaxbJobResult.getDeploymentUnit().getStatus().toString() + "]"
                + jaxbJobResult.getExplanation());

        waitForDeploymentJobToSucceed(kDepUnit, false, deploymentUrl, requestCreator);
    }

    private void waitForDeploymentJobToSucceed( KModuleDeploymentUnit kDepUnit, boolean deploy, URL deploymentUrl,
            RequestCreator requestCreator ) throws Exception {
        boolean success = false;
        int tries = 0;
        while( !success && tries++ < MAX_TRIES ) {
            KieRemoteHttpRequest httpRequest = requestCreator.createRequest("deployment/" + kDepUnit.getIdentifier() + "/");
            logger.debug(">> " + httpRequest.getUri());
            httpRequest.get();
            success = isDeployRequestComplete(kDepUnit, deploy, httpRequest);
            if( !success ) {
                logger.info("Sleeping for " + sleep / 1000 + " seconds");
                Thread.sleep(sleep);
            }
        }
        assertTrue("No result after " + MAX_TRIES + " checks.", tries < MAX_TRIES);
    }

    private boolean isDeployed( KModuleDeploymentUnit kDepUnit, KieRemoteHttpRequest httpRequest ) {
        return isDeployRequestComplete(kDepUnit, true, httpRequest);
    }

    private boolean isDeployRequestComplete( KModuleDeploymentUnit kDepUnit, boolean deploy, KieRemoteHttpRequest httpRequest ) {
        try {
            KieRemoteHttpResponse httpResponse = httpRequest.get().response();
            int status = httpResponse.code();
            if( status == 200 ) {
                JaxbDeploymentUnit jaxbDepUnit = deserialize(httpResponse, JaxbDeploymentUnit.class);
                JaxbDeploymentStatus jaxbDepStatus = checkJaxbDeploymentUnitAndGetStatus(kDepUnit, jaxbDepUnit);
                if( deploy && jaxbDepStatus == JaxbDeploymentStatus.DEPLOYED ) {
                    return true;
                } else if( !deploy && !jaxbDepStatus.equals(JaxbDeploymentStatus.DEPLOYED) ) {
                    return true;
                } else {
                    return false;
                }
            } else {
                if( status < 500 ) {
                    if( deploy ) {
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    throw new IllegalStateException();
                }
            }
        } catch( Exception e ) {
            logger.error("Unable to check if '{}' deployed: {}", deploymentId, httpRequest.response().body());
            return false;
        } finally {
            httpRequest.disconnect();
        }
    }

    private JaxbDeploymentStatus checkJaxbDeploymentUnitAndGetStatus( KModuleDeploymentUnit expectedDepUnit,
            JaxbDeploymentUnit jaxbDepUnit ) {
        assertEquals("GroupId", expectedDepUnit.getGroupId(), jaxbDepUnit.getGroupId());
        assertEquals("ArtifactId", expectedDepUnit.getArtifactId(), jaxbDepUnit.getArtifactId());
        assertEquals("Version", expectedDepUnit.getVersion(), jaxbDepUnit.getVersion());
        return jaxbDepUnit.getStatus();
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
        String executeOp = "runtime/" + deploymentId + "/execute";
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

        httpRequest = requestCreator.createRequest("task/execute");
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
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RuntimeEngine engine = restSessionFactory.newRuntimeEngine();
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

        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RuntimeEngine runtimeEngine = restSessionFactory.newRuntimeEngine();
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
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("runtime/" + deploymentId + "/execute");

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
        URL url = new URL(deploymentUrl, deploymentUrl.getPath() + "runtime/" + deploymentId + "/process/"
                + SCRIPT_TASK_PROCESS_ID + "/start");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        String authString = user + ":" + password;
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authStringEnc = new String(authEncBytes);
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
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RuntimeEngine engine = restSessionFactory.newRuntimeEngine();
        KieSession ksession = engine.getKieSession();

        // start process
        ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        Collection<ProcessInstance> processInstances = ksession.getProcessInstances();
        assertNotNull("Null process instance list!", processInstances);
        assertTrue("No process instances started: " + processInstances.size(), processInstances.size() > 0);
    }

    public void remoteApiExtraJaxbClasses( URL deploymentUrl, String user, String password ) throws Exception {
        runRemoteApiExtraJaxbClassesTest(deploymentId, deploymentUrl, user, password);
    }

    private void runRemoteApiExtraJaxbClassesTest( String deploymentId, URL deploymentUrl, String user, String password )
            throws Exception {
        // Remote API setup
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RemoteRuntimeEngine engine = restSessionFactory.newRuntimeEngine();
        // test
        testExtraJaxbClassSerialization(engine);
    }

    public void remoteApiRuleTaskProcess( URL deploymentUrl, String user, String password ) {
        // Remote API setup
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RemoteRuntimeEngine runtimeEngine = restSessionFactory.newRuntimeEngine();

        // runTest
        runRuleTaskProcess(runtimeEngine.getKieSession(), runtimeEngine.getAuditLogService());
    }

    public void remoteApiGetTaskInstance( URL deploymentUrl, String user, String password ) throws Exception {
        MediaType origType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;

        // Remote API setup
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RemoteRuntimeEngine engine = restSessionFactory.newRuntimeEngine();

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
        checkReturnedTask((Task) jaxbTask, taskId);

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
        String varVal = "10";
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("runtime/" + deploymentId + "/process/"
                + OBJECT_VARIABLE_PROCESS_ID + "/start?map_" + varId + "=" + varVal);
        JaxbProcessInstanceResponse procInstResp = post(httpRequest, 200, JaxbProcessInstanceResponse.class);
        long procInstId = procInstResp.getResult().getId();

        // var log
        httpRequest = requestCreator.createRequest("history/variable/" + varId);
        JaxbHistoryLogList jhll = get(httpRequest, JaxbHistoryLogList.class);
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
        assertEquals("ProcessInstanceLog list size", piLogs.size(), 1);
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
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authStringEnc = new String(authEncBytes);
        connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        connection.setRequestMethod("GET");

        logger.debug(">> [GET] " + url.toExternalForm());
        connection.connect();
        int respCode = connection.getResponseCode();
        if( 200 != respCode ) {
            logger.warn(connection.getContent().toString());
        }
        assertEquals(200, respCode);

        JaxbSerializationProvider jaxbSerializer = JaxbSerializationProvider.clientSideInstance();
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
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RemoteRuntimeEngine engine = restSessionFactory.newRuntimeEngine();
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
        List<VariableInstanceLog> varLogList = (List<VariableInstanceLog>) engine.getAuditLogService().findVariableInstancesByName(
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
        JAXBElement<MyType> retrievedVarElem;
        try {
            JAXBElement elem = (new ObjectMapper()).readValue(xmlOrJsonStr, JAXBElement.class);
            MyType og = (MyType) elem.getValue();
            System.out.println("YES! : " + og.toString());
        } catch( Exception e ) {
            System.out.println("NO.");
            e.printStackTrace();
        }
        try {
            MyType og = (new ObjectMapper()).readValue(xmlOrJsonStr, MyType.class);
            System.out.println("JA! : " + og.toString());
        } catch( Exception e ) {
            System.out.println("NEE...");
            e.printStackTrace();
        }

        try {
            if( mediaType.equals(MediaType.APPLICATION_XML_TYPE) ) {
                retrievedVarElem = (JAXBElement<MyType>) jaxbSerializationProvider.deserialize(xmlOrJsonStr);
            } else if( mediaType.equals(MediaType.APPLICATION_JSON_TYPE) ) {
                jsonSerializationProvider.setDeserializeOutputClass(JAXBElement.class);
                retrievedVarElem = (JAXBElement<MyType>) jsonSerializationProvider.deserialize(xmlOrJsonStr);
            } else {
                throw new IllegalStateException("Unknown media type: " + mediaType.getType() + "/" + mediaType.getSubtype());
            }
        } catch( SerializationException se ) {
            logger.error("Could not deserialize string:\n{}", xmlOrJsonStr);
            throw se;
        }

        MyType retrievedVar = retrievedVarElem.getValue();
        assertNotNull("Expected filled variable.", retrievedVar);
        assertEquals("Data integer doesn't match: ", retrievedVar.getData(), param.getData());
        assertEquals("Text string doesn't match: ", retrievedVar.getText(), param.getText());
    }

    public void remoteApiHumanTaskGroupIdTest( URL deploymentUrl ) {
        RemoteRestRuntimeEngineBuilder runtimeEngineBuilder = RemoteRestRuntimeEngineFactory.newBuilder()
                .addDeploymentId(deploymentId).addUrl(deploymentUrl);

        RemoteRuntimeEngine krisRemoteEngine = runtimeEngineBuilder.addUserName(KRIS_USER).addPassword(KRIS_PASSWORD).build();
        RemoteRuntimeEngine maryRemoteEngine = runtimeEngineBuilder.addUserName(MARY_USER).addPassword(MARY_PASSWORD).build();
        RemoteRuntimeEngine johnRemoteEngine = runtimeEngineBuilder.addUserName(JOHN_USER).addPassword(JOHN_PASSWORD).build();

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
        RemoteRuntimeEngineFactory maryRemoteEngineFactory = RemoteRestRuntimeEngineFactory.newBuilder()
                .addDeploymentId(deploymentId).addUserName(MARY_USER).addPassword(MARY_PASSWORD).addUrl(deploymentUrl)
                .buildFactory();

        RuntimeEngine runtimeEngine = maryRemoteEngineFactory.newRuntimeEngine();

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
        RemoteRuntimeEngineFactory maryRemoteEngineFactory 
            = RemoteRestRuntimeEngineFactory.newBuilder()
                .addDeploymentId(deploymentId)
                .addUserName(JOHN_USER)
                .addPassword(JOHN_PASSWORD)
                .addUrl(deploymentUrl)
                .addExtraJaxbClasses(MyType.class)
                .buildFactory();
        // @formatter:on

        RemoteRuntimeEngine runtimeEngine = maryRemoteEngineFactory.newRuntimeEngine();
        runRemoteApiHumanTaskOwnTypeTest(runtimeEngine, runtimeEngine.getAuditLogService());
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
        RequestCreator requestCreator = new RequestCreator(deploymentUrl, user, password, mediaType);

        KModuleDeploymentUnit kDepUnit = new KModuleDeploymentUnit(GROUP_ID, CLASSPATH_ARTIFACT_ID, VERSION);
        String classpathDeploymentId = kDepUnit.getIdentifier();

        // Check that project is not deployed
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("deployment/" + classpathDeploymentId + "/");
        httpRequest.accept(getContentType());

        // Run test the first time
        if( !isDeployed(kDepUnit, httpRequest) ) {
            // Deploy
            deploy(kDepUnit, user, password, deploymentUrl);
        }

        // Run process
        RemoteRuntimeEngine runtimeEngine = RemoteRestRuntimeEngineFactory.newBuilder().addDeploymentId(classpathDeploymentId)
                .addUrl(deploymentUrl).addUserName(user).addPassword(password).build();

        runClassPathProcessTest(runtimeEngine);

        // undeploy..
        undeploy(kDepUnit, deploymentUrl, requestCreator);

        // .. and (re)deploy
        deploy(kDepUnit, user, password, deploymentUrl);

        logger.info("Rerunning test.. is there a CNFE?");
        // Rerun process
        runClassPathProcessTest(runtimeEngine);
    }

    private void runClassPathProcessTest( RemoteRuntimeEngine runtimeEngine ) {
        KieSession ksession = runtimeEngine.getKieSession();

        Map<String, Object> params = new HashMap<String, Object>();
        String varId = "myobject";
        String text = UUID.randomUUID().toString();
        params.put(varId, new MyType(text, 10));
        ProcessInstance procInst = ksession.startProcess(TestConstants.CLASSPATH_OBJECT_PROCESS_ID, params);
        long processInstanceId = procInst.getId();

        AuditService auditLogService = runtimeEngine.getAuditLogService();
        List<VariableInstanceLog> varLogList = (List<VariableInstanceLog>) auditLogService.findVariableInstances(processInstanceId);

        assertNotNull("Null variable instance found.", varLogList);
        for( VariableInstanceLog varLog : varLogList ) {
            logger.debug(varLog.getVariableId() + " (" + varLog.getValue() + ") ");
        }

        List<VariableInstanceLog> varLogs = (List<VariableInstanceLog>) runtimeEngine.getAuditLogService()
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
}
