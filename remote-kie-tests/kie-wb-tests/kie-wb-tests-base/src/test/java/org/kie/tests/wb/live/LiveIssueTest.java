package org.kie.tests.wb.live;

import static org.junit.Assert.assertEquals;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.findTaskSummaryByProcessInstanceId;
import static org.kie.tests.wb.base.util.TestConstants.HUMAN_TASK_VAR_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.junit.Rule;
import org.junit.Test;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.remote.client.jaxb.JaxbTaskSummaryListResponse;
import org.kie.remote.tests.base.RestUtil;
import org.kie.remote.tests.base.unit.GetIgnoreRule;
import org.kie.remote.tests.base.unit.GetIgnoreRule.IgnoreIfGETFails;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessInstanceResponse;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.kie.tests.wb.base.methods.RepositoryDeploymentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveIssueTest {

    protected static final Logger logger = LoggerFactory.getLogger(LiveIssueTest.class);

    private static String user = "mary";
    private static String password = "mary123@";

    @Rule
    public GetIgnoreRule getIgnoreRule = new GetIgnoreRule();

    private static URL deploymentUrl;
    static {
        try {
            deploymentUrl =  new URL("http://localhost:8080/business-central/");
        } catch( MalformedURLException e ) {
            // do nothing
        }
    }

    @Test
    @IgnoreIfGETFails(getUrl="http://localhost:8080/kie-wb/business-central/deployment")
    public void issueTest() throws Exception {

        // deploy

        RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, user, password, 5);

        String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
        String repositoryName = "tests";
        String project = "integration-tests";
        String deploymentId = "org.test:kjar:1.0";
        String orgUnit = UUID.randomUUID().toString();
        orgUnit = orgUnit.substring(0, orgUnit.indexOf("-"));
        deployUtil.createRepositoryAndDeployProject(repoUrl, repositoryName, project, deploymentId, orgUnit);

        int sleep = 2;
        logger.info("Waiting {} more seconds to make sure deploy is done..", sleep);
        Thread.sleep(sleep * 1000);

        KieWbRestIntegrationTestMethods restTests = KieWbRestIntegrationTestMethods.newBuilderInstance()
                .setDeploymentId(KJAR_DEPLOYMENT_ID)
                .setMediaType(MediaType.APPLICATION_XML)
                .setStrategy(RuntimeStrategy.SINGLETON)
                .setTimeoutInSecs(5)
                .build();

        // Start process
        String startProcessUrl = "rest/runtime/" + deploymentId + "/process/" + HUMAN_TASK_VAR_PROCESS_ID + "/start";

        Map<String, String> formParams = new HashMap<String, String>(1);
        formParams.put("map_userName", "John");
        JaxbProcessInstanceResponse processInstance = RestUtil.postForm(deploymentUrl,
                startProcessUrl, MediaType.APPLICATION_XML,
                 200, user, password,
                 formParams,
                 JaxbProcessInstanceResponse.class);
        long procInstId = processInstance.getId();

        // query tasks for associated task Id
        Map<String, String> queryparams = new HashMap<String, String>();
        queryparams.put("processInstanceId", String.valueOf(procInstId));
        JaxbTaskSummaryListResponse taskSumlistResponse = RestUtil.getQuery( deploymentUrl,
                "task/query", MediaType.APPLICATION_XML,
                200, user, password,
                queryparams, JaxbTaskSummaryListResponse.class);

        TaskSummary taskSum = findTaskSummaryByProcessInstanceId(procInstId, taskSumlistResponse.getResult());
        long taskId = taskSum.getId();

        // get task info
        org.kie.remote.jaxb.gen.Task task = RestUtil.get(deploymentUrl,
                "task/" + taskId, MediaType.APPLICATION_XML,
                200, user, password,
                org.kie.remote.jaxb.gen.Task.class);
        assertEquals("Incorrect task id", taskId, task.getId().longValue());

        // query tasks for associated task Id
        queryparams = new HashMap<String, String>();
        queryparams.put("taskid", String.valueOf(taskId));
        taskSumlistResponse = RestUtil.getQuery( deploymentUrl,
                "task/query", MediaType.APPLICATION_XML,
                200, user, password,
                queryparams, JaxbTaskSummaryListResponse.class);

        assertEquals("Incorrect num tasks", 1, taskSumlistResponse.getResult().size() );
        taskSum = taskSumlistResponse.getResult().get(0);
        assertEquals("Incorrect task id", taskId, task.getId().longValue());
    }
}
