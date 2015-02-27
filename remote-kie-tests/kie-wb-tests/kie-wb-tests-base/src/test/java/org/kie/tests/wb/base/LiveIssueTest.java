package org.kie.tests.wb.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.findTaskId;
import static org.kie.tests.wb.base.util.TestConstants.CLIENT_KEYSTORE_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.CLIENT_KEY_TRUSTSTORE_LOCATION;
import static org.kie.tests.wb.base.util.TestConstants.HUMAN_TASK_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.base.util.TestConstants.SALA_USER;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.tests.wb.base.methods.KieWbJmsIntegrationTestMethods;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveIssueTest {

   protected static final Logger logger = LoggerFactory.getLogger(LiveIssueTest.class);
    
    private final KieWbRestIntegrationTestMethods restTests;
    private final KieWbJmsIntegrationTestMethods jmsTests;

    private URL deploymentUrl;
    {
        // Modify this string to match your kie-wb/BPMS installation
        String urlString = "http://localhost:7001/kie-wb-6.2.0-SNAPSHOT-weblogic12/";
        try { 
            deploymentUrl = new URL(urlString);
        } catch( Exception e ) { 
            System.err.println( "The following URL is not a valid URL: '" + urlString + "'");
            e.printStackTrace();
        }
    }
  
    private final String contentType = MediaType.APPLICATION_XML; 
    private final RuntimeStrategy stategy = RuntimeStrategy.SINGLETON;
    private final int timeoutInSecs = 5;
    
    private final String deploymentId = KJAR_DEPLOYMENT_ID;
   
    private final InitialContext remoteInitialContext;
    
    public LiveIssueTest() { 
         restTests = KieWbRestIntegrationTestMethods.newBuilderInstance()
                 .setDeploymentId(deploymentId)
                 .setMediaType(contentType)
                 .setStrategy(stategy)
                 .setTimeoutInSecs(timeoutInSecs)
                 .build();
         
         Properties initialProps = new Properties();
         initialProps.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
         initialProps.setProperty(InitialContext.PROVIDER_URL, "t3://localhost:7001");
         initialProps.setProperty(InitialContext.SECURITY_PRINCIPAL, "weblogic");
         initialProps.setProperty(InitialContext.SECURITY_CREDENTIALS, "pa55w3bl0g1c");
      
         for (Object keyObj : initialProps.keySet()) {
             String key = (String) keyObj;
             System.setProperty(key, (String) initialProps.get(key));
         }
         try {
              remoteInitialContext = new InitialContext(initialProps);
         } catch (NamingException e) {
             throw new RuntimeException("Unable to create " + InitialContext.class.getSimpleName(), e);
         }
    
         jmsTests = new KieWbJmsIntegrationTestMethods(deploymentId, false, initialProps);
    }

    protected void printTestName() { 
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println( "-=> " + testName );
    }
    
    @Test
    public void issueTest() throws Exception { 
        printTestName();
        
        String queueName = "jms/KIE.SESSION";
        Queue sessionQueue = (Queue) remoteInitialContext.lookup(queueName);
        queueName = "jms/KIE.TASK";
        Queue taskQueue = (Queue) remoteInitialContext.lookup(queueName);
        queueName = "jms/queue/KIE.RESPONSE.ALL";
        Queue responseQueue = (Queue) remoteInitialContext.lookup(queueName);

        String connFactoryName = "jms/cf/KIE.RESPONSE.ALL";
        ConnectionFactory connFact = (ConnectionFactory) remoteInitialContext.lookup(connFactoryName);
        
        RuntimeEngine engine = RemoteRuntimeEngineFactory.newJmsBuilder()
                .addDeploymentId(deploymentId)
                .addConnectionFactory(connFact)
                .addKieSessionQueue(sessionQueue)
                .addTaskServiceQueue(taskQueue)
                .addResponseQueue(responseQueue)
                .addUserName(MARY_USER)
                .addPassword(MARY_PASSWORD)
                .addHostName("localhost")
                .addJmsConnectorPort(7001)
                .useSsl(false)
                .disableTaskSecurity()
                .build();

        // create JMS request
        KieSession ksession = engine.getKieSession();
        ProcessInstance processInstance = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
        assertNotNull( "Null process instance!", processInstance);

        logger.debug("Started process instance: " + processInstance + " "
                + (processInstance == null ? "" : processInstance.getId()));

        TaskService taskService = engine.getTaskService();
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(SALA_USER, "en-UK");
        long taskId = findTaskId(processInstance.getId(), tasks);

        logger.debug("Found task " + taskId);
        Task task = taskService.getTaskById(taskId);
        logger.debug("Got task " + taskId + ": " + task);
        taskService.start(taskId, SALA_USER);
        taskService.complete(taskId, SALA_USER, null);

        logger.debug("Now expecting failure");
        try {
            taskService.complete(taskId, SALA_USER, null);
            fail("Should not have been able to complete task " + taskId + " a second time.");
        } catch (Throwable t) {
            // do nothing
        }

        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        List<TaskSummary> taskIds = taskService.getTasksByStatusByProcessInstanceId(processInstance.getId(), statuses, "en-UK");
        assertEquals("Expected 2 tasks.", 2, taskIds.size());
    }

}
