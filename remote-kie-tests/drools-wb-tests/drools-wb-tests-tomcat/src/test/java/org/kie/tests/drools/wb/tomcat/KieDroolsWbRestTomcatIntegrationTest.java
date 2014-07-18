package org.kie.tests.drools.wb.tomcat;

import static org.kie.remote.tests.base.DeployUtil.getWebArchive;
import static org.kie.remote.tests.base.DeployUtil.replaceJars;
import static org.kie.tests.drools.wb.base.util.TestConstants.PROJECT_VERSION;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.kie.tests.drools.wb.base.AbstractDroolsWbIntegrationTest;

@RunAsClient
@RunWith(Arquillian.class)
public class KieDroolsWbRestTomcatIntegrationTest extends AbstractDroolsWbIntegrationTest {

    protected static WebArchive createTestWar() {
        // Import kie-wb war
        WebArchive war = getWebArchive("org.kie", "kie-drools-wb-distribution-wars", "tomcat7", PROJECT_VERSION);

        String [][] jarsToReplace = { 
                { "org.drools", "drools-wb-rest" },
                { "org.guvnor", "guvnor-rest-backend" },
                { "org.guvnor", "guvnor-rest-client" }
        };
        replaceJars(war, PROJECT_VERSION, jarsToReplace);
        
        return war;
    }
    
    @Deployment(testable = false, name="drools-wb")
    public static Archive<?> createWar() {
       return createTestWar();
    }
    
}