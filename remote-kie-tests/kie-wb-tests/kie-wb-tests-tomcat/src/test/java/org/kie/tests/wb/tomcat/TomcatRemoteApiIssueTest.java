/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
package org.kie.tests.wb.tomcat;

import static org.kie.tests.wb.base.util.TestConstants.*;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.tomcat.KieWbWarTomcatDeploy.createTestWar;

import java.net.URL;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.kie.tests.wb.base.methods.RepositoryDeploymentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunAsClient
@RunWith(Arquillian.class)
public class TomcatRemoteApiIssueTest {

    @Deployment(testable = false, name = "kie-wb-tomcat")
    public static Archive<?> createWar() {
        return createTestWar();
    }

    private static final Logger logger = LoggerFactory.getLogger(TomcatRemoteApiIssueTest.class);

    @ArquillianResource
    URL deploymentUrl;


    @AfterClass
    public static void waitForTxOnServer() throws InterruptedException {
        long sleep = 1000;
        logger.info("Waiting " + sleep / 1000 + " secs for tx's on server to close.");
        Thread.sleep(sleep);
    }

    protected void printTestName() {
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println( "-=> " + testName );
    }

    @Test
    public void issueTest() throws Exception {
        printTestName();
        String username = MARY_USER;
        String password = MARY_PASSWORD;

        // deploy

        boolean deploy = true;
        if( deploy ) {
            RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, username, password, 5);
            deployUtil.setStrategy(RuntimeStrategy.SINGLETON);

            String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
            String repositoryName = "tests";
            String project = "integration-tests";
            String deploymentId = KJAR_DEPLOYMENT_ID;
            String orgUnit = UUID.randomUUID().toString();
            deployUtil.createRepositoryAndDeployProject(repoUrl, repositoryName, project, deploymentId, orgUnit);
        }

        int sleep = 2;
        logger.info("Waiting {} more seconds to make sure deploy is done..", sleep);
        Thread.sleep(sleep * 1000);

        KieWbRestIntegrationTestMethods restTests = KieWbRestIntegrationTestMethods.newBuilderInstance()
                .setDeploymentId(KJAR_DEPLOYMENT_ID)
                .setMediaType(MediaType.APPLICATION_XML)
                .setStrategy(RuntimeStrategy.PER_PROCESS_INSTANCE)
                .setTimeoutInSecs(5)
                .build();

        restTests.remoteApiInsecureHumanTaskProcess(deploymentUrl);
    }
}
