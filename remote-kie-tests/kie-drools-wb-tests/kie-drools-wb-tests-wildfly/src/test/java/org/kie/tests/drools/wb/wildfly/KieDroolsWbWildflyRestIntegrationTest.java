/*
 * JBoss, Home of Professional Open Source
 * 
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.tests.drools.wb.wildfly;

import static org.kie.remote.tests.base.DeployUtil.getWebArchive;
import static org.kie.remote.tests.base.DeployUtil.replaceJars;
import static org.kie.tests.drools.wb.base.util.TestConstants.PROJECT_VERSION;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.kie.tests.drools.wb.base.AbstractKieDroolsWbIntegrationTest;

@RunAsClient
@RunWith(Arquillian.class)
public class KieDroolsWbWildflyRestIntegrationTest extends AbstractKieDroolsWbIntegrationTest {

    private static final String classifier = "wildfly8";
    
    protected static WebArchive createTestWar() {
        // Import kie-wb war
        WebArchive war = getWebArchive("org.kie", "kie-drools-wb-distribution-wars", classifier, PROJECT_VERSION);

        String [][] jarsToReplace = { 
                { "org.guvnor", "guvnor-rest-client" },
                { "org.guvnor", "guvnor-rest-backend" }
        };
        replaceJars(war, PROJECT_VERSION, jarsToReplace);
        
        return war;
    }
    
    @Deployment(testable = false, name="kie-drools-wb-wildfly")
    public static Archive<?> createWar() {
       return createTestWar();
    }
    
}
