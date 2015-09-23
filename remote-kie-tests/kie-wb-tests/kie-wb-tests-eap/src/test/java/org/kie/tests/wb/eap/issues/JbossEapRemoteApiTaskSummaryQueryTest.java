/*
t  * JBoss, Home of Professional Open Source
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
package org.kie.tests.wb.eap.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.findTaskSummaryByProcessInstanceId;
import static org.kie.tests.wb.base.util.TestConstants.HUMAN_TASK_VAR_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.eap.KieWbWarJbossEapDeploy.createTestWar;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.remote.client.jaxb.JaxbTaskSummaryListResponse;
import org.kie.remote.tests.base.RestUtil;
import org.kie.remote.tests.base.unit.MavenBuildIgnoreRule;
import org.kie.remote.tests.base.unit.MavenBuildIgnoreRule.IgnoreWhenInMavenBuild;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessInstanceResponse;
import org.kie.tests.wb.base.methods.RepositoryDeploymentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunAsClient
@RunWith(Arquillian.class)
public class JbossEapRemoteApiTaskSummaryQueryTest {

    @Deployment(testable = false, name = "kie-wb-eap")
    public static Archive<?> createWar() {
        return createTestWar();
    }

    private static final Logger logger = LoggerFactory.getLogger(JbossEapRemoteApiTaskSummaryQueryTest.class);

    @ArquillianResource
    URL deploymentUrl;

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
        System.out.println( "-=> " + testName );
    }

    @Test
    @IgnoreWhenInMavenBuild
    public void issueTest() throws Exception {
        printTestName();
        String USER_ID = MARY_USER;
        String PASSWORD = MARY_PASSWORD;

        // deploy

        RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, USER_ID, PASSWORD, 5);
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

        // Start process
        String startProcessUrl = "rest/runtime/" + deploymentId + "/process/" + HUMAN_TASK_VAR_PROCESS_ID + "/start";

        Map<String, String> formParams = new HashMap<String, String>(1);
        formParams.put("map_userName", "John");
        JaxbProcessInstanceResponse processInstance = RestUtil.postForm(deploymentUrl,
                startProcessUrl, MediaType.APPLICATION_XML,
                 200, USER_ID, PASSWORD,
                 formParams,
                 JaxbProcessInstanceResponse.class);
        Long procInstId = processInstance.getId();

        // query tasks for associated task Id
        Map<String, String> queryparams = new HashMap<String, String>();
        queryparams.put("processInstanceId", String.valueOf(procInstId));
        JaxbTaskSummaryListResponse taskSumlistResponse = RestUtil.getQuery( deploymentUrl,
                "rest/task/query", MediaType.APPLICATION_XML,
                200, USER_ID, PASSWORD,
                queryparams, JaxbTaskSummaryListResponse.class);

        List<TaskSummary> taskSumList = taskSumlistResponse.getResult();
        checkForDuplicates(taskSumList);

        TaskSummary taskSum = findTaskSummaryByProcessInstanceId(procInstId, taskSumList );
        long taskId = taskSum.getId();
        Status taskStatus = taskSum.getStatus();

        // get task info
        org.kie.remote.jaxb.gen.Task task = RestUtil.get(deploymentUrl,
                "rest/task/" + taskId, MediaType.APPLICATION_XML,
                200, USER_ID, PASSWORD,
                org.kie.remote.jaxb.gen.Task.class);
        assertEquals("Incorrect task id", taskId, task.getId().longValue());

        // query tasks for associated task Id
        queryparams = new HashMap<String, String>();
        queryparams.put("piid", procInstId.toString());
        queryparams.put("status", taskStatus.toString());
        taskSumlistResponse = RestUtil.getQuery( deploymentUrl,
                "rest/task/query", MediaType.APPLICATION_XML,
                200, USER_ID, PASSWORD,
                queryparams, JaxbTaskSummaryListResponse.class);

        assertEquals("Incorrect num tasks", 1, taskSumlistResponse.getResult().size() );
        taskSum = taskSumlistResponse.getResult().get(0);
        assertEquals("Incorrect task id", taskId, task.getId().longValue());
    }

    private void checkForDuplicates(List<TaskSummary> taskSumList) {
        Set<Long> taskIds = new HashSet<Long>();
        for( TaskSummary taskSum : taskSumList )  {
            assertTrue( "TaskSummary " + taskSum.getId() + " already present in list!",
                        taskIds.add(taskSum.getId()) );
        }
    }
}
