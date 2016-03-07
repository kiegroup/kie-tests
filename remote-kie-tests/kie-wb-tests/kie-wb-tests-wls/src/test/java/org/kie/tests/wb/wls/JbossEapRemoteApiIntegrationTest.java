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
package org.kie.tests.wb.wls;

import static org.kie.tests.wb.wls.KieWbWarWeblogicDeploy.createTestWar;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.runner.RunWith;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.tests.wb.base.AbstractRemoteApiIntegrationTest;

@RunAsClient
@RunWith(Arquillian.class)
public class JbossEapRemoteApiIntegrationTest extends AbstractRemoteApiIntegrationTest {

    @Deployment(testable = false, name = "kie-wb-eap")
    public static Archive<?> createWar() {
        return createTestWar();
    }

    public boolean doDeploy() {
        return true;
    }

    public String getContentType() {
        return MediaType.APPLICATION_XML;
    }

    @Override
    public boolean jmsQueuesAvailable() {
        return true;
    }

    @Override
    public boolean doRestTests() {
        return false;
    }

    @Override
    public RuntimeStrategy getStrategy() {
        return RuntimeStrategy.SINGLETON;
    }

    @Override
    public int getTimeoutInSecs() {
        return 4;
    }

    public void noLiveSetDeploymentUrl() {
        // Modify this string to match your kie-wb/BPMS installation
        String urlString = "http://localhost:8080/business-central/";
        try {
            this.deploymentUrl = new URL(urlString);
        } catch( Exception e ) {
            System.err.println( "The following URL is not a valid URL: '" + urlString + "'");
            e.printStackTrace();
        }
    }
}
