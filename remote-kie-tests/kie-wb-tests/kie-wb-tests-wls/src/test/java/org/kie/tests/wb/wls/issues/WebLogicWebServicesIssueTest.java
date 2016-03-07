/*
 * t * JBoss, Home of Professional Open Source
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
package org.kie.tests.wb.wls.issues;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.kie.tests.wb.base.util.TestConstants.HUMAN_TASK_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.wls.KieWbWarWeblogicDeploy.createTestWar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.command.Command;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Task;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.remote.client.api.exception.RemoteCommunicationException;
import org.kie.remote.client.jaxb.JaxbCommandsRequest;
import org.kie.remote.client.jaxb.JaxbCommandsResponse;
import org.kie.remote.jaxb.gen.GetContentByIdCommand;
import org.kie.remote.jaxb.gen.GetTaskCommand;
import org.kie.remote.jaxb.gen.GetTaskContentCommand;
import org.kie.remote.jaxb.gen.GetTasksByProcessInstanceIdCommand;
import org.kie.remote.jaxb.gen.JaxbStringObjectPairArray;
import org.kie.remote.jaxb.gen.StartProcessCommand;
import org.kie.remote.jaxb.gen.util.JaxbStringObjectPair;
import org.kie.remote.services.ws.command.generated.CommandWebService;
import org.kie.remote.tests.base.unit.MavenBuildIgnoreRule;
import org.kie.remote.tests.base.unit.MavenBuildIgnoreRule.IgnoreWhenInMavenBuild;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbLongListResponse;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessInstanceResponse;
import org.kie.services.client.serialization.jaxb.rest.JaxbExceptionResponse;
import org.kie.tests.MyType;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.kie.tests.wb.base.methods.KieWbWebServicesIntegrationTestMethods;
import org.kie.tests.wb.base.methods.RepositoryDeploymentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// @RunAsClient
// @RunWith(Arquillian.class)
public class WebLogicWebServicesIssueTest {

    @Deployment(testable = false, name = "kie-wb-eap")
    public static Archive<?> createWar() {
        return createTestWar();
    }

    private static final Logger logger = LoggerFactory.getLogger(WebLogicWebServicesIssueTest.class);

    @ArquillianResource
    URL deploymentUrl;
    {
        try {
            deploymentUrl = new URL("http://localhost:8080/business-central/");
        } catch( MalformedURLException murle ) {
            throw new IllegalStateException("Unable to create URL: " + murle.getMessage(), murle);
        }
    }

    @Rule
    public MavenBuildIgnoreRule rule = new MavenBuildIgnoreRule();

    @AfterClass
    public static void waitForTxOnServer() throws InterruptedException {
        long sleep = 1000;
        logger.info("Waiting " + sleep / 1000 + " secs for tx's on server to close.");
        Thread.sleep(sleep);
    }

    protected void printTestName() {
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println("-=> " + testName);
    }

    @Test
    @IgnoreWhenInMavenBuild
    public void clientSecurityTest() throws Exception {
        printTestName();
        String user = MARY_USER;
        String password = MARY_PASSWORD;

        // deploy
        System.setProperty("http.maxDirects", "2");

        boolean deploy = false;
        if( deploy ) {
            RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, user, password, 5);
            deployUtil.setStrategy(RuntimeStrategy.SINGLETON);

            String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
            String repositoryName = "tests";
            String project = "integration-tests";
            String deploymentId = KJAR_DEPLOYMENT_ID;
            String orgUnit = UUID.randomUUID().toString();
            orgUnit = orgUnit.substring(0, orgUnit.indexOf("-"));
            deployUtil.createRepositoryAndDeployProject(repoUrl, repositoryName, project, deploymentId, orgUnit);

            int sleep = 2;
            logger.info("Waiting {} more seconds to make sure deploy is done..", sleep);
            Thread.sleep(sleep * 1000);
        }

        startSimpleProcess(deploymentUrl);
    }

    public static void startSimpleProcess(URL deploymentUrl) throws Exception {
        boolean exceptionThrown = false;
        try {
            createDefaultClient(deploymentUrl, "Nemo", "Underwater");
        } catch( RemoteCommunicationException rce ) {
            exceptionThrown = true;
           // unauthorized user
        }
        assertTrue( "No exception thrown when unauthorized client was created!", exceptionThrown);

        CommandWebService commandWebService = createDefaultClient(deploymentUrl, MARY_USER, MARY_PASSWORD);
        startSimpleProcess(commandWebService, deploymentUrl, MARY_USER);
    }

    public static void startSimpleProcess(CommandWebService commandWebService, URL deploymentUrl, String user) throws Exception {
        // start process
        StartProcessCommand spc = new StartProcessCommand();
        spc.setProcessId(HUMAN_TASK_PROCESS_ID);
        JaxbStringObjectPairArray map = new JaxbStringObjectPairArray();
        JaxbStringObjectPair keyValue = new JaxbStringObjectPair();
        keyValue.setKey("myobject");
        keyValue.setValue(new MyType("variable", 29));
        map.getItems().add(keyValue);
        spc.setParameter(map);

        // keyValue = new JaxbStringObjectPair();
        // keyValue.setKey("mylist");
        // Float [] floatArrObj = new Float[] { 10.3f, 5.6f };
        // keyValue.setValue(floatArrObj);
        // map.getItems().add(keyValue);
        // spc.setParameter(map);

        // webService
        JaxbProcessInstanceResponse jpir = doWebserviceRequest(commandWebService, spc, "start process", JaxbProcessInstanceResponse.class);
        long procInstId = ((JaxbProcessInstanceResponse) jpir).getId();

        doBadAuthTest(deploymentUrl, spc);

        // get task id
        GetTasksByProcessInstanceIdCommand gtbic = new GetTasksByProcessInstanceIdCommand();
        gtbic.setProcessInstanceId(procInstId);
        gtbic.setUserId(user);

        // webservice
        JaxbLongListResponse jllr = doWebserviceRequest(commandWebService, gtbic, "get tasks by", JaxbLongListResponse.class );
        List<Long> taskIds = jllr.getResult();

        assertFalse( "Empty task id list", taskIds.isEmpty());
        long taskId = taskIds.get(0);

        // get task and task content
        GetTaskCommand gtc = new GetTaskCommand();
        gtc.setTaskId(taskId);
        gtc.setUserId(user);
        GetTaskContentCommand gtcc = new GetTaskContentCommand();
        gtcc.setTaskId(taskId);
        gtcc.setUserId(user);

        // webservice
        JaxbCommandsRequest req = new JaxbCommandsRequest(KJAR_DEPLOYMENT_ID, gtc);
        req.getCommands().add(gtcc);
        JaxbCommandsResponse response = commandWebService.execute(req);

        // task and content response
        Task task = (Task) response.getResponses().get(0).getResult();
        Map<String, Object> contentMap = (Map<String, Object>) response.getResponses().get(1).getResult();

        // get content
        GetContentByIdCommand gcc = new GetContentByIdCommand();
        gcc.setContentId(task.getTaskData().getDocumentContentId());
        gcc.setUserId(user);

        // webservice
        req = new JaxbCommandsRequest(KJAR_DEPLOYMENT_ID, gcc);
        response = commandWebService.execute(req);

        // content response
        Content content = (Content) response.getResponses().get(0).getResult();
        Object contentMapObj = ContentMarshallerHelper.unmarshall(content.getContent(), null);
        if( contentMapObj != null ) {
            contentMap = (Map<String, Object>) contentMapObj;
            for( Entry<String, Object> entry : contentMap.entrySet() ) {
                logger.info(entry.getKey() + " -> "  +  entry.getValue());
            }
        }

    }

    private static void doBadAuthTest(URL deploymentUrl, Command cmd) {
        boolean exceptionThrown = false;
        boolean unauthClientCreated = false; // this will change when we move to token based
        boolean unauthClientCallSucceeded = false; // this will change when we move to token based
        try {
            CommandWebService unauthCommandWebService = createDefaultClient(deploymentUrl, "Nemo", "Underwater");
            unauthClientCreated = true;
            doWebserviceRequest(unauthCommandWebService, cmd, "start process", JaxbProcessInstanceResponse.class);
            unauthClientCallSucceeded = true;
        } catch( Exception e ) {
            exceptionThrown = true;
            // test exception type
        }
        assertTrue( "Unauthorized webservice actions should not have succeeded! [" + unauthClientCreated + "/" + unauthClientCallSucceeded + "]",
                ! unauthClientCreated && ! unauthClientCallSucceeded );
    }
    private static <T> T doWebserviceRequest(CommandWebService service, Command<?> cmd, String oper, Class<T> respClass) throws Exception {
        // Get a response from the WebService
        JaxbCommandsRequest req = new JaxbCommandsRequest(KJAR_DEPLOYMENT_ID, cmd);
        JaxbCommandsResponse response = service.execute(req);
        assertNotNull( oper + ": null response", response );
        assertFalse( oper + ": empty response", response.getResponses().isEmpty() );

        // check response
        JaxbCommandResponse<?> cmdResp = response.getResponses().get(0);
        assertNotNull( oper + ": null command response", cmdResp );
        if( ! respClass.isAssignableFrom(cmdResp.getClass()) ) {
           if( cmdResp instanceof JaxbExceptionResponse ) {
               System.out.println( ((JaxbExceptionResponse) cmdResp).getMessage() );
               System.out.println( ((JaxbExceptionResponse) cmdResp).getStackTrace() );
           }
        }
        assertTrue( oper + ": incorrect cmd response type: " + cmdResp.getClass(), respClass.isAssignableFrom(cmdResp.getClass()) );

        return (T) cmdResp;
    }

    private static CommandWebService createDefaultClient(URL deploymentUrl, String user, String pwd) throws Exception {
        CommandWebService client =
        RemoteRuntimeEngineFactory.newCommandWebServiceClientBuilder()
            .addServerUrl(deploymentUrl)
            .addUserName(user)
            .addPassword(pwd)
            .addDeploymentId(KJAR_DEPLOYMENT_ID)
            .addExtraJaxbClasses(MyType.class)
            .buildBasicAuthClient();

        return client;
    }

}
