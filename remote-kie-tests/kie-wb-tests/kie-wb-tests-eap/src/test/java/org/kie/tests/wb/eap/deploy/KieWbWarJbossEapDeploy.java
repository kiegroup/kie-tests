package org.kie.tests.wb.eap.deploy;

import static org.kie.tests.wb.base.methods.TestConstants.projectVersion;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.kie.tests.wb.base.deploy.TestKjarDeploymentLoader;
import org.kie.tests.wb.base.test.AbstractDeploy;
import org.kie.tests.wb.base.test.objects.MyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWarJbossEapDeploy extends AbstractDeploy {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbWarJbossEapDeploy.class);
    
    protected static WebArchive createTestWar(String classifier) {
        return createTestWar(classifier, false);
    }
    
    protected static WebArchive createTestWar(String classifier, boolean testPostgres) {
        // Deploy test deployment
        createAndDeployTestKJarToMaven();
        
        // Import kie-wb war
        File [] warFile = 
                Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("org.kie:kie-wb-distribution-wars:war:" + classifier + ":" + projectVersion )
                .withoutTransitivity()
                .asFile();
        ZipImporter zipWar = ShrinkWrap.create(ZipImporter.class, "test.war").importFrom(warFile[0]);
        
        WebArchive war = zipWar.as(WebArchive.class);
        
        // Add kjar deployer
        war.addClass(TestKjarDeploymentLoader.class);
        
        // Replace persistence.xml with postgres version
        if( testPostgres ) { 
            war.delete("WEB-INF/classes/META-INF/persistence.xml");
            war.addAsResource("META-INF/persistence-postgres.xml", "META-INF/persistence.xml");
        }
        
        // Replace kie-services-remote jar with the one we just generated
        String [][] jarsToReplace = { 
                { "org.kie.remote", "kie-services-remote" },
                { "org.kie.remote", "kie-services-client" },
                { "org.jbpm", "jbpm-kie-services" },
                { "org.kie", "kie-internal" }
        };
        
        for( String [] jar : jarsToReplace ) { 
            logger.info( "Deleting " + jar[1] + " from test war");
            war.delete("WEB-INF/lib/" + jar[1] + "-" + projectVersion + ".jar");
        }
        String [] jarsArg = new String[jarsToReplace.length];
        for( int i = 0; i < jarsToReplace.length; ++i ) { 
           jarsArg[i] = jarsToReplace[i][0] + ":" + jarsToReplace[i][1];
           logger.info("About to resolve " + jarsArg[i]);
        }
        File [] kieRemoteDeps = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve(jarsArg)
                .withoutTransitivity()
                .asFile();
        war.addAsLibraries(kieRemoteDeps);
       
        // Add data service resource for tests
        war.addPackage("org/kie/tests/wb/base/services/data");
       
        return war;
    }

    protected void printTestName() { 
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        logger.info( "] Starting " + ste.getMethodName());
    }
    
}
