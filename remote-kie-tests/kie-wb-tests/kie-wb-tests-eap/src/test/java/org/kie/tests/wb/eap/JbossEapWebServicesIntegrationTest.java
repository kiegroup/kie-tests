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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.SCRIPT_TASK_PROCESS_ID;
import static org.kie.tests.wb.eap.KieWbWarJbossEapDeploy.createTestWar;

import java.net.URL;

import javax.xml.namespace.QName;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.remote.client.jaxb.JaxbCommandsRequest;
import org.kie.remote.client.jaxb.JaxbCommandsResponse;
import org.kie.remote.jaxb.gen.StartProcessCommand;
import org.kie.remote.services.ws.command.CommandWebServiceImpl;
import org.kie.remote.services.ws.command.generated.CommandServiceBasicAuthClient;
import org.kie.remote.services.ws.command.generated.CommandWebService;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandResponse;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessInstanceResponse;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunAsClient
@RunWith(Arquillian.class)
public class JbossEapWebServicesIntegrationTest {

    protected static final Logger logger = LoggerFactory.getLogger(JbossEapWebServicesIntegrationTest.class);

    @Deployment(testable = false, name = "kie-wb-eap-ws")
    public static Archive<?> createWar() {
        return createTestWar();
    }

    private KieWbRestIntegrationTestMethods restTests 
        = KieWbRestIntegrationTestMethods.newBuilderInstance().setDeploymentId(KJAR_DEPLOYMENT_ID).build();

    @ArquillianResource
    URL deploymentUrl;

    private static final String WSDL_PATH = "ws/CommandService?wsdl";

    protected static URL[] wsdlURL = new URL[2];
    protected static QName [] serviceName= new QName[2];
    protected static QName[] portName = new QName[2];
    
    static {
        serviceName[0] = new QName(CommandWebServiceImpl.NAMESPACE, "CommandServiceBasicAuth");
        portName[0] = new QName(CommandWebServiceImpl.NAMESPACE, "CommandServiceBasicAuthPort");
    }
    
    @Test
    public void testCommandWebService() throws Exception {
        CommandServiceBasicAuthClient client = createClient(); 
        
        logger.info("[Client] Webservice request.");
        // Get a response from the WebService
        StartProcessCommand cmd = new StartProcessCommand();
        cmd.setProcessId(SCRIPT_TASK_PROCESS_ID);
        JaxbCommandsRequest req = new JaxbCommandsRequest(KJAR_DEPLOYMENT_ID, cmd);
        CommandWebService commandWebService = client.getCommandServiceBasicAuthPort();
        final JaxbCommandsResponse response = commandWebService.execute(req);
        assertNotNull( "Null webservice response", response );
        assertFalse( "Empty webservice response", response.getResponses().isEmpty() );

        JaxbCommandResponse<?> cmdResp = response.getResponses().get(0);
        assertNotNull( "Null command response", cmdResp );
        if( ! (cmdResp instanceof JaxbProcessInstanceResponse) ) { 
            System.out.println( "!!: " + cmdResp.getClass().getSimpleName() );
            assertTrue( "Incorrect cmd response type", cmdResp instanceof JaxbProcessInstanceResponse );
        }
        
        logger.info("[WebService] response: {} [{}]", 
                ((JaxbProcessInstanceResponse) cmdResp).getId(),
                ((JaxbProcessInstanceResponse) cmdResp).getProcessId()
                );
    }

    private CommandServiceBasicAuthClient createClient() throws Exception {
        final String user = "mary";
        final String pwd = "mary123@";
 
        CommandServiceBasicAuthClient client =
        RemoteRuntimeEngineFactory.newCommandWebServiceClientBuilder()
            .addUserName(user)
            .addServerUrl(deploymentUrl)
            .addPassword(pwd)
            .buildBasicAuthClient();
        
        return client;
    }
    
}
