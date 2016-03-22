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
package org.kie.tests.wb.eap;

import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.tests.wb.base.AbstractRemoteApiIntegrationTest;

public class LiveRemoteApiIntegrationTest extends AbstractRemoteApiIntegrationTest {

    public void liveSetDeploymentUrl() {
        // Modify this string to match your kie-wb/BPMS installation
        String urlString = "http://localhost:9080/kie-wb/";
        try {
            this.deploymentUrl = new URL(urlString);
        } catch( Exception e ) {
            System.err.println( "The following URL is not a valid URL: '" + urlString + "'");
            e.printStackTrace();
        }
    }

    public boolean doDeploy() {
        return false;
    }

    public String getContentType() {
        return MediaType.APPLICATION_XML;
    }

    @Override
    public boolean jmsQueuesAvailable() {
        return false;
    }

    @Override
    public boolean doRestTests() {
        return true;
    }

    @Override
    public RuntimeStrategy getStrategy() {
        return RuntimeStrategy.SINGLETON;
    }

    @Override
    public int getTimeoutInSecs() {
        return 4;
    }
}
