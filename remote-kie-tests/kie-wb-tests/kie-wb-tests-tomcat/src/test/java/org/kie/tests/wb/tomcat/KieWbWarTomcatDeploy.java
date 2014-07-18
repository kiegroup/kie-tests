package org.kie.tests.wb.tomcat;

import static org.kie.remote.tests.base.DeployUtil.*;
import static org.kie.tests.wb.base.util.TestConstants.projectVersion;

import java.io.File;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWarTomcatDeploy {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbWarTomcatDeploy.class);

    static WebArchive createTestWar(boolean replace) {
        return createTestWar(null, replace);
    }
    
    static WebArchive createTestWar() {
        return createTestWar(null, true);
    }
    
    static WebArchive createTestWar(String database, boolean replace) {
        // Import kie-wb war
        WebArchive war = getWebArchive("org.kie", "kie-wb-distribution-wars", "tomcat7", projectVersion);
        
        // Replace persistence.xml with postgres version
        if( database != null ) { 
            war.delete("WEB-INF/classes/META-INF/persistence.xml");
            if( "oracle".equals(database) ) { 
                war.addAsResource("META-INF/persistence-oracle.xml", "META-INF/persistence.xml");
            } else if( "postgresql".equals(database) || "postgres".equals(database) ) { 
                war.addAsResource("META-INF/persistence-postgres.xml", "META-INF/persistence.xml");
            } else { 
                throw new IllegalArgumentException("Unknown database type: " + database );
            }
        } 

        // Tomcat JEE modifications
        logger.info( "Enabling Tomcat JEE changes in test war");
//        war.delete("WEB-INF/web.xml");
//        war.addAsWebInfResource("war/web-tomcat-jee.xml", "web.xml");
//        war.delete("WEB-INF/beans.xml");
//        war.addAsWebInfResource("war/beans-tomcat-jee.xml", "beans.xml");
        war.delete("WEB-INF/classes/META-INF/services/org.uberfire.security.auth.AuthenticationSource");
        war.addAsWebInfResource("war/org.uberfire.security.auth.AuthenticationSource-TOMCAT-JEE-SECURITY", 
                "classes/META-INF/services/org.uberfire.security.auth.AuthenticationSource");
        war.addAsWebInfResource("war/logging.properties", "classes/logging.properties");

        if( replace ) { 
            String [][] jarsToReplace = {
                    { "org.kie.remote", "kie-remote-services" }, 
                    { "org.kie.remote", "kie-remote-client" },
                    { "org.kie.remote", "kie-remote-jaxb" }
            };
        }
        // Add data service resource for tests
        war.addPackage("org/kie/tests/wb/base/services/data");
       
        return war;
    }

    protected void printTestName() { 
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        logger.info( "] Starting " + ste.getMethodName());
    }
    
}
