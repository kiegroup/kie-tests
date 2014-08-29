package org.kie.tests.wb.wildfly;

import static org.kie.remote.tests.base.DeployUtil.*;
import static org.kie.tests.wb.base.util.TestConstants.PROJECT_VERSION;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWarWildflyDeploy {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbWarWildflyDeploy.class);
    
    static WebArchive createTestWar(String classifier) {
        // Import kie-wb war
        WebArchive war = getWebArchive("org.kie", "kie-wb-distribution-wars", "jboss-as7", PROJECT_VERSION);
       
        String [][] jarsToReplace = {
                { "org.kie.remote", "kie-remote-services" }, 
                { "org.kie.remote", "kie-remote-jaxb" },
                { "org.kie.remote", "kie-remote-common" }
        };
        replaceJars(war, PROJECT_VERSION, jarsToReplace);
        
       
        // Add data service resource for tests
        war.addPackage("org/kie/tests/wb/base/services/data");
        
        return war;
    }
    
}
