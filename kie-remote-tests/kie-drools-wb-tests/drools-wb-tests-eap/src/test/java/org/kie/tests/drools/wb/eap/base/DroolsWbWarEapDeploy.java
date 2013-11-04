package org.kie.tests.drools.wb.eap.base;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

public class DroolsWbWarEapDeploy {

    protected static final String PROCESS_ID = "org.jbpm.humantask";

    protected static WebArchive createWarWithTestDeploymentLoader(String deployName, String classifier) {
        return createWarWithTestDeploymentLoader(deployName, classifier, false, false);
    }

    protected static WebArchive createWarWithTestDeploymentLoader(String deployName, String classifier,
            boolean useExecServerWebXml, boolean mailDeps) {
        // Import kie-wb war
        File[] warFile = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("org.kie:kie-drools-wb-distribution-wars:war:" + classifier + ":" + "6.0.0-SNAPSHOT")
                .withoutTransitivity().asFile();
        
        ZipImporter zipWar = ShrinkWrap.create(ZipImporter.class, deployName + ".war").importFrom(warFile[0]);
        WebArchive war = zipWar.as(WebArchive.class);

        boolean addDepAndSucceed = false;
        if( addDepAndSucceed ) { 
            File[] formProviderDep = Maven.resolver()
                    .loadPomFromFile("pom.xml")
                    .resolve("org.jbpm:jbpm-form-modeler-form-provider")
                    .withTransitivity()
                    .asFile();
            war.addAsLibraries(formProviderDep);
        }

        // export in order to inspect it
        war.as(ZipExporter.class).exportTo(new File("target/" + war.getName()), true);
        
        return war;
    }

}
