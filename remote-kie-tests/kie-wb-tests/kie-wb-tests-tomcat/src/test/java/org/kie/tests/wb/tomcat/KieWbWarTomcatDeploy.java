package org.kie.tests.wb.tomcat;

import static org.kie.remote.tests.base.DeployUtil.*;
import static org.kie.tests.wb.base.util.TestConstants.PROJECT_VERSION;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWarTomcatDeploy {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbWarTomcatDeploy.class);

    private static final String classifier = "tomcat7";
    
    static WebArchive createTestWar() {
        // Import kie-wb war
        WebArchive war = getWebArchive("org.kie", "kie-wb-distribution-wars", classifier, PROJECT_VERSION);

        String [][] jarsToReplace = {
                { "org.kie.remote", "kie-remote-services" },
                { "org.guvnor", "guvnor-rest-client" },
                { "org.guvnor", "guvnor-rest-backend" }
        };
        replaceJars(war, PROJECT_VERSION, jarsToReplace);

        boolean replaceWebXml = false;
        if( replaceWebXml ) { 
          war.delete("WEB-INF/web.xml");
          war.addAsWebResource("war/web.xml");
        }
        
        return war;
    }

    protected void printTestName() { 
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        logger.info( "] Starting " + ste.getMethodName());
    }
    
}
