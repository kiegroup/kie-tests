package org.kie.tests.wb.jboss;

import static org.kie.remote.tests.base.DeployUtil.getWebArchive;
import static org.kie.remote.tests.base.DeployUtil.replaceJars;
import static org.kie.tests.wb.base.util.TestConstants.PROJECT_VERSION;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWarJbossAsDeploy {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbWarJbossAsDeploy.class);
   
    public static WebArchive createTestWar() { 
        // Import kie-wb war
        WebArchive war = getWebArchive("org.kie", "kie-wb-distribution-wars", "jboss-as7", PROJECT_VERSION);
       
        String [][] jarsToReplace = {
                { "org.kie.remote", "kie-remote-services" }, 
                { "org.guvnor", "guvnor-rest-client" },
                { "org.guvnor", "guvnor-rest-backend" }
        };
        replaceJars(war, PROJECT_VERSION, jarsToReplace);
        
        return war;
    }

}
