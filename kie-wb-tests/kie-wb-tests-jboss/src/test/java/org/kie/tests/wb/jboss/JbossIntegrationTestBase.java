package org.kie.tests.wb.jboss;

import static org.junit.Assert.assertNotEquals;
import static org.kie.tests.wb.jboss.setup.TestDeploymentLoader.*;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.kie.api.task.model.TaskSummary;
import org.kie.tests.wb.jboss.jms.JbossAsJmsIntegrationTest;
import org.kie.tests.wb.jboss.setup.TestDeploymentLoader;

public class JbossIntegrationTestBase {

    protected static final String USER = "guest";
    protected static final String PASSWORD = "1234";
    
    protected final static String projectVersion;
    static { 
        Properties testProps = new Properties();
        try {
            testProps.load(JbossAsJmsIntegrationTest.class.getResourceAsStream("/test.properties"));
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize projectVersion property: " + e.getMessage(), e);
        }
        projectVersion = testProps.getProperty("project.version");
    }
    
    protected static final String DEPLOYMENT_ID;
    static { 
        KModuleDeploymentUnit deploymentUnit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION, KBASE_NAME, KSESSION_NAME);
        DEPLOYMENT_ID = deploymentUnit.getIdentifier();
    }
    
    protected static final String PROCESS_ID = "org.jbpm.humantask";
    
    @Deployment(testable = false)
    public static WebArchive createWarWithTestDeploymentLoader() {
        File [] warFile = 
                Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("org.kie:kie-wb-distribution-wars:war:jboss-as7:" + projectVersion )
                .withoutTransitivity()
                .asFile();
        
        ZipImporter zipWar = ShrinkWrap.create(ZipImporter.class, "kie-wb-test.war").importFrom(warFile[0]);
        
        WebArchive war = zipWar.as(WebArchive.class);
        war.addClass(TestDeploymentLoader.class);
        
        TestDeploymentLoader.deployKjarToMaven();
        
        return war;
    }
    
    protected long findTaskId(long procInstId, List<TaskSummary> taskSumList) { 
        long taskId = -1;
        for( TaskSummary task : taskSumList ) { 
            if( task.getProcessInstanceId() == procInstId ) {
                taskId = task.getId();
            }
        }
        assertNotEquals("Could not determine taskId!", -1, taskId);
        return taskId;
    }
    
}
