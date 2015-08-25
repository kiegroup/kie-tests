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
package org.kie.tests.wb.eap;

import static org.junit.Assert.*;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.eap.KieWbWarJbossEapDeploy.createTestWar;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Arrays;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.remote.client.jaxb.ClientJaxbSerializationProvider;
import org.kie.remote.client.jaxb.JaxbCommandsRequest;
import org.kie.remote.jaxb.gen.JaxbStringObjectPairArray;
import org.kie.remote.jaxb.gen.StartProcessCommand;
import org.kie.remote.jaxb.gen.util.JaxbStringObjectPair;
import org.kie.tests.MyType;
import org.kie.tests.wb.base.methods.RepositoryDeploymentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunAsClient
@RunWith(Arquillian.class)
public class CallIssueTest {

    @Deployment(testable = false, name = "kie-wb-eap")
    public static Archive<?> createWar() {
        return createTestWar();
    }
 
    private static final Logger logger = LoggerFactory.getLogger(CallIssueTest.class);
    
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
    
    private static String basicAuthenticationHeader( String user, String password ) {
        String token = user + ":" + password;
        try {
            return "BASIC " + DatatypeConverter.printBase64Binary(token.getBytes("UTF-8"));
        } catch( UnsupportedEncodingException ex ) {
            throw new IllegalStateException("Cannot encode with UTF-8", ex);
        }
    } 
        
    @Test
    public void issueTest() throws Exception { 
        setup();
      
        // command request
        JaxbCommandsRequest req = new JaxbCommandsRequest();
        req.setDeploymentId("org.test:kjar:1.0");
       
        // start process command
        StartProcessCommand cmd = new StartProcessCommand();
        req.getCommands().add(cmd);
        cmd.setProcessId("org.test.kjar.HumanTaskWithOwnType");
        JaxbStringObjectPairArray map = new JaxbStringObjectPairArray();
        cmd.setParameter(map);
        JaxbStringObjectPair pair = new JaxbStringObjectPair();
        map.getItems().add(pair);
        pair.setKey("myObject");
        
        // process variable object
        MyType val = new MyType();
        val.setData(23);
        val.setText("23-is-a-lucky-number");
        pair.setValue(val);
      
        // serialize request to xml
        Class<?> [] classes = { MyType.class };
        String xml = ClientJaxbSerializationProvider.newInstance(Arrays.asList(classes)).serialize(req);
        System.out.println( "===" );
        System.out.println( xml );
        System.out.println( "===" );
       
        // create HTTP POST request
        Request httpRequest = Request.Post("http://localhost:8080/test/rest/execute")
            .bodyString(xml, ContentType.APPLICATION_XML)
            .addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .addHeader("Kie-Deployment-Id", "org.test:kjar:1.0") // needed for deserialization on the server side
            .addHeader(HttpHeaders.AUTHORIZATION, basicAuthenticationHeader(MARY_USER, MARY_PASSWORD));
        
        // do request
        Response httpResp = httpRequest.execute();
        int status = httpResp.returnResponse().getStatusLine().getStatusCode();
        assertEquals( "POST request did not complete successfully, but with status: " + status, 200, status );
    }
    
    public void setup() throws Exception { 
        printTestName();
        String username = MARY_USER;
        String password = MARY_PASSWORD;
        
        // deploy

        RepositoryDeploymentUtil deployUtil = new RepositoryDeploymentUtil(deploymentUrl, username, password, 5);
        deployUtil.setStrategy(RuntimeStrategy.SINGLETON);

        String repoUrl = "https://github.com/droolsjbpm/jbpm-playground.git";
        String repositoryName = "tests";
        String project = "integration-tests";
        String deploymentId = KJAR_DEPLOYMENT_ID;
        String orgUnit = UUID.randomUUID().toString();
        deployUtil.createRepositoryAndDeployProject(repoUrl, repositoryName, project, deploymentId, orgUnit);

        int sleep = 2;
        logger.info("Waiting {} more seconds to make sure deploy is done..", sleep);
        Thread.sleep(sleep * 1000); 
    }
}
