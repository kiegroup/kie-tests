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
package org.kie.tests.wb.eap.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.kie.tests.wb.base.methods.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.methods.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.methods.TestConstants.MARY_USER;
import static org.kie.tests.wb.base.methods.TestConstants.SCRIPT_TASK_PROCESS_ID;
import static org.kie.tests.wb.base.methods.TestConstants.projectVersion;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.drools.core.command.runtime.process.StartProcessCommand;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQJMSConnectionFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.command.Command;
import org.kie.services.client.serialization.JaxbSerializationProvider;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandsRequest;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandsResponse;
import org.kie.tests.wb.base.methods.RestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@RunAsClient
//@RunWith(Arquillian.class)
@Ignore
public class KieWbSecurityIntegrationTest {

    protected static final Logger logger = LoggerFactory.getLogger(KieWbSecurityIntegrationTest.class);
   
    private RestIntegrationTestMethods restTests = new RestIntegrationTestMethods(KJAR_DEPLOYMENT_ID);
    
    @Deployment(testable=false, name = "kie-wb-security")
    public static Archive<?> createWar() {
        return createTestWar("eap-6_1");
    }

    static WebArchive createTestWar(String classifier) {
        logger.info( "] import");
        // Import kie-wb war
        File [] warFile = 
                Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("org.kie:kie-wb-distribution-wars:war:" + classifier + ":" + projectVersion )
                .withoutTransitivity()
                .asFile();
        ZipImporter zipWar = ShrinkWrap.create(ZipImporter.class, "test.war").importFrom(warFile[0]);
        
        logger.info( "] transform");
        WebArchive war = zipWar.as(WebArchive.class);
        
        // Add kjar deployer
        logger.info( "] add classes");
//        war.addClasses(SecurityBean.class, UserPassCallbackHandler.class);
     
        // Replace kie-services-remote jar with the one we just generated
        logger.info( "] replace libs");
        String [][] jarsToReplace = { 
                { "org.kie.remote", "kie-services-remote" },
                { "org.kie.remote", "kie-services-client" },
                { "org.kie.remote", "kie-services-jaxb" },
                { "org.jbpm", "jbpm-human-task-core" }
        };
        String [] jarsArg = new String[jarsToReplace.length];
        for( String [] jar : jarsToReplace ) { 
            war.delete("WEB-INF/lib/" + jar[1] + "-" + projectVersion + ".jar");
        }
        for( int i = 0; i < jarsToReplace.length; ++i ) { 
            jarsArg[i] = jarsToReplace[i][0] + ":" + jarsToReplace[i][1];
        }

        File [] kieRemoteDeps = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve(jarsArg)
                .withoutTransitivity()
                .asFile();
        for( File file : kieRemoteDeps ) { 
            logger.info( "Replacing " + file.getName());
        }
        war.addAsLibraries(kieRemoteDeps);
        
        // <run-as> added to ejb-jar.xml
        logger.info( "] Replace ejb-jar.xml");
        war.delete("WEB-INF/ejb-jar.xml");
        war.addAsWebInfResource("WEB-INF/ejb-jar.xml");
        
        logger.info( "] done");
        return war;
    }
  
    @ArquillianResource
    URL deploymentUrl;
   
    private InitialContext remoteInitialContext = null;
    private final JaxbSerializationProvider jaxbSerializationProvider = new JaxbSerializationProvider();
    
    private boolean useSsl = true;
    
    private static final String KSESSION_QUEUE_NAME = "jms/queue/KIE.SESSION";
    private static final String TASK_QUEUE_NAME = "jms/queue/KIE.TASK";
    private static final String RESPONSE_QUEUE_NAME = "jms/queue/KIE.RESPONSE";
    
    private static final long QUALITY_OF_SERVICE_THRESHOLD_MS = 5 * 1000;
 
    
    @Test
    public void securityTest() throws Exception { 
        // restTests.urlsDeployModuleForOtherTests(deploymentUrl, MARY_USER, MARY_PASSWORD, false);
       
        this.remoteInitialContext = getRemoteInitialContext(MARY_USER, MARY_PASSWORD);
        
        logger.info("-->");
        Command<?> cmd = new StartProcessCommand(SCRIPT_TASK_PROCESS_ID);
        JaxbCommandsRequest req = new JaxbCommandsRequest(KJAR_DEPLOYMENT_ID, cmd);
        sendJmsJaxbCommandsRequest(KSESSION_QUEUE_NAME, req, MARY_USER, MARY_PASSWORD);
        logger.info("<--");
    }
    
