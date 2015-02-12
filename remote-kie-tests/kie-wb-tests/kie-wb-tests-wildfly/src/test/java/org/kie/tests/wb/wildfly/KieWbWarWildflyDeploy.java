package org.kie.tests.wb.wildfly;

import static org.kie.remote.tests.base.DeployUtil.addNewJars;
import static org.kie.remote.tests.base.DeployUtil.getWebArchive;
import static org.kie.remote.tests.base.DeployUtil.replaceJars;
import static org.kie.tests.wb.base.util.TestConstants.PROJECT_VERSION;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWarWildflyDeploy {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbWarWildflyDeploy.class);

    private static final String classifier = "wildfly8";
    
    static WebArchive createTestWar() {
        // Import kie-wb war
        WebArchive war = getWebArchive("org.kie", "kie-wb-distribution-wars", classifier, PROJECT_VERSION);

        String[][] jarsToReplace = { 
                { "org.kie.remote", "kie-remote-services" }
                };
        replaceJars(war, PROJECT_VERSION, jarsToReplace);

        // ADD
        boolean add = false;
        if( add ) {
            String[][] jarsToAdd = {
            // web services
            { "javax.xml.ws", "jaxws-api" } };
            addNewJars(war, jarsToAdd);
        }

        return war;
    }

    private static void replaceWebXmlForWebServices( WebArchive war ) {
        war.delete("WEB-INF/web.xml");
        war.addAsWebInfResource("WEB-INF/web.xml", "web.xml");
    }
}
