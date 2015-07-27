package org.kie.tests.wb.eap;

import static org.kie.remote.tests.base.DeployUtil.*;
import static org.kie.remote.tests.base.DeployUtil.replaceJars;
import static org.kie.tests.wb.base.util.TestConstants.PROJECT_VERSION;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWarJbossEapDeploy {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbWarJbossEapDeploy.class);

    static WebArchive createTestWar() {
        return createTestWar(null);
    }

    private static final String classifier = "eap6_4";

    static WebArchive createTestWar( String database ) {
        WebArchive war = getWebArchive("org.kie", "kie-wb-distribution-wars", classifier, PROJECT_VERSION);

        String[][] jarsToReplace = {
                // kie-internal
                { "org.kie", "kie-internal" },
                // drools
                { "org.drools", "drools-core" },
                // jbpm
                { "org.jbpm", "jbpm-persistence-jpa" },
                { "org.jbpm", "jbpm-audit" },
                { "org.jbpm", "jbpm-flow" },
                { "org.jbpm", "jbpm-human-task-audit" },
                { "org.jbpm", "jbpm-human-task-core" },
                { "org.jbpm", "jbpm-kie-services" },
                // kie-remote
                
                { "org.kie.remote", "kie-remote-services" }
        };
        replaceJars(war, PROJECT_VERSION, jarsToReplace);

        boolean webXmlReplace = false;
        if( webXmlReplace ) {
            replaceWebXmlForWebServices(war);
        }

        return war;
    }

    private static void replaceWebXmlForWebServices( WebArchive war ) {
        war.delete("WEB-INF/web.xml");
        war.addAsWebInfResource("WEB-INF/web.xml", "web.xml");
    }

    protected void printTestName() {
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        logger.info("] Starting " + ste.getMethodName());
    }

}
