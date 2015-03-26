package org.kie.tests.wb.live;

import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.kie.tests.wb.base.methods.RepositoryDeploymentUtil;

//@Ignore // add Junit "Ping Succeed or Ignore" rule
public class RepositoryDeploymentUtilLiveTest {

    private static String user = "mary";
    private static String password = "mary123@";
   
    
    private static URL deploymentUrl;
    static { 
        try {
            deploymentUrl =  new URL("http://localhost:8080/business-central/");
        } catch( MalformedURLException e ) {
            // do nothing
        }
    }
    
    @Test
    public void optimizedRepeatedCalls() { 
        // create repo if not present
        RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, user, password, 5);
        String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
        String repositoryName = "tests";
        String project = "integration-tests";
        String orgUnitName = UUID.randomUUID().toString();
        
        deployUtil.createRepositoryAndDeployProject(repoUrl, repositoryName, project, KJAR_DEPLOYMENT_ID, orgUnitName, user);
    }
}
