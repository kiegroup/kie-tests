package org.kie.tests.wb.live;

import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.runRemoteApiGroupAssignmentEngineeringTest;
import static org.kie.tests.wb.base.util.TestConstants.JOHN_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.JOHN_USER;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;

import java.net.URL;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;

import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.remote.client.api.RemoteJmsRuntimeEngineBuilder;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.tests.wb.base.methods.KieWbWebServicesIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore // add Junit "Ping Succeed or Ignore" rule
public class JmsGroupLiveTest {

   protected static final Logger logger = LoggerFactory.getLogger(JmsGroupLiveTest.class);
    
    private static boolean weblogic = false;
    
    private URL deploymentUrl;
    {
        // Modify this string to match your kie-wb/BPMS installation
        String urlString;
        if( weblogic ) { 
            urlString = "http://localhost:7001/kie-wb-6.2.1-SNAPSHOT-weblogic12/";
        } else { 
            urlString = "http://localhost:8080/kie-wb/";
        }
        try { 
            deploymentUrl = new URL(urlString);
        } catch( Exception e ) { 
            System.err.println( "The following URL is not a valid URL: '" + urlString + "'");
            e.printStackTrace();
        }
    }
  
    private final String deploymentId = KJAR_DEPLOYMENT_ID;
    private final InitialContext remoteInitialContext;
    
    public JmsGroupLiveTest() { 
         Properties initialProps = new Properties();
         if( weblogic ) {
             initialProps.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
             initialProps.setProperty(InitialContext.PROVIDER_URL, "t3://localhost:7001");
             initialProps.setProperty(InitialContext.SECURITY_PRINCIPAL, "weblogic");
             initialProps.setProperty(InitialContext.SECURITY_CREDENTIALS, "pa55w3bl0g1c");

             for (Object keyObj : initialProps.keySet()) {
                 String key = (String) keyObj;
                 System.setProperty(key, (String) initialProps.get(key));
             }
         } else { 
             initialProps.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
             initialProps.setProperty(InitialContext.PROVIDER_URL, "remote://localhost:4447");
             initialProps.setProperty(InitialContext.SECURITY_PRINCIPAL, MARY_USER);
             initialProps.setProperty(InitialContext.SECURITY_CREDENTIALS, MARY_PASSWORD);
         }
         
         try {
             remoteInitialContext = new InitialContext(initialProps);
         } catch (NamingException e) {
             throw new RuntimeException("Unable to create " + InitialContext.class.getSimpleName(), e);
         }
    }

    protected void printTestName() { 
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println( "-=> " + testName );
    }
    
    @Test
    public void restIssueTest() throws Exception { 
        printTestName();
      
        RuntimeEngine engine = RemoteRuntimeEngineFactory.newRestBuilder()
            .addDeploymentId(deploymentId)
            .addUserName(MARY_USER)
            .addPassword(MARY_PASSWORD)
            .addUrl(deploymentUrl)
            .build();
        
        RuntimeEngine johnEngine = RemoteRuntimeEngineFactory.newRestBuilder()
                .addDeploymentId(deploymentId)
                .addUserName(JOHN_USER)
                .addPassword(JOHN_PASSWORD)
                .addUrl(deploymentUrl)
                .build();
        
        runRemoteApiGroupAssignmentEngineeringTest(engine, johnEngine);
    }
    
    @Test
    public void jmsIssueTest() throws Exception { 
        RuntimeEngine engine = createRemoteJmsRuntimeEngine(MARY_USER, MARY_PASSWORD);
        RuntimeEngine johnEngine = createRemoteJmsRuntimeEngine(JOHN_USER, JOHN_PASSWORD);
        
        runRemoteApiGroupAssignmentEngineeringTest(engine, johnEngine);
    }
    
    private RuntimeEngine createRemoteJmsRuntimeEngine( String user, String password ) throws Exception { 
        RemoteJmsRuntimeEngineBuilder builder = RemoteRuntimeEngineFactory.newJmsBuilder()
                .addDeploymentId(deploymentId);
        if( weblogic ) { 
            String queueName = "jms/KIE.SESSION";
            Queue sessionQueue = (Queue) remoteInitialContext.lookup(queueName);
            queueName = "jms/KIE.TASK";
            Queue taskQueue = (Queue) remoteInitialContext.lookup(queueName);
            queueName = "jms/queue/KIE.RESPONSE.ALL";
            Queue responseQueue = (Queue) remoteInitialContext.lookup(queueName);

            String connFactoryName = "jms/cf/KIE.RESPONSE.ALL";
            ConnectionFactory connFact = (ConnectionFactory) remoteInitialContext.lookup(connFactoryName);
            
            builder
                .addConnectionFactory(connFact)
                .addKieSessionQueue(sessionQueue)
                .addTaskServiceQueue(taskQueue)
                .addResponseQueue(responseQueue);
        } else { 
            builder.addRemoteInitialContext(remoteInitialContext);
        }
        builder
            .addUserName(user)
            .addPassword(password)
            .addHostName("localhost");
        if( weblogic ) {  
            builder.addJmsConnectorPort(7001);
        }
        builder
            .useSsl(false)
            .disableTaskSecurity();
        
        return builder.build(); 
    }
    
}
