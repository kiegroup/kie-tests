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
import static org.kie.remote.tests.base.RestUtil.checkResponsePostTime;
import static org.kie.remote.tests.base.RestUtil.get;
import static org.kie.remote.tests.base.RestUtil.getResponseEntity;
import static org.kie.remote.tests.base.RestUtil.post;
import static org.kie.remote.tests.base.RestUtil.setAcceptHeader;
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
import java.util.Properties;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.util.Base64;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.drools.core.command.runtime.process.GetProcessIdsCommand;
import org.drools.core.command.runtime.process.StartProcessCommand;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.spi.ReaderException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.process.audit.AuditLogService;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.process.audit.VariableInstanceLog;
import org.jbpm.process.audit.event.AuditEvent;
import org.jbpm.services.task.commands.CompleteTaskCommand;
import org.jbpm.services.task.commands.GetTaskCommand;
import org.jbpm.services.task.commands.GetTasksByProcessInstanceIdCommand;
import org.jbpm.services.task.commands.StartTaskCommand;
import org.jbpm.services.task.impl.model.xml.JaxbContent;
import org.jbpm.services.task.impl.model.xml.JaxbTask;
import org.junit.Assume;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskData;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.services.client.api.RemoteRestRuntimeEngineFactory;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.services.client.api.RestRequestHelper;
import org.kie.services.client.api.builder.RemoteRestRuntimeEngineBuilder;
import org.kie.services.client.api.command.RemoteRuntimeEngine;
import org.kie.services.client.serialization.JaxbSerializationProvider;
import org.kie.services.client.serialization.JsonSerializationProvider;
import org.kie.services.client.serialization.SerializationException;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandsRequest;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandsResponse;
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
import org.kie.services.client.serialization.jaxb.impl.task.JaxbTaskSummaryListResponse;
import org.kie.services.client.serialization.jaxb.rest.JaxbExceptionResponse;
import org.kie.services.client.serialization.jaxb.rest.JaxbGenericResponse;
import org.kie.services.shared.ServicesVersion;
import org.kie.tests.MyType;
import org.kie.tests.wb.base.util.TestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestIntegrationTestMethods extends AbstractIntegrationTestMethods {

    private static Logger logger = LoggerFactory.getLogger(RestIntegrationTestMethods.class);

    private static final String taskUserId = "salaboy";
    private static final String DEPLOY_FLAG_FILE_NAME = ".deployed";

    private final String deploymentId;
    private final KModuleDeploymentUnit deploymentUnit;
    private RuntimeStrategy strategy = RuntimeStrategy.SINGLETON;

    private MediaType mediaType;
    private int timeout;
    private static final int DEFAULT_TIMEOUT = 10;
  
    private RestIntegrationTestMethods(String deploymentId, MediaType mediaType, int timeout, RuntimeStrategy strategy) {
        if( mediaType == null ) { 
            mediaType = MediaType.APPLICATION_XML_TYPE;
        }
        if( strategy != null ) { 
            this.strategy = strategy;
        }
        
        this.deploymentId = deploymentId;
        this.deploymentUnit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION);
        assertEquals( "Deployment unit information", deploymentId, deploymentUnit.getIdentifier());
        this.mediaType = mediaType;
        this.timeout = timeout;
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
        
        public Builder setDeploymentId(String deploymentId) { 
            this.deploymentId = deploymentId; return this;
        }
        public Builder setStrategy(RuntimeStrategy strategy) { 
            this.strategy = strategy; return this;
        }
        public Builder setMediaType(MediaType mediaType) { 
            this.mediaType = mediaType; return this;
        }
        public Builder setTimeout(int timeout) { 
            this.timeout = timeout; return this;
        }
        public RestIntegrationTestMethods build() { 
            if( this.deploymentId == null ) { 
                throw new IllegalStateException("The deployment id must be set to create the test methods instance!");
            }
            return new RestIntegrationTestMethods(deploymentId, mediaType, timeout, strategy);
        }
    }
    
    private JaxbSerializationProvider jaxbSerializationProvider = new JaxbSerializationProvider();
    {
        jaxbSerializationProvider.addJaxbClasses(MyType.class);
    }
    private JsonSerializationProvider jsonSerializationProvider = new JsonSerializationProvider();

    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    private long restCallDurationLimit = 2000;

    private long sleep = 15*1000;


    /**
     * Helper methods
     */

    private void addToRequestBody(ClientRequest restRequest, Object obj) throws Exception {
        if (mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
            String body = jaxbSerializationProvider.serialize(obj);
            restRequest.body(mediaType, body);
        } else if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
            String body = jsonSerializationProvider.serialize(obj);
            restRequest.body(mediaType, body);
        }
    }

    private RestRequestHelper getRestRequestHelper(URL deploymentUrl, String user, String password) { 
        return RestRequestHelper.newInstance(deploymentUrl, user, password, timeout, mediaType);
    }
   
    private RemoteRuntimeEngineFactory getRemoteRuntimeFactory(URL deploymentUrl, String user, String password) { 
       return getRemoteRuntimeFactory(deploymentId, deploymentUrl, user, password);
    }
    
    private RemoteRuntimeEngineFactory getRemoteRuntimeFactory(String deploymentId, URL deploymentUrl, String user, String password) { 
        RemoteRestRuntimeEngineFactory r3eFactory = RemoteRestRuntimeEngineFactory.newBuilder()
                .addDeploymentId(deploymentId)
                .addUrl(deploymentUrl)
                .addUserName(user)
                .addPassword(password)
                .buildFactory();
        return r3eFactory;
    }
    
    /**
     * Test methods
     */

    public static boolean checkDeployFlagFile() throws Exception { 
        Properties props = new Properties();
        props.load(RestIntegrationTestMethods.class.getResourceAsStream("/test.properties"));
        String buildDir = (String) props.get("build.dir");

        String fileNameLocation = buildDir + "/" + DEPLOY_FLAG_FILE_NAME;
        File deployFlag = new File(fileNameLocation);
        if (!deployFlag.exists()) {
            PrintWriter output = null;
            try  {
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
            logger.debug("Deployed on " + string );
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
    public void urlsDeployModuleForOtherTests(URL deploymentUrl, String user, String password, boolean check) throws Exception {
        if( check ) { 
            Assume.assumeFalse(checkDeployFlagFile());
        }
        
        RestRepositoryDeploymentUtil deployUtil = new RestRepositoryDeploymentUtil(deploymentUrl, user, password);
       
        String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
        String repositoryName = "playground";
        String project = "integration-tests";
        String deploymentId = "org.test:kjar:1.0";
        String orgUnit = "integTestUser";
        deployUtil.createAndDeployRepository(repoUrl, repositoryName, project, deploymentId, orgUnit, user, 5);
        
        int sleep = 5;
        logger.info( "Waiting {} more seconds to make sure deploy is done..", sleep);
        Thread.sleep(sleep*1000);
    }

    private JaxbDeploymentJobResult deploy(KModuleDeploymentUnit depUnit, String user, String password, URL appUrl) throws Exception {
        // This code has been refactored but is essentially the same as the org.jboss.qa.bpms.rest.wb.RestWorkbenchClient code
    
        // Create request
        String url = appUrl.toExternalForm() + "rest/deployment/" + depUnit.getIdentifier() + "/deploy";
        if (strategy.equals(RuntimeStrategy.SINGLETON)) {
            url += "?strategy=" + strategy.toString();
        }
        ClientRequest request;
        String AUTH_HEADER = "Basic " + Base64.encodeBase64String(String.format("%s:%s", user, password).getBytes()).trim();

        DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient.getCredentialsProvider().setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM),
                new UsernamePasswordCredentials(user, password));
        ClientExecutor clientExecutor = new ApacheHttpClient4Executor(httpClient);
        ClientRequestFactory factory = new ClientRequestFactory(clientExecutor, ResteasyProviderFactory.getInstance());

        request = factory.createRequest(url).header("Authorization", AUTH_HEADER).accept(mediaType)
                .followRedirects(true);

        // POST request
        JaxbDeploymentJobResult result = null;
        ClientResponse<JaxbDeploymentJobResult> response = null;
        try {
            response = request.post(JaxbDeploymentJobResult.class);
        } catch (Exception ex) {
            logger.error("POST operation failed.", ex);
            fail("POST operation failed.");
        }
        assertNotNull("Response is null!", response);
    
        // Retrieve request
        try {
            String contentType = response.getHeaders().getFirst("Content-Type");
            if (MediaType.APPLICATION_JSON.equals(contentType) || MediaType.APPLICATION_XML.equals(contentType)) {
                result = getResponseEntity(response, JaxbDeploymentJobResult.class);
            } else {
                logger.error("Response body: {}",
                // get response as string
                        response.getEntity(String.class)
                        // improve HTML readability
                                .replaceAll("><", ">\n<"));
                fail("Unexpected content-type: " + contentType);
            }
        } catch (ReaderException ex) {
            response.resetStream();
            logger.error("Bad entity: [{}]", response.getEntity(String.class));
            fail("Bad entity: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unmarshalling failed.", ex);
            fail("Unmarshalling entity failed: " + ex.getMessage());
        }
   
        assertNotNull("Null response!", result);
        assertTrue("The deployment unit was not created successfully.", result.isSuccess());
      
        // wait for deploy to succeed
        RestRequestHelper requestHelper = getRestRequestHelper(appUrl, user, password);
        waitForDeploymentJobToSucceed(depUnit, true, appUrl, requestHelper);
    
        return result;
    }

    private void undeploy(KModuleDeploymentUnit kDepUnit, URL deploymentUrl, RestRequestHelper requestHelper) throws Exception {
        logger.info("undeploy");
        // Exists, so undeploy
        ClientRequest restRequest = requestHelper.createRequest("deployment/" + kDepUnit.getIdentifier() + "/undeploy");
    
        ClientResponse<?> responseObj = checkResponsePostTime(restRequest, mediaType, 202);
        JaxbDeploymentJobResult jaxbJobResult = responseObj.getEntity(JaxbDeploymentJobResult.class);
        
        assertEquals("Undeploy operation", jaxbJobResult.getOperation(), "UNDEPLOY");
        logger.info("UNDEPLOY : [" + jaxbJobResult.getDeploymentUnit().getStatus().toString() + "]"
                + jaxbJobResult.getExplanation());
   
        waitForDeploymentJobToSucceed(kDepUnit, false, deploymentUrl, requestHelper);
    }

    private void waitForDeploymentJobToSucceed(KModuleDeploymentUnit kDepUnit, boolean deploy, URL deploymentUrl, RestRequestHelper requestHelper) throws Exception {
        boolean success = false;
        int tries = 0;
        while (!success && tries++ < MAX_TRIES) {
            ClientRequest restRequest = requestHelper.createRequest("deployment/" + kDepUnit.getIdentifier() + "/");
            logger.debug(">> " + restRequest.getUri());
            ClientResponse<?> responseObj = restRequest.get();
            success = isDeployRequestComplete(kDepUnit, deploy, responseObj);
            if (!success) {
                logger.info("Sleeping for " + sleep / 1000 + " seconds");
                Thread.sleep(sleep);
            }
        }
        assertTrue( "No result after " + MAX_TRIES + " checks.", tries < MAX_TRIES );
    }

    private boolean isDeployed(KModuleDeploymentUnit kDepUnit, ClientResponse<?> responseObj) {
       return isDeployRequestComplete(kDepUnit, true, responseObj);
    }
    
    private boolean isDeployRequestComplete(KModuleDeploymentUnit kDepUnit, boolean deploy, ClientResponse<?> responseObj) {
        try {
            int status = responseObj.getStatus();
            if (status == 200) {
                JaxbDeploymentUnit jaxbDepUnit = getResponseEntity(responseObj, JaxbDeploymentUnit.class);
                JaxbDeploymentStatus jaxbDepStatus = checkJaxbDeploymentUnitAndGetStatus(kDepUnit, jaxbDepUnit);
                if( deploy && jaxbDepStatus == JaxbDeploymentStatus.DEPLOYED) {
                    return true;
                } else if( ! deploy && ! jaxbDepStatus.equals(JaxbDeploymentStatus.DEPLOYED)) { 
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
            responseObj.resetStream();
            logger.error( "Unable to check if '{}' deployed: {}", deploymentId, responseObj.getEntity(String.class)); 
            return false;
        } finally {
            responseObj.releaseConnection();
        }
    }

    private JaxbDeploymentStatus checkJaxbDeploymentUnitAndGetStatus(KModuleDeploymentUnit expectedDepUnit, JaxbDeploymentUnit jaxbDepUnit) {
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
    public void urlsStartHumanTaskProcess(URL deploymentUrl, String user, String password) throws Exception {
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        RestRequestHelper queryRequestHelper = getRestRequestHelper(deploymentUrl, JOHN_USER, JOHN_PASSWORD);

        // Start process
        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/" + HUMAN_TASK_PROCESS_ID + "/start");
        JaxbProcessInstanceResponse processInstance = post(restRequest, mediaType, JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        // query tasks for associated task Id
        restRequest = queryRequestHelper.createRequest("task/query?processInstanceId=" + procInstId);
        JaxbTaskSummaryListResponse taskSumlistResponse = get(restRequest, mediaType, JaxbTaskSummaryListResponse.class);

        TaskSummary taskSum = findTaskSummary(procInstId, taskSumlistResponse.getResult());
        long taskId = taskSum.getId();
        assertNotNull( "Null actual owner", taskSum.getActualOwner() );

        // get task info
        restRequest = requestHelper.createRequest("task/" + taskId);
        JaxbTask task = get(restRequest, mediaType,JaxbTask.class);
        assertEquals("Incorrect task id", taskId, task.getId().longValue());

        // start task
        restRequest = requestHelper.createRequest("task/" + taskId + "/start");
        JaxbGenericResponse resp = post(restRequest, mediaType, JaxbGenericResponse.class);
        assertNotNull("Response from task start is null.", resp);

        // get task info
        restRequest = requestHelper.createRequest("task/" + taskId);
        JaxbTask jaxbTask = get(restRequest, mediaType, JaxbTask.class);
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
    public void urlsCommandsStartProcess(URL deploymentUrl, String user, String password) throws Exception {
        MediaType originalType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;

        RestRequestHelper helper = getRestRequestHelper(deploymentUrl, user, password);

        // Start process
        String executeOp = "runtime/" + deploymentId + "/execute";
        ClientRequest restRequest = helper.createRequest(executeOp);
        JaxbCommandsRequest commandMessage = new JaxbCommandsRequest(deploymentId, new StartProcessCommand(HUMAN_TASK_PROCESS_ID));
        commandMessage.setVersion(ServicesVersion.VERSION);
        addToRequestBody(restRequest, commandMessage);

        logger.debug(">> [startProcess] " + restRequest.getUri());
        JaxbCommandsResponse cmdsResp = post(restRequest, mediaType, JaxbCommandsResponse.class);
        assertFalse("Exception received!", cmdsResp.getResponses().get(0) instanceof JaxbExceptionResponse);
        long procInstId = ((ProcessInstance) cmdsResp.getResponses().get(0)).getId();

        // query tasks
        restRequest = helper.createRequest(executeOp);
        commandMessage = new JaxbCommandsRequest(deploymentId, new GetTasksByProcessInstanceIdCommand(procInstId));
        commandMessage.setVersion(ServicesVersion.VERSION);
        addToRequestBody(restRequest, commandMessage);

        logger.debug(">> [getTasksByProcessInstanceId] " + restRequest.getUri());
        JaxbCommandsResponse cmdResponse = post(restRequest, mediaType, JaxbCommandsResponse.class);
        List<?> list = (List<?>) cmdResponse.getResponses().get(0).getResult();
        long taskId = (Long) list.get(0);

        // start task

        logger.debug(">> [startTask] " + restRequest.getUri());
        restRequest = helper.createRequest(executeOp);
        commandMessage = new JaxbCommandsRequest(new StartTaskCommand(taskId, taskUserId));
        commandMessage.setVersion(ServicesVersion.VERSION);
        addToRequestBody(restRequest, commandMessage);

        // Get response
        ClientResponse<?> responseObj = post(restRequest, mediaType);
        responseObj.releaseConnection();

        restRequest = helper.createRequest("task/execute");
        Map<String, Object> results = new HashMap<String, Object>();
        results.put("myType", new MyType("serialization", 3224950));
        commandMessage = new JaxbCommandsRequest(new CompleteTaskCommand(taskId, taskUserId, results));
        commandMessage.setVersion(ServicesVersion.VERSION);
        addToRequestBody(restRequest, commandMessage);

        // Get response
        logger.debug(">> [completeTask] " + restRequest.getUri());
        JaxbCommandsResponse jaxbResp = post(restRequest, mediaType, JaxbCommandsResponse.class);
        assertNotNull( "Response is null", jaxbResp);
        
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
    public void remoteApiHumanTaskProcess(URL deploymentUrl, String user, String password) throws Exception {
        // create REST request
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RuntimeEngine engine = restSessionFactory.newRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        assertNotNull( "Null ProcessInstance!", processInstance);
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
        } catch (Throwable t) {
            logger.info("The above exception was an expected part of the test.");
            // do nothing
        }

        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        List<TaskSummary> taskIds = taskService.getTasksByStatusByProcessInstanceId(procInstId, statuses, "en-UK");
        assertEquals("Expected 2 tasks.", 2, taskIds.size());
    }

    public void urlsCommandsTaskCommands(URL deploymentUrl, String user, String password) throws Exception {
        MediaType origType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;
        
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RuntimeEngine runtimeEngine = restSessionFactory.newRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);

        long processInstanceId = processInstance.getId();
        JaxbCommandResponse<?> response = executeCommand(deploymentUrl, user, password, deploymentId,
                new GetTasksByProcessInstanceIdCommand(processInstanceId));

        long taskId = ((JaxbLongListResponse) response).getResult().get(0);
        assertTrue("task id is less than 0", taskId > 0);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", taskUserId);
        
        this.mediaType = origType;
    }

    private JaxbCommandResponse<?> executeCommand(URL appUrl, String user, String password, String deploymentId, Command<?> command) throws Exception {
        MediaType originalMediaType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;

        RestRequestHelper requestHelper = getRestRequestHelper(appUrl, user, password);

        List<Command> commands = new ArrayList<Command>();
        commands.add(command);

        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/execute");

        JaxbCommandsRequest commandMessage = new JaxbCommandsRequest(commands);
        assertNotNull("Commands are null!", commandMessage.getCommands());
        assertTrue("Commands are empty!", commandMessage.getCommands().size() > 0);

        addToRequestBody(restRequest, commandMessage);

        logger.debug(">> [" + command.getClass().getSimpleName() + "] " + restRequest.getUri());
        JaxbCommandsResponse cmdsResp = post(restRequest, mediaType, JaxbCommandsResponse.class);

        this.mediaType = originalMediaType;
        return cmdsResp.getResponses().get(0);
    }

    public void urlsHistoryLogs(URL deploymentUrl, String user, String password) throws Exception {
        RestRequestHelper helper = getRestRequestHelper(deploymentUrl, user, password);

        // Start process
        ClientRequest restRequest = helper.createRequest("runtime/" + deploymentId + "/process/" + SCRIPT_TASK_VAR_PROCESS_ID + "/start?map_x=initVal");
        logger.debug(">> " + restRequest.getUri());
        JaxbProcessInstanceResponse processInstance = post(restRequest, mediaType, JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        // instances/
        {
            restRequest = helper.createRequest("history/instances");
            logger.debug(">> [runtime] " + restRequest.getUri());
            JaxbHistoryLogList historyResult = get(restRequest, mediaType, JaxbHistoryLogList.class);
            List<AuditEvent> historyLogList = historyResult.getResult();

            for (AuditEvent event : historyLogList) {
                assertTrue("ProcessInstanceLog", event instanceof ProcessInstanceLog);
                ProcessInstanceLog procLog = (ProcessInstanceLog) event;
                Object[][] out = { { procLog.getDuration(), "duration" }, { procLog.getEnd(), "end date" },
                        { procLog.getExternalId(), "externalId" }, { procLog.getId(), "id" },
                        { procLog.getIdentity(), "identity" }, { procLog.getOutcome(), "outcome" },
                        { procLog.getParentProcessInstanceId(), "parent proc id" }, { procLog.getProcessId(), "process id" },
                        { procLog.getProcessInstanceId(), "process instance id" }, { procLog.getProcessName(), "process name" },
                        { procLog.getProcessVersion(), "process version" }, { procLog.getStart(), "start date" },
                        { procLog.getStatus(), "status" } };
                for (int i = 0; i < out.length; ++i) {
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
        restRequest = helper.createRequest("history/instance/" + procInstId + "/variable/x");
        logger.debug(">> [runtime]" + restRequest.getUri());
        JaxbHistoryLogList historyLogList = get(restRequest, mediaType, JaxbHistoryLogList.class);
        List<AbstractJaxbHistoryObject> historyVarLogList = historyLogList.getHistoryLogList();
        
        restRequest = helper.createRequest("history/instance/" + procInstId + "/variable/x");
        logger.debug(">> [runtime]" + restRequest.getUri());
        JaxbHistoryLogList runtimeLogList = get(restRequest, mediaType, JaxbHistoryLogList.class);
        List<AbstractJaxbHistoryObject> runtimeVarLogList = runtimeLogList.getHistoryLogList();
        assertTrue("Incorrect number of variable logs: " + runtimeVarLogList.size(), 4 <= runtimeVarLogList.size());

        assertEquals( "history list size", historyVarLogList.size(), runtimeVarLogList.size());
        
        for(int i = 0; i < runtimeVarLogList.size(); ++i) {
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

    public void urlsJsonJaxbStartProcess(URL deploymentUrl, String user, String password) throws Exception {
        MediaType origType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        
        // XML
        String startProcessOper = "runtime/" + deploymentId + "/process/" + HUMAN_TASK_PROCESS_ID + "/start";
        ClientRequest restRequest = requestHelper.createRequest(startProcessOper);
        logger.debug(">> " + restRequest.getUri());
        String result = post(restRequest, mediaType, String.class);
        assertTrue("Doesn't start like a JAXB string!", result.startsWith("<"));

        // JSON
        this.mediaType = MediaType.APPLICATION_JSON_TYPE;
        requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        restRequest = requestHelper.createRequest(startProcessOper);
        logger.debug(">> " + restRequest.getUri());
        result = post(restRequest, mediaType, String.class);
        if( ! result.startsWith("{") ) { 
            logger.error( "Should be JSON:\n" + result );
            fail("Doesn't start like a JSON string!");
        }
        
        this.mediaType = origType;
    }

    public void urlsHumanTaskWithVariableChangeFormParameters(URL deploymentUrl, String user, String password) throws Exception {
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);

        // Start process
        ClientRequest restRequest = requestHelper.createRequest(
                "runtime/" + deploymentId + "/process/" + HUMAN_TASK_VAR_PROCESS_ID + "/start");
        restRequest.formParameter("map_userName", "John");
        JaxbProcessInstanceResponse processInstance = post(restRequest, mediaType, JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        // query tasks for associated task Id
        restRequest = requestHelper.createRequest("task/query");
        restRequest.formParameter("processInstanceId", String.valueOf(procInstId));
        JaxbTaskSummaryListResponse taskSumlistResponse = get(restRequest, mediaType, JaxbTaskSummaryListResponse.class);
        
        TaskSummary taskSum = findTaskSummary(procInstId, taskSumlistResponse.getResult());
        long taskId = taskSum.getId();

        // start task
        restRequest = requestHelper.createRequest("task/" + taskId + "/start");
        JaxbGenericResponse resp = post(restRequest, mediaType,JaxbGenericResponse.class);
        assertNotNull("Response from task start operation is null.", resp);

        // check task status
        restRequest = requestHelper.createRequest("task/" + taskId);
        JaxbTask task  = get(restRequest, mediaType, JaxbTask.class);
        assertNotNull("Response from task start operation is null.", resp);
        logger.debug("Task {}: status [{}] / owner [{}]", 
                taskId, task.getTaskData().getStatus().toString(), task.getTaskData().getActualOwner().getId() );

        // complete task
        String georgeVal = "George";
        restRequest = requestHelper.createRequest("task/" + taskId + "/complete");
        restRequest.formParameter("map_outUserName", georgeVal);
        resp = post(restRequest, mediaType, JaxbGenericResponse.class);

        restRequest = requestHelper.createRequest("history/instance/" + procInstId + "/variable/userName");
        ClientResponse<?> responseObj = get(restRequest, mediaType);
        responseObj.releaseConnection();
        
        restRequest = requestHelper.createRequest("history/instance/" + procInstId + "/variable/userName");
        JaxbHistoryLogList histResp = get(restRequest, mediaType, JaxbHistoryLogList.class);
        List<AbstractJaxbHistoryObject> histList = histResp.getHistoryLogList();
        boolean georgeFound = false;
        for (AbstractJaxbHistoryObject<VariableInstanceLog> absVarLog : histList) {
            VariableInstanceLog varLog = ((JaxbVariableInstanceLog) absVarLog).getResult();
            if ("userName".equals(varLog.getVariableId()) && georgeVal.equals(varLog.getValue())) {
                georgeFound = true;
            }
        }
        assertTrue("'userName' var with value '" + georgeVal + "' not found!", georgeFound);
    }

    public void urlsHttpURLConnectionAcceptHeaderIsFixed(URL deploymentUrl, String user, String password) throws Exception {
        URL url = new URL(deploymentUrl, deploymentUrl.getPath() + "rest/runtime/" + deploymentId + "/process/"
                + SCRIPT_TASK_PROCESS_ID + "/start");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        String authString = user + ":" + password;
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authStringEnc = new String(authEncBytes);
        connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        connection.setRequestMethod("POST");

        logger.debug(">> [POST] " + url.toExternalForm());
        connection.connect();
        if (200 != connection.getResponseCode()) {
            logger.warn(connection.getContent().toString());
        }
        assertEquals(200, connection.getResponseCode());
    }

    public void remoteApiSerialization(URL deploymentUrl, String user, String password) throws Exception {
        // setup
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RuntimeEngine engine = restSessionFactory.newRuntimeEngine();
        KieSession ksession = engine.getKieSession();
       
        // start process
        ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        Collection<ProcessInstance> processInstances = ksession.getProcessInstances();
        assertNotNull("Null process instance list!", processInstances );
        assertTrue("No process instances started.", processInstances.size() > 0);
    }

    public void remoteApiExtraJaxbClasses(URL deploymentUrl, String user, String password) throws Exception {
        runRemoteApiExtraJaxbClassesTest(deploymentId, deploymentUrl, user, password); 
    }

    private void runRemoteApiExtraJaxbClassesTest(String deploymentId, URL deploymentUrl, String user, String password) throws Exception { 
        // Remote API setup
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RemoteRuntimeEngine engine = restSessionFactory.newRuntimeEngine();
        // test
        testExtraJaxbClassSerialization(engine);
    }
    
    public void remoteApiRuleTaskProcess(URL deploymentUrl, String user, String password) {
        // Remote API setup
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RemoteRuntimeEngine runtimeEngine = restSessionFactory.newRuntimeEngine();

        // runTest
        runRuleTaskProcess(runtimeEngine.getKieSession(), runtimeEngine.getAuditLogService());
    }

    public void remoteApiGetTaskInstance(URL deploymentUrl, String user, String password) throws Exception {
        MediaType origType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_XML_TYPE;
        
        // Remote API setup
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RemoteRuntimeEngine engine = restSessionFactory.newRuntimeEngine();

        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = null;
        try {
            processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        } catch (Exception e) {
            fail("Unable to start process: " + e.getMessage());
        }
        
        assertNotNull("Null processInstance!", processInstance);
        long procInstId = processInstance.getId();

        TaskService taskService = engine.getTaskService();
        List<Long> tasks = taskService.getTasksByProcessInstanceId(procInstId);
        assertEquals("Incorrect number of tasks for started process: ", 1, tasks.size());
        long taskId = tasks.get(0);

        // Get it via the command
        JaxbCommandResponse<?> response = executeCommand(deploymentUrl, user, password, deploymentId, new GetTaskCommand(taskId));
        Task task = (Task) response.getResult();
        checkReturnedTask(task, taskId);

        // Get it via the URL
        this.mediaType = origType;
        ClientRequest restRequest = getRestRequestHelper(deploymentUrl, user, password).createRequest("task/" + taskId);
        JaxbTask jaxbTask = get(restRequest, mediaType, JaxbTask.class);
        checkReturnedTask((Task) jaxbTask, taskId);

        // Get it via the remote API
        task = engine.getTaskService().getTaskById(taskId);
        checkReturnedTask(task, taskId);
    }

    private void checkReturnedTask(Task task, long taskId) {
        assertNotNull("Could not retrietve task " + taskId, task);
        assertEquals("Incorrect task retrieved", taskId, task.getId().longValue());
        TaskData taskData = task.getTaskData();
        assertNotNull(taskData);
    }

    public void urlsStartScriptProcess(URL deploymentUrl, String user, String password) throws Exception {
        // Remote API setup
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);

        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/" + SCRIPT_TASK_PROCESS_ID
                + "/start");

        // Start process
        JaxbProcessInstanceResponse jaxbProcInstResp = post(restRequest, mediaType,JaxbProcessInstanceResponse.class);
        ProcessInstance procInst = jaxbProcInstResp.getResult();

        int procStatus = procInst.getState();
        assertEquals("Incorrect process status: " + procStatus, ProcessInstance.STATE_COMPLETED, procStatus);
    }

    
    public void urlsGetTaskAndTaskContent(URL deploymentUrl, String user, String password) throws Exception {
        // Remote API setup
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);

        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/" + TASK_CONTENT_PROCESS_ID + "/start");

        // Start process
        JaxbProcessInstanceResponse jaxbProcInstResp = post(restRequest, mediaType, JaxbProcessInstanceResponse.class);
        ProcessInstance procInst = jaxbProcInstResp.getResult();

        int procStatus = procInst.getState();
        assertEquals("Incorrect process status: " + procStatus, ProcessInstance.STATE_ACTIVE, procStatus);

        // Get taskId
        restRequest = requestHelper.createRequest("task/query?processInstanceId=" + procInst.getId());
        JaxbTaskSummaryListResponse taskSumList = get(restRequest, mediaType, JaxbTaskSummaryListResponse.class);
        assertFalse( "No tasks found!", taskSumList.getResult().isEmpty() );
        TaskSummary taskSum = taskSumList.getResult().get(0);
        long taskId = taskSum.getId();
        
        // get task content
        restRequest = requestHelper.createRequest("task/" + taskId + "/content");
        JaxbContent content = get(restRequest, mediaType, JaxbContent.class);
        assertNotNull( "No content retrieved!", content.getContentMap() );
        assertEquals( "reviewer", content.getContentMap().get("GroupId"));
        
        // get (JSON) task
        MediaType origType = this.mediaType;
        this.mediaType = MediaType.APPLICATION_JSON_TYPE;
        restRequest = getRestRequestHelper(deploymentUrl, user, password).createRequest("task/" + taskId);
        JaxbTask jsonTask = get(restRequest, mediaType, JaxbTask.class);
        
        assertNotNull( "No task retrieved!", jsonTask);
        assertEquals( "task id", taskId, jsonTask.getId().intValue());
        this.mediaType = origType;
    }
    
    public void urlsVariableHistory(URL deploymentUrl, String user, String password) throws Exception {
        // Remote API setup
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
       
        String varId = "myobject";
        String varVal = "10";
        ClientRequest restRequest 
            = requestHelper.createRequest("runtime/" + deploymentId + "/process/" + OBJECT_VARIABLE_PROCESS_ID + "/start?map_" + varId + "=" + varVal);
        JaxbProcessInstanceResponse procInstResp = post(restRequest, mediaType, JaxbProcessInstanceResponse.class);
        long procInstId = procInstResp.getResult().getId();
       
        // var log
        restRequest = requestHelper.createRequest("history/variable/" + varId);
        JaxbHistoryLogList jhll = get(restRequest, mediaType, JaxbHistoryLogList.class);
        List<VariableInstanceLog> viLogs = new ArrayList<VariableInstanceLog>();
        if (jhll != null) {
            List<AuditEvent> history = jhll.getResult();
            for (AuditEvent ae : history) {
                VariableInstanceLog viLog = (VariableInstanceLog) ae;
                if( viLog.getProcessInstanceId() == procInstId ) { 
                    viLogs.add(viLog);
                }
            }
        }

        assertNotNull("Empty VariableInstanceLog list.", viLogs);
        assertEquals("VariableInstanceLog list size",  1, viLogs.size());
        VariableInstanceLog vil = viLogs.get(0);
        assertNotNull("Empty VariableInstanceLog instance.", vil);
        assertEquals("Process instance id", vil.getProcessInstanceId(), procInstId);
        assertEquals("Variable id", vil.getVariableId(), "myobject");
        assertEquals("Variable value", vil.getValue(), varVal);
       
        // proc log
        restRequest = requestHelper.createRequest("history/variable/" + varId + "/instances");
        jhll = get(restRequest, mediaType, JaxbHistoryLogList.class);
        
        assertNotNull("Empty ProcesInstanceLog list", jhll);
        List<ProcessInstanceLog> piLogs = new ArrayList<ProcessInstanceLog>();
        if (jhll != null) {
            List<AuditEvent> history = jhll.getResult();
            for (AuditEvent ae : history) {
                piLogs.add((ProcessInstanceLog) ae);
            }
        }
        assertNotNull("Empty ProcesInstanceLog list", piLogs);
        assertEquals("ProcessInstanceLog list size", piLogs.size(), 1);
        ProcessInstanceLog pi = piLogs.get(0);
        assertNotNull(pi);
        assertEquals(procInstId, pi.getId());
    }

    public void urlsGetDeployments(URL deploymentUrl, String user, String password) throws Exception {
        // test with normal RestRequestHelper
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
      
        ClientRequest restRequest = requestHelper.createRequest("deployment/");
        JaxbDeploymentUnitList depList = get(restRequest, mediaType, JaxbDeploymentUnitList.class);
        assertNotNull( "Null answer!", depList);
        assertNotNull( "Null deployment list!", depList.getDeploymentUnitList() );
        assertTrue( "Empty deployment list!", depList.getDeploymentUnitList().size() > 0);
       
        String deploymentId = depList.getDeploymentUnitList().get(0).getIdentifier();
        restRequest = requestHelper.createRequest("deployment/" + deploymentId);
        JaxbDeploymentUnit dep = get(restRequest, mediaType, JaxbDeploymentUnit.class);
        assertNotNull( "Null answer!", dep);
        assertNotNull( "Null deployment list!", dep);
        assertEquals( "Empty status!", JaxbDeploymentStatus.DEPLOYED, dep.getStatus());

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
        if (200 != respCode) { 
            logger.warn(connection.getContent().toString());
        }
        assertEquals(200, respCode);

        JaxbSerializationProvider jaxbSerializer = new JaxbSerializationProvider();
        String xmlStrObj = getConnectionContent(connection.getContent());
        logger.info( "Output: |" + xmlStrObj + "|");
        depList = (JaxbDeploymentUnitList) jaxbSerializer.deserialize(xmlStrObj);

        assertNotNull( "Null answer!", depList);
        assertNotNull( "Null deployment list!", depList.getDeploymentUnitList() );
        assertTrue( "Empty deployment list!", depList.getDeploymentUnitList().size() > 0);
    }
   
    private String getConnectionContent(Object content) throws Exception { 
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
    
    public void urlsGetRealProcessVariable(URL deploymentUrl, String user, String password) throws Exception { 
        // Setup
        RemoteRuntimeEngineFactory restSessionFactory = getRemoteRuntimeFactory(deploymentUrl, user, password);
        RemoteRuntimeEngine engine = restSessionFactory.newRuntimeEngine();
        logger.info( "deployment url: " + deploymentUrl.toExternalForm());
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        
        // Start process
        MyType param = new MyType("variable", 29);
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("myobject", param);
        long procInstId = engine.getKieSession().startProcess(OBJECT_VARIABLE_PROCESS_ID, parameters).getId();
        
        /**
         * Check that MyType was correctly deserialized on server side
         */
        String varName = "myobject";
        List<VariableInstanceLog> varLogList = engine.getAuditLogService().findVariableInstancesByName(varName, false);
        VariableInstanceLog thisProcInstVarLog = null;
        for( VariableInstanceLog varLog : varLogList ) {
            if( varLog.getProcessInstanceId() == procInstId ) { 
                thisProcInstVarLog = varLog;
                break;
            }
        }
        assertNotNull( "No VariableInstanceLog found!", thisProcInstVarLog);
        assertEquals( varName, thisProcInstVarLog.getVariableId() );
        Object procInstVar = thisProcInstVarLog.getValue();
        assertNotNull("Null process instance variable!", procInstVar);
        assertEquals( "De/serialization of Kjar type did not work.", param.toString(), procInstVar );
        
        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/instance/" + procInstId );
        JaxbProcessInstanceResponse jaxbProcInstResp = get(restRequest, mediaType,JaxbProcessInstanceResponse.class);
        ProcessInstance procInst = jaxbProcInstResp.getResult();
        assertNotNull( procInst );
        assertEquals( "Unequal process instance id.", procInstId, procInst.getId());
       
        restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/instance/" + procInstId + "/variable/" + varName );
        String xmlOrJsonStr = get(restRequest, mediaType, String.class);
        JAXBElement<MyType> retrievedVarElem;
        try { 
            JAXBElement elem = (new ObjectMapper()).readValue(xmlOrJsonStr, JAXBElement.class);
            MyType og = (MyType) elem.getValue();
            System.out.println( "YES! : " + og.toString());
        } catch( Exception e ) { 
            System.out.println( "NO." );
            e.printStackTrace();
        }
        try { 
            MyType og = (new ObjectMapper()).readValue(xmlOrJsonStr, MyType.class);
            System.out.println( "JA! : " + og.toString());
        } catch( Exception e ) { 
            System.out.println( "NEE..." );
            e.printStackTrace();
        }
        
        try { 
            if( mediaType.equals(MediaType.APPLICATION_XML_TYPE) ) { 
                retrievedVarElem = (JAXBElement<MyType>) jaxbSerializationProvider.deserialize(xmlOrJsonStr);
            } else if( mediaType.equals(MediaType.APPLICATION_JSON_TYPE) ) { 
                jsonSerializationProvider.setDeserializeOutputClass(JAXBElement.class);
                retrievedVarElem = (JAXBElement<MyType>) jsonSerializationProvider.deserialize(xmlOrJsonStr);
            } else { 
                throw new IllegalStateException("Unknown media type: " + mediaType.getType() + "/" + mediaType.getSubtype() );
            }
        } catch( SerializationException se ) { 
           logger.error( "Could not deserialize string:\n{}", xmlOrJsonStr);
           throw se;
        }
       
        MyType retrievedVar = retrievedVarElem.getValue();
        assertNotNull( "Expected filled variable.", retrievedVar);
        assertEquals("Data integer doesn't match: ", retrievedVar.getData(), param.getData());
        assertEquals("Text string doesn't match: ", retrievedVar.getText(), param.getText());
    }
    
    public void remoteApiHumanTaskGroupIdTest(URL deploymentUrl) { 
        RemoteRestRuntimeEngineBuilder runtimeEngineBuilder
            = RemoteRestRuntimeEngineFactory.newBuilder()
            .addDeploymentId(deploymentId)
            .addUrl(deploymentUrl);
       
        RemoteRuntimeEngine krisRemoteEngine = runtimeEngineBuilder
                .addUserName(KRIS_USER)
                .addPassword(KRIS_PASSWORD)
                .build();
        RemoteRuntimeEngine maryRemoteEngine = runtimeEngineBuilder
                .addUserName(MARY_USER)
                .addPassword(MARY_PASSWORD)
                .build();
        RemoteRuntimeEngine johnRemoteEngine = runtimeEngineBuilder
                .addUserName(JOHN_USER)
                .addPassword(JOHN_PASSWORD)
                .build();
            
        runHumanTaskGroupIdTest(krisRemoteEngine, johnRemoteEngine, maryRemoteEngine);
    }
    
    public void urlsGroupAssignmentTest(URL deploymentUrl) throws Exception { 
        RestRequestHelper maryReqHelper = RestRequestHelper.newInstance(deploymentUrl, MARY_USER, MARY_PASSWORD);
        RestRequestHelper johnReqHelper = RestRequestHelper.newInstance(deploymentUrl, JOHN_USER, JOHN_PASSWORD);
       
        ClientRequest restRequest = maryReqHelper.createRequest("runtime/" + deploymentId + "/process/" + GROUP_ASSSIGNMENT_PROCESS_ID + "/start");
        JaxbProcessInstanceResponse procInstResp = post(restRequest, mediaType,JaxbProcessInstanceResponse.class);
        assertEquals(ProcessInstance.STATE_ACTIVE, procInstResp.getState());
        long procInstId = procInstResp.getId();

        // assert the task
        TaskSummary taskSummary = getTaskSummary(maryReqHelper, procInstId, Status.Ready);
        long taskId = taskSummary.getId();
        assertNull(taskSummary.getActualOwner());
        assertNull(taskSummary.getPotentialOwners());
        assertEquals("Task 1", taskSummary.getName());

        // complete 'Task 1' as mary
        restRequest = maryReqHelper.createRequest("task/"+ taskId + "/claim");
        ClientResponse<?> responseObj = post(restRequest, mediaType);
        responseObj.releaseConnection();
        
        restRequest = maryReqHelper.createRequest("task/"+ taskId + "/start");
        responseObj = post(restRequest, mediaType);
        responseObj.releaseConnection();
        restRequest = maryReqHelper.createRequest("task/"+ taskId + "/complete");
        responseObj = post(restRequest, mediaType);
        responseObj.releaseConnection();

        // now make sure that the next task has been assigned to the
        // correct person. it should be mary.
        taskSummary = getTaskSummary(maryReqHelper, procInstId, Status.Reserved);
        assertEquals("Task 2", taskSummary.getName());
        assertEquals(MARY_USER, taskSummary.getActualOwner().getId());
        taskId = taskSummary.getId();

        // complete 'Task 2' as john
        restRequest = maryReqHelper.createRequest("task/"+ taskId + "/release");
        responseObj = post(restRequest, mediaType);
        responseObj.releaseConnection();
        restRequest = johnReqHelper.createRequest("task/"+ taskId + "/start");
        responseObj = post(restRequest, mediaType);
        responseObj.releaseConnection();
        restRequest = johnReqHelper.createRequest("task/"+ taskId + "/complete");
        responseObj = post(restRequest, mediaType);
        responseObj.releaseConnection();

        // now make sure that the next task has been assigned to the
        // correct person. it should be john.
        taskSummary = getTaskSummary(johnReqHelper, procInstId, Status.Reserved);
        assertEquals("Task 3", taskSummary.getName());
        assertEquals(JOHN_USER, taskSummary.getActualOwner().getId());
        taskId = taskSummary.getId();
        
        // complete 'Task 3' as john
        restRequest = johnReqHelper.createRequest("task/"+ taskId + "/start");
        responseObj = post(restRequest, mediaType);
        responseObj.releaseConnection();
        restRequest = johnReqHelper.createRequest("task/"+ taskId + "/complete");
        responseObj = post(restRequest, mediaType);
        responseObj.releaseConnection();

        // assert process finished
        restRequest = maryReqHelper.createRequest("history/instance/" + procInstId);
        JaxbProcessInstanceLog jaxbProcInstLog = get(restRequest, mediaType, JaxbProcessInstanceLog.class);
        ProcessInstanceLog procInstLog = jaxbProcInstLog.getResult();
        assertEquals( "Process instance has not completed!", ProcessInstance.STATE_COMPLETED, procInstLog.getStatus().intValue());
    }
   
    private TaskSummary getTaskSummary(RestRequestHelper requestHelper, long processInstanceId, Status status) throws Exception {
        ClientRequest restRequest = requestHelper.createRequest("task/query?processInstanceId=" + processInstanceId+ "&status=" + status.toString() );
        JaxbTaskSummaryListResponse taskSumListResp = get(restRequest, mediaType,JaxbTaskSummaryListResponse.class);
        List<TaskSummary> taskSumList = taskSumListResp.getResult();
        assertEquals(1, taskSumList.size());
        return taskSumList.get(0);
    }
    
    public void urlsWorkItemTest(URL deploymentUrl, String user, String password) throws Exception {
        RestRequestHelper helper = getRestRequestHelper(deploymentUrl, user, password);

        // Start process
        ClientRequest restRequest = helper.createRequest("runtime/" + deploymentId + "/workitem/200" );
        ClientResponse<?> responseObj = get(restRequest, mediaType);
    }
  
    public void remoteApiHumanTaskGroupVarAssignTest(URL deploymentUrl) { 
        RemoteRuntimeEngineFactory maryRemoteEngineFactory 
            = RemoteRestRuntimeEngineFactory.newBuilder()
            .addDeploymentId(deploymentId)
            .addUserName(MARY_USER)
            .addPassword(MARY_PASSWORD)
            .addUrl(deploymentUrl)
            .buildFactory();

        RuntimeEngine runtimeEngine = maryRemoteEngineFactory.newRuntimeEngine();

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("taskOwnerGroup", "HR");
        params.put("taskName", "Mary's Task");
        ProcessInstance pi = runtimeEngine.getKieSession().startProcess(GROUP_ASSSIGN_VAR_PROCESS_ID, params);
        assertNotNull( "No ProcessInstance!", pi);
        long procInstId = pi.getId();
        
        List<Long> taskIds = runtimeEngine.getTaskService().getTasksByProcessInstanceId(procInstId);
        assertEquals( 1, taskIds.size());

        List<String> processIds = runtimeEngine.getKieSession().execute(new GetProcessIdsCommand());
        assertTrue( "No process ids returned.", ! processIds.isEmpty() && processIds.size() > 5 );
     }
    
    public void remoteApiHumanTaskOwnTypeTest(URL deploymentUrl) { 
        RemoteRuntimeEngineFactory maryRemoteEngineFactory 
            = RemoteRestRuntimeEngineFactory.newBuilder()
            .addDeploymentId(deploymentId)
            .addUserName(JOHN_USER)
            .addPassword(JOHN_PASSWORD)
            .addUrl(deploymentUrl)
            .addExtraJaxbClasses(MyType.class)
            .buildFactory();

        RemoteRuntimeEngine runtimeEngine = maryRemoteEngineFactory.newRuntimeEngine();
        runRemoteApiHumanTaskOwnTypeTest(runtimeEngine, runtimeEngine.getAuditLogService());
     }
    
    public void runRemoteApiHumanTaskOwnTypeTest(RuntimeEngine runtimeEngine, AuditLogService auditLogService) { 
        MyType myType = new MyType("wacky", 123);

        ProcessInstance pi = runtimeEngine.getKieSession().startProcess(HUMAN_TASK_OWN_TYPE_ID);
        assertNotNull(pi);
        assertEquals( ProcessInstance.STATE_ACTIVE, pi.getState());

        TaskService taskService = runtimeEngine.getTaskService();
        List<Long> taskIds = taskService.getTasksByProcessInstanceId(pi.getId());
        assertFalse(taskIds.isEmpty());
        long taskId = taskIds.get(0);

        taskService.start(taskId, JOHN_USER);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("outMyObject", myType);
        taskService.complete(taskId, JOHN_USER, data);

        Task task = taskService.getTaskById(taskId);
        assertEquals( Status.Completed, task.getTaskData().getStatus());

        List<VariableInstanceLog> vill = auditLogService.findVariableInstances(pi.getId(), "myObject");
        assertNotNull(vill);
        assertEquals(myType.toString(), vill.get(0).getValue());
    }
    
    public void urlsCreateMemoryLeakOnTomcat(URL deploymentUrl, String user, String password, long timeout) throws Exception { 
        long origCallDurationLimit = this.restCallDurationLimit;
        this.restCallDurationLimit = timeout; 
        
        // Remote API setup
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        try { 
            for( int i = 0; i < 20; ++i ) { 
                logger.info( i + " process started.");
                startProcessWithUserDefinedClass(requestHelper); 
            }
        } finally { 
            this.restCallDurationLimit = origCallDurationLimit;
        }
     }
    
    private void startProcessWithUserDefinedClass(RestRequestHelper requestHelper) throws Exception {
        String varId = "myobject";
        ClientRequest restRequest = requestHelper.createRequest("runtime/" + deploymentId + "/process/" + OBJECT_VARIABLE_PROCESS_ID + "/start?map_" + varId + "=10");
        ClientResponse<?> responseObj = checkResponsePostTime(restRequest, mediaType, 200);
        JaxbProcessInstanceResponse procInstResp = getResponseEntity(responseObj, JaxbProcessInstanceResponse.class);
        long procInstId = procInstResp.getResult().getId();;
        assertTrue( "Process instance should be larger than 0: " + procInstId, procInstId > 0 );
    }
    
    public void urlsGetProcessDefinitionInfo(URL deploymentUrl, String user, String password) throws Exception { 
        RestRequestHelper helper = getRestRequestHelper(deploymentUrl, user, password);

        // Start process
        ClientRequest restRequest = helper.createRequest("deployment/processes/" );
        JaxbProcessDefinitionList jaxbProcDefList = get(restRequest, mediaType, JaxbProcessDefinitionList.class);
       
        List<JaxbProcessDefinition> procDefList = jaxbProcDefList.getProcessDefinitionList();
        for( JaxbProcessDefinition jaxbProcDef : procDefList ) { 
            validateProcessDefinition(jaxbProcDef);
        }    
        
    }
    
    private void validateProcessDefinition( JaxbProcessDefinition procDef ) { 
       String id = procDef.getId();
       assertFalse("Process def " + id + ": null deployment id", procDef.getDeploymentId() == null || procDef.getDeploymentId().isEmpty() ); 
       assertFalse("Process def " + id + ": null name", procDef.getName() == null || procDef.getName().isEmpty() );
       assertFalse("Process def " + id + ": null pkg name", procDef.getPackageName() == null || procDef.getPackageName().isEmpty() );
       assertFalse("Process def " + id + ": null variables", procDef.getVariables() == null || procDef.getVariables().isEmpty() );
       assertFalse("Process def " + id + ": null version", procDef.getVersion() == null || procDef.getVersion().isEmpty() );
    }
    
    public void remoteApiDeploymentRedeployClassPathTest(URL deploymentUrl, String user, String password) throws Exception  {
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        
        KModuleDeploymentUnit kDepUnit = new KModuleDeploymentUnit(GROUP_ID, CLASSPATH_ARTIFACT_ID, VERSION);
        String classpathDeploymentId = kDepUnit.getIdentifier();
        
        // Check that project is not deployed
        ClientRequest restRequest = requestHelper.createRequest("deployment/" + classpathDeploymentId + "/");
        setAcceptHeader(restRequest, mediaType);
  
        // Run test the first time
        if ( ! isDeployed(kDepUnit, restRequest.get()) ) { 
            // Deploy
            deploy(kDepUnit, user, password, deploymentUrl);
        }
            
        // Run process
        RemoteRuntimeEngine runtimeEngine = RemoteRestRuntimeEngineFactory.newBuilder()
                .addDeploymentId(classpathDeploymentId)
                .addUrl(deploymentUrl)
                .addUserName(user)
                .addPassword(password)
                .build();
                
        runClassPathProcessTest(runtimeEngine);
           
        // undeploy..
        undeploy(kDepUnit, deploymentUrl, requestHelper);
            
        // .. and (re)deploy
        deploy(kDepUnit, user, password, deploymentUrl);

        logger.info( "Rerunning test.. is there a CNFE?");
        // Rerun process
        runClassPathProcessTest(runtimeEngine);
    }
 
    private void runClassPathProcessTest(RemoteRuntimeEngine runtimeEngine) { 
        KieSession ksession = runtimeEngine.getKieSession();

        Map<String, Object> params = new HashMap<String, Object>();
        String varId = "myobject";
        String text = UUID.randomUUID().toString();
        params.put(varId, new MyType(text, 10));
        ProcessInstance procInst = ksession.startProcess(TestConstants.CLASSPATH_OBJECT_PROCESS_ID, params);
        long processInstanceId = procInst.getId();

        AuditLogService auditLogService = runtimeEngine.getAuditLogService();
        List<VariableInstanceLog> varLogList = auditLogService.findVariableInstances(processInstanceId);
        
        assertNotNull("Null variable instance found.", varLogList);
        for (VariableInstanceLog varLog : varLogList ) { 
            logger.debug(varLog.getVariableId() + " (" + varLog.getValue() + ") " );
        }

        List<VariableInstanceLog> varLogs = runtimeEngine.getAuditLogService().findVariableInstancesByName(varId, false);
        assertTrue(varLogs.size() > 0);
        assertEquals(varId, varLogs.get(0).getVariableId());

        procInst = ksession.getProcessInstance(processInstanceId);
        assertNull(procInst);
    }
    
    public void urlsDeploymentProcessDefinitions(URL deploymentUrl, String user, String password) throws Exception  {
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl, user, password);
        
        ClientRequest restRequest = requestHelper.createRequest("/deployment/processes");
        JaxbProcessDefinitionList jaxbProcDefList = get(restRequest, mediaType, JaxbProcessDefinitionList.class);
        
        assertTrue( "Null response!", jaxbProcDefList != null);
        List<JaxbProcessDefinition> procDefList = jaxbProcDefList.getProcessDefinitionList();
        assertTrue( "Empty response list!", 
                jaxbProcDefList != null && ! procDefList.isEmpty() );
        
        JaxbProcessDefinition jaxbProcDef = procDefList.get(0);
        assertNotNull( "Null deployment id", jaxbProcDef.getDeploymentId() );
        assertNotNull( "Null id", jaxbProcDef.getId() );
        assertNotNull( "Null name", jaxbProcDef.getName() );
        assertNotNull( "Null package name", jaxbProcDef.getPackageName() );
       
        for( JaxbProcessDefinition procDef : procDefList ) { 
            if( procDef.getVariables() != null ) { 
                logger.info("{}/{} : {}", procDef.getDeploymentId(), procDef.getName(), procDef.getVariables().size() );
            }
        }
    }
}
