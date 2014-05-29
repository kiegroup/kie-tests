package org.kie.tests.wb.eap;

import static org.kie.tests.wb.base.methods.TestConstants.projectVersion;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.kie.tests.wb.base.deploy.TestKjarDeploymentLoader;
import org.kie.tests.wb.base.test.AbstractDeploy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWarJbossEapDeploy extends AbstractDeploy {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbWarJbossEapDeploy.class);
    
    static WebArchive createTestWar(String classifier, boolean replace) {
        return createTestWar(classifier, null, replace);
    }
    
    static WebArchive createTestWar(String classifier) {
        return createTestWar(classifier, null, true);
    }
    
    static WebArchive createTestWar(String classifier, String database, boolean replace) {
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
        if( database != null ) { 
            war.delete("WEB-INF/classes/META-INF/persistence.xml");
            if( "oracle".equals(database) ) { 
                war.addAsResource("META-INF/persistence-oracle.xml", "META-INF/persistence.xml");
            } else if( "postgres".equals(database) ) { 
                war.addAsResource("META-INF/persistence-postgres.xml", "META-INF/persistence.xml");
            } else { 
                throw new IllegalArgumentException("Unknown database type: " + database );
            }
        } 

        if( replace ) { 
            // Replace kie-services-remote jar with the one we just generated
            replaceJars(war);
            addNewJars(war);
            replaceWebXmlForWebServices(war);
        }
       
        // Add data service resource for tests
        war.addPackage("org/kie/tests/wb/base/services/data");
     
        return war;
    }

    private static void replaceWebXmlForWebServices(WebArchive war) {
        war.delete("WEB-INF/web.xml");
        war.addAsWebInfResource("WEB-INF/web.xml", "web.xml");
    }

    protected void printTestName() { 
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        logger.info( "] Starting " + ste.getMethodName());
    }
  
    private static void replaceJars(WebArchive war) { 
       String [][] jarsToReplace = { 
                { "org.jbpm", "jbpm-human-task-core" },
                { "org.kie.remote", "kie-services-remote" },
                { "org.kie.remote", "kie-services-jaxb" },
                { "org.kie.remote.ws", "kie-remote-ws-common" },
                { "org.kie.remote.ws", "kie-remote-ws-wsdl-cmd" }
        };
        String [] jarsArg = new String[jarsToReplace.length];
        for( String [] jar : jarsToReplace ) { 
            logger.info( "Deleting '{}' from test war", jar[1] );
            war.delete("WEB-INF/lib/" + jar[1] + "-" + projectVersion + ".jar");
        }
        for( int i = 0; i < jarsToReplace.length; ++i ) { 
            jarsArg[i] = jarsToReplace[i][0] + ":" + jarsToReplace[i][1];
            logger.info("Resolving '{}'", jarsArg[i]);
        }

        
        File [] kieRemoteDeps = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve(jarsArg)
                .withoutTransitivity()
                .asFile();
        for( File dep : kieRemoteDeps ) { 
            logger.info("Replacing with '{}'", dep.getName() ); 
        }
        war.addAsLibraries(kieRemoteDeps);
       
    }
    
    private static void addNewJars(WebArchive war) { 
        war.delete("WEB-INF/lib/jaxws-api-2.1.jar");
        
        String [][] jarsToAdd = { 
                { "javax.xml.ws", "jaxws-api" }
        };
        if( jarsToAdd.length > 0 ) { 
            String [] jarsArg = new String[jarsToAdd.length];
            for( int i = 0; i < jarsToAdd.length; ++i ) { 
                jarsArg[i] = jarsToAdd[i][0] + ":" + jarsToAdd[i][1];
                logger.info("Resolving '{}'", jarsArg[i]);
            }

            File [] depsToAdd = Maven.resolver()
                    .loadPomFromFile("pom.xml")
                    .resolve(jarsArg)
                    .withoutTransitivity()
                    .asFile();
            for( File dep : depsToAdd ) { 
                logger.info("Adding '{}'", dep.getName() ); 
            }
            war.addAsLibraries(depsToAdd);
        }
    }
}
