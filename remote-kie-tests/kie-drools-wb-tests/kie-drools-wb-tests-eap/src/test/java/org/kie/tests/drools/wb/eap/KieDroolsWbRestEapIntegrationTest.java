package org.kie.tests.drools.wb.eap;

import static org.kie.remote.tests.base.DeployUtil.getWebArchive;
import static org.kie.remote.tests.base.DeployUtil.replaceJars;
import static org.kie.tests.drools.wb.base.util.TestConstants.PROJECT_VERSION;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.kie.tests.drools.wb.base.AbstractKieDroolsWbIntegrationTest;

@RunAsClient
@RunWith(Arquillian.class)
public class KieDroolsWbRestEapIntegrationTest extends AbstractKieDroolsWbIntegrationTest {

    private static final String classifier = "eap6_4";
    
    protected static WebArchive createTestWar() {
        // Import kie-wb war
        WebArchive war = getWebArchive("org.kie", "kie-drools-wb-distribution-wars", classifier, PROJECT_VERSION);

        String [][] jarsToReplace = { 
                { "org.guvnor", "guvnor-rest-client" },
                { "org.guvnor", "guvnor-rest-backend" }
        };
        replaceJars(war, PROJECT_VERSION, jarsToReplace);
        
        return war;
    }
    
    @Deployment(testable = false, name="kie-drools-wb")
    public static Archive<?> createWar() {
       return createTestWar();
    }
   

}
