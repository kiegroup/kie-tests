/*
t  * JBoss, Home of Professional Open Source
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
package org.kie.tests.wb.eap.issues;

import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.eap.KieWbWarJbossEapDeploy.createTestWar;

import java.net.URL;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.remote.tests.base.unit.MavenBuildIgnoreRule;
import org.kie.remote.tests.base.unit.MavenBuildIgnoreRule.IgnoreWhenInMavenBuild;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.kie.tests.wb.base.methods.RepositoryDeploymentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunAsClient
@RunWith(Arquillian.class)
public class JbossEapRemoteApiIssueTest {

    @Deployment(testable = false, name = "kie-wb-eap")
    public static Archive<?> createWar() {
        return createTestWar();
    }

    private static final Logger logger = LoggerFactory.getLogger(JbossEapRemoteApiIssueTest.class);

    @ArquillianResource
    URL deploymentUrl;

    @Rule
    public MavenBuildIgnoreRule rule = new MavenBuildIgnoreRule();

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

    private static final String USER_ID = MARY_USER;
    private static final String PASSWORD = MARY_PASSWORD;

    private static final AtomicBoolean deploymentDeployed = new AtomicBoolean(false);

    @Before
    public void deployTestDeployment() throws Exception {
        if( deploymentDeployed.compareAndSet(false, true) ) {
            // deploy
            RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, USER_ID, PASSWORD, 5);
            deployUtil.setStrategy(RuntimeStrategy.SINGLETON);

            String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
            String repositoryName = "tests";
            String project = "integration-tests";
            String deploymentId = KJAR_DEPLOYMENT_ID;
            String orgUnit = UUID.randomUUID().toString();
            orgUnit = orgUnit.substring(0, orgUnit.indexOf("-"));
            deployUtil.createRepositoryAndDeployProject(repoUrl, repositoryName, project, deploymentId, orgUnit);

            int sleep = 2;
            logger.info("Waiting {} more seconds to make sure deploy is done..", sleep);
            Thread.sleep(sleep * 1000);
        }

        printTestName();
    }

    @Test
    @IgnoreWhenInMavenBuild
    public void issueTest() throws Exception {

        KieWbRestIntegrationTestMethods restTests = KieWbRestIntegrationTestMethods.newBuilderInstance()
                .setDeploymentId(KJAR_DEPLOYMENT_ID)
                .setMediaType(MediaType.APPLICATION_XML)
                .setStrategy(RuntimeStrategy.SINGLETON)
                .setTimeoutInSecs(5)
                .build();

        restTests.urlsProcessQueryOperations(deploymentUrl, USER_ID, PASSWORD);
    }
}
