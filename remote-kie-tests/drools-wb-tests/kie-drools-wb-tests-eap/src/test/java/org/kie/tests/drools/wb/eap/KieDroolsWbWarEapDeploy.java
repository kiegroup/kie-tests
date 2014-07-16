package org.kie.tests.drools.wb.eap;

import static org.kie.remote.tests.base.DeployUtil.replaceJars;
import static org.kie.tests.drools.wb.base.methods.TestConstants.PROJECT_VERSION;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieDroolsWbWarEapDeploy {

    private static final String classifier = "eap-6_1";

    private static Logger logger = LoggerFactory.getLogger(KieDroolsWbWarEapDeploy.class);
    
    protected static WebArchive createTestWar() {
        // Import kie-wb war
        File[] warFile = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("org.kie:kie-drools-wb-distribution-wars:war:" + classifier + ":" + PROJECT_VERSION)
                .withoutTransitivity().asFile();
        
        ZipImporter zipWar = ShrinkWrap.create(ZipImporter.class, classifier + ".war").importFrom(warFile[0]);
        WebArchive war = zipWar.as(WebArchive.class);

        String [][] jarsToReplace = { 
                { "org.drools", "drools-wb-rest" },
                { "org.kie.workbench.services", "kie-wb-common-services-api" }
        };
       
        // Replace kie-services-remote jar with the one we just generated
        replaceJars(war, PROJECT_VERSION, jarsToReplace);
        
        return war;
    }

}