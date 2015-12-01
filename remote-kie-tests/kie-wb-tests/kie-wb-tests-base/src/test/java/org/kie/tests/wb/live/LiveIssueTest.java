package org.kie.tests.wb.live;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.remote.client.api.RemoteRuntimeEngineFactory;
import org.kie.remote.tests.base.unit.GetIgnoreRule;
import org.kie.remote.tests.base.unit.GetIgnoreRule.IgnoreIfGETFails;
import org.kie.tests.Person;
import org.kie.tests.wb.base.methods.RepositoryDeploymentUtil;
import org.kie.tests.wb.base.util.TestConstants;
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
    @IgnoreIfGETFails(getUrl="http://localhost:8080/business-central/rest-api.jsp", getUserName="mary", getPassword="mary123@")
    public void issueTest() throws Exception {

        // deploy
        boolean doDeploy = false;

        if( doDeploy) {
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
        }

        // Configure the RuntimeEngine instance with the necessarry information to communicate with the REST services, and build it
        RuntimeEngine engine = RemoteRuntimeEngineFactory.newRestBuilder()
                .addDeploymentId("org.kie.example:project1:1.0.0-SNAPSHOT")
                .addUrl(deploymentUrl)
                .addUserName(user)
                .addPassword(password)
                .build();

        KieSession ksession = engine.getKieSession();

        Map<String, Object> params = new HashMap<String, Object>();
        String varId = "myobject";
        int varVal = 10;
        String txtVal = "test";
        Person type = new Person();
        type.setName("Suresh");
        params.put(varId, type);
        ProcessInstance procInst = ksession.startProcess(TestConstants.OBJECT_VARIABLE_PROCESS_ID, params);
        long processInstanceId = procInst.getId();
    }
}
