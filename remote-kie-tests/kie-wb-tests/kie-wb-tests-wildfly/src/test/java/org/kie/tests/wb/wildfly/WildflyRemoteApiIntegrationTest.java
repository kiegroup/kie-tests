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
package org.kie.tests.wb.wildfly;

import static org.kie.tests.wb.wildfly.KieWbWarWildflyDeploy.createTestWar;

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
public class WildflyRemoteApiIntegrationTest extends AbstractRemoteApiIntegrationTest {

    @Deployment(testable = false, name = "kie-wb-jboss")
    public static Archive<?> createWar() {
        return createTestWar("jboss-as7");
    }

    public boolean doDeploy() { 
        return true;
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.APPLICATION_JSON_TYPE;
    }

    @Override
    public boolean jmsQueuesAvailable() {
        return true;
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
        return 7*1000;
    }
}
