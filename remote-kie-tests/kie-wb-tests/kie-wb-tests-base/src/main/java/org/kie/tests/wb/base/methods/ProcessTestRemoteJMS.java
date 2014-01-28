package org.kie.tests.wb.base.methods;

import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.services.client.api.RemoteJmsRuntimeEngineFactory;

public class ProcessTestRemoteJMS {
    
    private static final String password = "XXXXXXXXXX";
 
    @Test
    public void testProcess() throws Exception {
         
        InitialContext remoteInitialContext
            = getRemoteInitialContext("localhost", "krisv", password);
        RemoteJmsRuntimeEngineFactory restSessionFactory = new RemoteJmsRuntimeEngineFactory(
            "org.jbpm:Evaluation:1.0", remoteInitialContext, "krisv", password);
 
        RuntimeEngine engine = restSessionFactory.newRuntimeEngine();
         
        KieSession ksession = engine.getKieSession();
        TaskService taskService = engine.getTaskService();
         
        // start a new process instance
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("employee", "krisv");
        params.put("reason", "Yearly performance evaluation");
        ProcessInstance processInstance = 
            ksession.startProcess("evaluation", params);
        System.out.println("Process started ...");
         
        // complete Self Evaluation
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("krisv", "en-UK");
        assertEquals(1, tasks.size());
        TaskSummary task = tasks.get(0);
        System.out.println("'krisv' completing task " + task.getName() + ": " + task.getDescription());
        taskService.start(task.getId(), "krisv");
        Map<String, Object> results = new HashMap<String, Object>();
        results.put("performance", "exceeding");
        taskService.complete(task.getId(), "krisv", results);
         
        remoteInitialContext = getRemoteInitialContext("localhost", "john", password);
        restSessionFactory = new RemoteJmsRuntimeEngineFactory(
            "org.jbpm:Evaluation:1.0", remoteInitialContext, "john", password);
        engine = restSessionFactory.newRuntimeEngine();
        ksession = engine.getKieSession();
        taskService = engine.getTaskService();
         
        // john from HR
        tasks = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");
        assertEquals(1, tasks.size());
        task = tasks.get(0);
        System.out.println("'john' completing task " + task.getName() + ": " + task.getDescription());
        taskService.claim(task.getId(), "john");
        taskService.start(task.getId(), "john");
        results = new HashMap<String, Object>();
        results.put("performance", "acceptable");
        taskService.complete(task.getId(), "john", results);
         
        remoteInitialContext = getRemoteInitialContext("localhost", "mary", password);
        restSessionFactory = new RemoteJmsRuntimeEngineFactory(
            "org.jbpm:Evaluation:1.0", remoteInitialContext, "mary", password);
        engine = restSessionFactory.newRuntimeEngine();
        ksession = engine.getKieSession();
        taskService = engine.getTaskService();
         
        // mary from PM
        tasks = taskService.getTasksAssignedAsPotentialOwner("mary", "en-UK");
        assertEquals(1, tasks.size());
        task = tasks.get(0);
        System.out.println("'mary' completing task " + task.getName() + ": " + task.getDescription());
        taskService.claim(task.getId(), "mary");
        taskService.start(task.getId(), "mary");
        results = new HashMap<String, Object>();
        results.put("performance", "outstanding");
        taskService.complete(task.getId(), "mary", results);
         
        assertProcessInstanceCompleted(processInstance.getId(), ksession);
        System.out.println("Process instance completed");
    }
    
    public void assertProcessInstanceCompleted(long procId, KieSession ksession) { 
       ProcessInstance processInstance = ksession.getProcessInstance(procId); 
       assertTrue( "Process instance " + procId + " has not completed!",
               processInstance == null || processInstance.getState() == ProcessInstance.STATE_COMPLETED );
    }
     
    private static InitialContext getRemoteInitialContext(String jbossServerHostName, String user, String password) {
        // Configure the (JBoss AS 7/EAP 6) InitialContextFactory
        Properties initialProps = new Properties();
        initialProps.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY,
                "org.jboss.naming.remote.client.InitialContextFactory");
        initialProps.setProperty(InitialContext.PROVIDER_URL, "remote://"
                + jbossServerHostName + ":4447");
        initialProps.setProperty(InitialContext.SECURITY_PRINCIPAL, user);
        initialProps.setProperty(InitialContext.SECURITY_CREDENTIALS, password);
 
        for (Object keyObj : initialProps.keySet()) {
            String key = (String) keyObj;
            System.setProperty(key, (String) initialProps.get(key));
        }
 
        // Create the remote InitialContext instance
        try {
            return new InitialContext(initialProps);
        } catch (NamingException e) {
            throw new RuntimeException("Unable to create " + InitialContext.class.getSimpleName(), e);
        }
    }
 
}
