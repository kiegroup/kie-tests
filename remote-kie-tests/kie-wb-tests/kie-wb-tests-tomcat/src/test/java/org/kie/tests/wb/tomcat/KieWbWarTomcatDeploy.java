package org.kie.tests.wb.tomcat;

import static org.kie.remote.tests.base.DeployUtil.*;
import static org.kie.tests.wb.base.util.TestConstants.PROJECT_VERSION;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWarTomcatDeploy {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbWarTomcatDeploy.class);

    static WebArchive createTestWar() {
        return createTestWar(true);
    }
    
    static WebArchive createTestWar(boolean replace) {
        // Import kie-wb war
        WebArchive war = getWebArchive("org.kie", "kie-wb-distribution-wars", "tomcat7", PROJECT_VERSION);

        war.addAsWebInfResource("war/logging.properties", "classes/logging.properties");

        if( replace ) { 
            String [][] jarsToReplace = {
                    { "org.kie.remote", "kie-remote-services" },
                    { "org.kie.remote", "kie-remote-common" }
            };
            replaceJars(war, PROJECT_VERSION, jarsToReplace);
        }
      
        String [] jarsToDelete = { "cxf-bundle-jaxrs-2.7.11.jar" };
        deleteJars(war, jarsToDelete);
        
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
