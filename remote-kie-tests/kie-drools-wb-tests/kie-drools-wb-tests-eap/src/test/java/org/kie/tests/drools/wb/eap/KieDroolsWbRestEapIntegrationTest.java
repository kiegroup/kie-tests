package org.kie.tests.drools.wb.eap;

import static org.kie.remote.tests.base.DeployUtil.getWebArchive;
import static org.kie.remote.tests.base.DeployUtil.replaceJars;
import static org.kie.tests.drools.wb.base.util.TestConstants.PROJECT_VERSION;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.runner.RunWith;
import org.kie.tests.drools.wb.base.AbstractKieDroolsWbIntegrationTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunAsClient
@RunWith(Arquillian.class)
public class KieDroolsWbRestEapIntegrationTest extends AbstractKieDroolsWbIntegrationTest {

    protected static WebArchive createTestWar() {
        // Import kie-wb war
        WebArchive war = getWebArchive("org.kie", "kie-drools-wb-distribution-wars", "eap6_3", PROJECT_VERSION);

        String [][] jarsToReplace = { 
                { "org.guvnor", "guvnor-rest-backend" },
                { "org.guvnor", "guvnor-rest-client" }
        };
        replaceJars(war, PROJECT_VERSION, jarsToReplace);
        
        return war;
    }
    
    @Deployment(testable = false, name="kie-drools-wb")
    public static Archive<?> createWar() {
       return createTestWar();
    }
   
    private static final String classifier = "eap-6_1";

    private static Logger logger = LoggerFactory.getLogger(KieDroolsWbRestEapIntegrationTest.class);
    
    protected static WebArchive createWarWithTestDeploymentLoader(String deployName) {
        // Import kie-wb war
        File[] warFile = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("org.kie:kie-drools-wb-distribution-wars:war:" + classifier + ":" + PROJECT_VERSION)
                .withoutTransitivity().asFile();
        
        ZipImporter zipWar = ShrinkWrap.create(ZipImporter.class, deployName + ".war").importFrom(warFile[0]);
        WebArchive war = zipWar.as(WebArchive.class);

        String [][] jarsToReplace = { 
                { "org.uberfire", "uberfire-commons" },
                { "org.uberfire", "uberfire-io" },
                { "org.guvnor", "guvnor-project-backend" },
                { "org.drools", "drools-wb-rest" },
                { "org.kie.workbench.services", "kie-wb-common-services-api" }
        };
        
        // Replace kie-services-remote jar with the one we just generated
        for( int i = 0; i < jarsToReplace.length; ++i ) { 
            war.delete("WEB-INF/lib/" + jarsToReplace[i][1] + "-" + PROJECT_VERSION + ".jar");
        }
        String [] jarsToAdd = new String[jarsToReplace.length];
        for( int i = 0; i < jarsToReplace.length; ++i ) { 
           jarsToAdd[i] = jarsToReplace[i][0] + ":" + jarsToReplace[i][1];
        }
        File [] kieRemoteDeps = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve(jarsToAdd)
                .withoutTransitivity()
                .asFile();
        for( File depFile : kieRemoteDeps ) { 
           logger.info( "Replacing " + depFile.getName());
        }
        war.addAsLibraries(kieRemoteDeps);
        
        return war;
    }

}
