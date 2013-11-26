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

public class KieWbWarJbossEapDeploy extends AbstractDeploy {

    protected static final String PROCESS_ID = "org.jbpm.humantask";
    
    protected static WebArchive createWarWithTestDeploymentLoader(String classifier) {
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
        
        // Replace kie-services-remote jar with the one we just generated
        war.delete("WEB-INF/lib/kie-services-remote-" + projectVersion + ".jar");
        war.delete("WEB-INF/lib/kie-services-client-" + projectVersion + ".jar");
        war.delete("WEB-INF/lib/jbpm-kie-services-" + projectVersion + ".jar");
        File [] kieRemoteDeps = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("org.kie.remote:kie-services-remote", "org.kie.remote:kie-services-client", "org.jbpm:jbpm-kie-services")
                .withoutTransitivity()
                .asFile();
        war.addAsLibraries(kieRemoteDeps);
       
        // Add data service resource for tests
        war.addPackage("org/kie/tests/wb/base/services/data");
       
        // extra JAXB classes / own classes tests
        war.addClass(MyType.class);
        
        // Deploy test deployment
        createAndDeployTestKJarToMaven();
        
        return war;
    }

}
