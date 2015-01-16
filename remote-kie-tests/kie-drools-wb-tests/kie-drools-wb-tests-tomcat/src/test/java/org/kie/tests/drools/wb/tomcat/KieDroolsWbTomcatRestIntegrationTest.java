package org.kie.tests.drools.wb.tomcat;

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
public class KieDroolsWbTomcatRestIntegrationTest extends AbstractKieDroolsWbIntegrationTest {

    @Deployment(testable = false, name="kie-drools-wb-tomcat")
    public static Archive<?> createWar() {
       return createWarWithTestDeploymentLoader("tomcat");
    }
 
   private static final String classifier = "tomcat7";
    
    private static Logger logger = LoggerFactory.getLogger(KieDroolsWbTomcatRestIntegrationTest.class);

    protected static WebArchive createWarWithTestDeploymentLoader(String deployName) {
        // Import kie-wb war
        File[] warFile = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("org.kie:kie-drools-wb-distribution-wars:war:" + classifier + ":" + PROJECT_VERSION)
                .withoutTransitivity().asFile();
        
        ZipImporter zipWar = ShrinkWrap.create(ZipImporter.class, deployName + ".war").importFrom(warFile[0]);
        WebArchive war = zipWar.as(WebArchive.class);

        String [][] jarsToReplace = { 
                { "org.guvnor", "guvnor-rest-backend" },
                { "org.guvnor", "guvnor-rest-client" }
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
