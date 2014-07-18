package org.kie.tests.wb.base;

import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.AfterClass;
import org.junit.Test;
import org.kie.internal.deployment.DeploymentUnit.RuntimeStrategy;
import org.kie.tests.wb.base.methods.RestRepositoryDeploymentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractIssueIntegrationTest {
    
    protected static final Logger logger = LoggerFactory.getLogger(AbstractIssueIntegrationTest.class);
    
    @ArquillianResource
    URL deploymentUrl;
   
    public abstract boolean doDeploy();
    public abstract MediaType getMediaType();
    public abstract boolean useFormBasedAuth();
    public abstract RuntimeStrategy getStrategy();
    public abstract long getTimeout();
   
    public AbstractIssueIntegrationTest() { 
    
    }

    
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
    public void issueTest() throws Exception { 
        printTestName();
        RestRepositoryDeploymentUtil deploymentUtil = new RestRepositoryDeploymentUtil(deploymentUrl, MARY_USER, MARY_PASSWORD);
        
        String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
        String repositoryName = "playground";
        String project = "integration-tests";
        String deploymentId = "org.test:kjar:1.0";
        String orgUnit = "integTestUser";
        String user = MARY_USER;
        deploymentUtil.createAndDeployRepository(repoUrl, repositoryName, project, deploymentId, orgUnit, user, 5);
    }
    
}
