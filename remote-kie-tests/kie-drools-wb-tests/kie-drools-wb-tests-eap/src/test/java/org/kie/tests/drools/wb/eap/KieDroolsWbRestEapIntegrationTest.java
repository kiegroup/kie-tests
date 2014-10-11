package org.kie.tests.drools.wb.eap;

import static org.kie.tests.drools.wb.eap.KieDroolsWbWarEapDeploy.createWarWithTestDeploymentLoader;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.runner.RunWith;
import org.kie.tests.drools.wb.base.AbstractDroolsWbIntegrationTest;

@RunAsClient
@RunWith(Arquillian.class)
public class KieDroolsWbRestEapIntegrationTest extends AbstractDroolsWbIntegrationTest {

    @Deployment(testable = false, name="drools-wb")
    public static Archive<?> createWar() {
       return createWarWithTestDeploymentLoader("test");
    }
    
}