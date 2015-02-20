package org.kie.tests.wb.dummy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQJMSConnectionFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.remote.client.jaxb.ClientJaxbSerializationProvider;
import org.kie.remote.client.jaxb.JaxbCommandsRequest;
import org.kie.remote.client.jaxb.JaxbCommandsResponse;
import org.kie.services.client.serialization.JaxbSerializationProvider;

public class LiveSslConfigurationIssuesTest {

    private static final String CONNECTION_FACTORY_NAME = "jms/RemoteConnectionFactory";
    
    private static final String KSESSION_QUEUE_NAME = "jms/queue/KIE.SESSION";
    private static final String TASK_QUEUE_NAME = "jms/queue/KIE.TASK";
    private static final String RESPONSE_QUEUE_NAME = "jms/queue/KIE.RESPONSE";

    private static final long QUALITY_OF_SERVICE_THRESHOLD_MS = 5 * 1000;
            
    private static InitialContext getRemoteInitialContext( String user, String password ) {
        Properties initialProps = new Properties();
        initialProps.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        initialProps.setProperty(InitialContext.PROVIDER_URL, "remote://localhost:4447");
        initialProps.setProperty(InitialContext.SECURITY_PRINCIPAL, user);
        initialProps.setProperty(InitialContext.SECURITY_CREDENTIALS, password);

        for( Object keyObj : initialProps.keySet() ) {
            String key = (String) keyObj;
            System.setProperty(key, (String) initialProps.get(key));
        }
        try {
            return new InitialContext(initialProps);
        } catch( NamingException e ) {
            throw new RuntimeException("Unable to create " + InitialContext.class.getSimpleName(), e);
        }
    }

    @Test
    public void fixSsl() throws Exception {
        InitialContext remoteInitialContext = getRemoteInitialContext(MARY_USER, MARY_PASSWORD);

        int port = 5445;
        boolean useSsl = true;
        if( useSsl ) { 
            port = 5446;
        }
        
        ConnectionFactory factory;
        Map<String, Object> connParams = new HashMap<String, Object>();
        connParams.put(TransportConstants.PORT_PROP_NAME, 5446);
        connParams.put(TransportConstants.HOST_PROP_NAME, "localhost");
        // SSL
        connParams.put(TransportConstants.SSL_ENABLED_PROP_NAME, true);
        connParams.put(TransportConstants.KEYSTORE_PASSWORD_PROP_NAME, "CLIENT_KEYSTORE_PASSWORD");
        URL keyStoreUrl = this.getClass().getResource("/ssl/client_keystore.jks");
        assertNotNull("Null keystore url", keyStoreUrl);
        File keyStoreFile = new File(keyStoreUrl.toURI());
        assertNotNull("Keystore file does not exist", keyStoreFile.exists() );
        connParams.put(TransportConstants.KEYSTORE_PATH_PROP_NAME, keyStoreFile.getAbsolutePath() );

        factory = new HornetQJMSConnectionFactory(false, new TransportConfiguration(NettyConnectorFactory.class.getName(),
                connParams));
        Queue jbpmQueue = (Queue) remoteInitialContext.lookup(KSESSION_QUEUE_NAME);
        Queue responseQueue = (Queue) remoteInitialContext.lookup(RESPONSE_QUEUE_NAME);

        Connection connection = null;
        Session session = null;
        JaxbCommandsResponse cmdResponse = null;
        try {
            // setup
            connection = factory.createConnection(MARY_USER, MARY_PASSWORD);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageProducer producer = session.createProducer(jbpmQueue);
            String corrId = UUID.randomUUID().toString();
            String selector = "JMSCorrelationID = '" + corrId + "'";
            MessageConsumer consumer = session.createConsumer(responseQueue, selector);

            connection.start();

            JaxbCommandsRequest req = new JaxbCommandsRequest();
            JaxbSerializationProvider jaxbSerializationProvider = ClientJaxbSerializationProvider.newInstance();
            
            // Create msg
            String xmlStr = jaxbSerializationProvider.serialize(req);
            TextMessage msg = session.createTextMessage(xmlStr);
            msg.setJMSCorrelationID(corrId);
            msg.setIntProperty("serialization", JaxbSerializationProvider.JMS_SERIALIZATION_TYPE);
            msg.setStringProperty("username", MARY_USER);
            msg.setStringProperty("password", MARY_PASSWORD);

            // send
            producer.send(msg);

            // receive
            Message response = consumer.receive(QUALITY_OF_SERVICE_THRESHOLD_MS);

            // check
            assertNotNull("Response is empty.", response);
            assertEquals("Correlation id not equal to request msg id.", corrId, response.getJMSCorrelationID());
            assertNotNull("Response from MDB was null!", response);
            xmlStr = ((TextMessage) response).getText();
            cmdResponse = (JaxbCommandsResponse) jaxbSerializationProvider.deserialize(xmlStr);
            assertNotNull("Jaxb Cmd Response was null!", cmdResponse);
        } finally {
            if( connection != null ) {
                connection.close();
                if( session != null ) {
                    session.close();
                }
            }
        }
    }
}
