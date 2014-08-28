package org.kie.tests.console.tomcat;

import static org.kie.tests.console.util.TestConstants.*;
import static org.kie.remote.tests.base.DeployUtil.getWebArchive;
import static org.kie.remote.tests.base.DeployUtil.replaceJars;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWarTomcatDeploy {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbWarTomcatDeploy.class);
    
    static WebArchive createTestWar() {
        // Import kie-wb war
        WebArchive war = getWebArchive("org.jbpm", "jbpm-console-ng-distribution-wars", "tomcat7", PROJECT_VERSION );

        war.addAsWebInfResource("war/logging.properties", "classes/logging.properties");

        String [][] jarsToReplace = {
                { "org.kie.remote", "kie-remote-services" }, 
                { "org.kie.remote", "kie-remote-jaxb" },
                { "org.kie.remote", "kie-remote-common" }
        };
        replaceJars(war, PROJECT_VERSION, jarsToReplace);
       
        return war;
    }

    protected void printTestName() { 
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        logger.info( "] Starting " + ste.getMethodName());
    }
    
}