    private static InitialContext getRemoteInitialContext(String user, String password) {
        Properties initialProps = new Properties();
        initialProps.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        initialProps.setProperty(InitialContext.PROVIDER_URL, "remote://localhost:4447");
        initialProps.setProperty(InitialContext.SECURITY_PRINCIPAL, user);
        initialProps.setProperty(InitialContext.SECURITY_CREDENTIALS, password);

        for (Object keyObj : initialProps.keySet()) {
            String key = (String) keyObj;
            System.setProperty(key, (String) initialProps.get(key));
        }
        try {
            return new InitialContext(initialProps);
        } catch (NamingException e) {
            throw new RuntimeException("Unable to create " + InitialContext.class.getSimpleName(), e);
        }
    }

    private ConnectionFactory getConnectionFactory(boolean useSsl) { 
        ConnectionFactory factory;
        Map<String, Object> connParams = new HashMap<String, Object>();  
        if( ! useSsl ) { 
            connParams.put(TransportConstants.PORT_PROP_NAME, 5445);  
            connParams.put(TransportConstants.HOST_PROP_NAME, "localhost");
            connParams.put(org.hornetq.core.remoting.impl.netty.TransportConstants.SSL_ENABLED_PROP_NAME, false);  
        } else { 
            connParams.put(TransportConstants.PORT_PROP_NAME, 5446);  
            connParams.put(TransportConstants.HOST_PROP_NAME, "localhost");
            
            // arguments
            String password = "CLIENT_KEYSTORE_PASSWORD";
            String keystorePath = this.getClass().getResource("/ssl/client_keystore.jks").getPath();
            
            // SSL
            connParams.put(org.hornetq.core.remoting.impl.netty.TransportConstants.SSL_ENABLED_PROP_NAME, true);  
            connParams.put(TransportConstants.KEYSTORE_PASSWORD_PROP_NAME, password); 
            connParams.put(TransportConstants.KEYSTORE_PATH_PROP_NAME, keystorePath);
            connParams.put(TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME, password); 
            connParams.put(TransportConstants.TRUSTSTORE_PATH_PROP_NAME, keystorePath);
        }
        factory = new HornetQJMSConnectionFactory(false, 
                new TransportConfiguration(NettyConnectorFactory.class.getName(), connParams));
        return factory;
    }
    
    private JaxbCommandsResponse sendJmsJaxbCommandsRequest(String sendQueueName, JaxbCommandsRequest req, String USER,
            String PASSWORD) throws Exception {
        ConnectionFactory factory = getConnectionFactory(useSsl);
        
        Queue jbpmQueue = (Queue) remoteInitialContext.lookup(sendQueueName);
        Queue responseQueue = (Queue) remoteInitialContext.lookup(RESPONSE_QUEUE_NAME);

        Connection connection = null;
        Session session = null;
        JaxbCommandsResponse cmdResponse = null;
        try {
            // setup
            connection = factory.createConnection(USER, PASSWORD);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageProducer producer = session.createProducer(jbpmQueue);
            String corrId = UUID.randomUUID().toString();
            String selector = "JMSCorrelationID = '" + corrId + "'";
            MessageConsumer consumer = session.createConsumer(responseQueue, selector);

            connection.start();

            // Create msg
            BytesMessage msg = session.createBytesMessage();
            msg.setJMSCorrelationID(corrId);
            msg.setIntProperty("serialization", JaxbSerializationProvider.JMS_SERIALIZATION_TYPE);
            msg.setStringProperty("username", MARY_USER);
            msg.setStringProperty("password", MARY_PASSWORD);
            String xmlStr = jaxbSerializationProvider.serialize(req);
            msg.writeUTF(xmlStr);

            // send
            producer.send(msg);

            // receive
            Message response = consumer.receive(QUALITY_OF_SERVICE_THRESHOLD_MS);

            // check
            assertNotNull("Response is empty.", response);
            assertEquals("Correlation id not equal to request msg id.", corrId, response.getJMSCorrelationID());
            assertNotNull("Response from MDB was null!", response);
            xmlStr = ((BytesMessage) response).readUTF();
            cmdResponse = (JaxbCommandsResponse) jaxbSerializationProvider.deserialize(xmlStr);
            assertNotNull("Jaxb Cmd Response was null!", cmdResponse);
        } finally {
            if (connection != null) {
                connection.close();
                if( session != null ) { 
                    session.close();
                }
            }
        }
        return cmdResponse;
    }
}
