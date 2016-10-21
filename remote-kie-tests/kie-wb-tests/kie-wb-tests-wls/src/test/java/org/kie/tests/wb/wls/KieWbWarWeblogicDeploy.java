package org.kie.tests.wb.wls;

import static org.kie.remote.tests.base.DeployUtil.getWebArchive;
import static org.kie.remote.tests.base.DeployUtil.replaceJars;
import static org.kie.tests.wb.base.util.TestConstants.PROJECT_VERSION;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWarWeblogicDeploy {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbWarWeblogicDeploy.class);

    public static WebArchive createTestWar() {
        return createTestWar(null);
    }

    private static final String classifier = "weblogic12";

    static WebArchive createTestWar( String database ) {
        WebArchive war = getWebArchive("org.kie", "kie-wb-distribution-wars", classifier, PROJECT_VERSION);

        String[][] jarsToReplace = {
                { "org.drools", "drools-compiler" },

                // kie-remote
                { "org.kie.remote", "kie-remote-services" }
        };
        replaceJars(war, PROJECT_VERSION, jarsToReplace);

        if( false ) {
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