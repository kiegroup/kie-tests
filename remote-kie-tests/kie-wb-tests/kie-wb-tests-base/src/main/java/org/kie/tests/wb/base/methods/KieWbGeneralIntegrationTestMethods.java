package org.kie.tests.wb.base.methods;

import static org.kie.tests.wb.base.util.TestConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kie.tests.wb.base.util.TestConstants.EVALUTAION_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.OBJECT_VARIABLE_PROCESS_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.audit.AuditService;
import org.kie.api.runtime.manager.audit.VariableInstanceLog;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.services.client.api.command.RemoteRuntimeEngine;
import org.kie.tests.MyType;
import org.kie.tests.Person;
import org.kie.tests.Request;
import org.kie.tests.wb.base.util.TestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbGeneralIntegrationTestMethods {

    protected static Logger logger = LoggerFactory.getLogger(KieWbGeneralIntegrationTestMethods.class);
   
    protected final static int MAX_TRIES = 5;
    
    public static long findTaskId(Long procInstId, List<TaskSummary> taskSumList) { 
        long taskId = -1;
        TaskSummary task = findTaskSummary(procInstId, taskSumList);
        if( task != null ) { 
            taskId = task.getId();
        }
        assertNotEquals("Could not determine taskId!", -1, taskId);
        return taskId;
    }
    
    public static TaskSummary findTaskSummary(Long procInstId, List<TaskSummary> taskSumList) { 
        for( TaskSummary task : taskSumList ) { 
            if( procInstId.equals(task.getProcessInstanceId()) ) {
                return task;
            }
        }
        fail( "Unable to find task summary for process instance " + procInstId); 
        return null;
    }

    /**
     * Shared tests
     */
    
    public static void testExtraJaxbClassSerialization(RemoteRuntimeEngine engine) {
        
        /**
         * MyType
         */
        testParamSerialization(engine, new MyType("variable", 29));
        
        /**
         * Float
         */
        testParamSerialization(engine, new Float(23.01));
        
        /**
         * Float []
         */
        testParamSerialization(engine, new Float [] { 39.391f });
    }
    

    public static void testParamSerialization(RemoteRuntimeEngine  engine, Object param) { 
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("myobject", param);
        KieSession ksession = engine.getKieSession();
        logger.info("Sending start-process-request");
        ProcessInstance procInst = ksession.startProcess(OBJECT_VARIABLE_PROCESS_ID, parameters);
        assertNotNull( "No process instance returned!", procInst);
        long procInstId = procInst.getId();
        
        /**
         * Check that MyType was correctly deserialized on server side
         */
        List<VariableInstanceLog> varLogList 
            = (List<VariableInstanceLog>) engine.getAuditLogService().findVariableInstancesByName("type", false);
        VariableInstanceLog thisProcInstVarLog = null;
        for( VariableInstanceLog varLog : varLogList ) {
            if( varLog.getProcessInstanceId() == procInstId ) { 
                thisProcInstVarLog = varLog;
            }
        }
        assertNotNull( "No VariableInstanceLog found!", thisProcInstVarLog );
        assertEquals( "type", thisProcInstVarLog.getVariableId() );
        assertEquals( "De/serialization of Kjar type did not work.", param.getClass().getName(), thisProcInstVarLog.getValue() );
        
        // Double check for BZ-1085267
        varLogList = (List<org.kie.api.runtime.manager.audit.VariableInstanceLog>) engine.getAuditLogService().findVariableInstances(procInstId, "type");
        assertNotNull("No variable log list retrieved!", varLogList);
        assertTrue("Variable log list is empty!", varLogList.size() > 0);
    }
    
    public static void runRuleTaskProcess(KieSession ksession, AuditService auditLogService) { 
        // Setup facts
        Person person = new Person("guest", "Dluhoslav Chudobny");
        person.setAge(25); // >= 18
        Request request = new Request("1");
        request.setPersonId("guest");
        request.setAmount(500); // < 1000
        
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("request", request);
        params.put("person", person);
       
        // Start process
        ProcessInstance pi = ksession.startProcess(TestConstants.RULE_TASK_PROCESS_ID, params);
        assertNotNull( "No Process instance returned!", pi);
        ksession.fireAllRules();        
        
        // Check
//        assertEquals("Poor customer", ((Request)ksession.getObject(factHandle)).getInvalidReason());
        assertNull(ksession.getProcessInstance(pi.getId()));
        
        List<VariableInstanceLog> varLogs = (List<VariableInstanceLog>) auditLogService.findVariableInstancesByName("requestReason", false);
        for( VariableInstanceLog varLog : varLogs ) { 
            if( varLog.getProcessInstanceId() == pi.getId() ) { 
                assertEquals( "Poor customer", varLog.getValue() );
            }
        }
    }
   
    public static void runHumanTaskGroupIdTest(RuntimeEngine krisRuntimeEngine, RuntimeEngine johnRuntimeEngine, RuntimeEngine maryRuntimeEngine) {
        KieSession ksession = krisRuntimeEngine.getKieSession();

        // start a new process instance
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("employee", "krisv");
        params.put("reason", "Yearly performance evaluation");
        ProcessInstance processInstance = ksession.startProcess(EVALUTAION_PROCESS_ID, params);
        assertNotNull( "Null process instance!", processInstance);
        long procInstId = processInstance.getId();
        System.out.println("Process started ...");

        // complete Self Evaluation
        {
            String user = "krisv";
            TaskService taskService = krisRuntimeEngine.getTaskService();
            List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(user, "en-UK");
            TaskSummary task = findTaskSummary(procInstId, tasks);
            assertNotNull("Unable to find " + user + "'s task", task);
            System.out.println("'" + user + "' completing task " + task.getName() + ": " + task.getDescription());
            taskService.start(task.getId(), user);
            Map<String, Object> results = new HashMap<String, Object>();
            results.put("performance", "exceeding");
            taskService.complete(task.getId(), user, results);
        }

        // john from HR
        { 
            String user = "john";
            TaskService taskService = johnRuntimeEngine.getTaskService();
            List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(user, "en-UK");
            TaskSummary task = findTaskSummary(procInstId, tasks);
            assertNotNull("Unable to find " + user + "'s task", task);
            System.out.println("'john' completing task " + task.getName() + ": " + task.getDescription());
            taskService.start(task.getId(), user);
            Map<String, Object> results = new HashMap<String, Object>();
            results.put("performance", "acceptable");
            taskService.complete(task.getId(), user, results);
        }

        // mary from PM
        {
            String user = "mary";
            TaskService taskService = maryRuntimeEngine.getTaskService();
            List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(user, "en-UK");
            TaskSummary task = findTaskSummary(procInstId, tasks);
            assertNotNull("Unable to find " + user + "'s task", task);
            System.out.println("'" + user + "' completing task " + task.getName() + ": " + task.getDescription());
            taskService.start(task.getId(), user);
            Map<String, Object> results = new HashMap<String, Object>();
            results.put("performance", "outstanding");
            taskService.complete(task.getId(), user,  results);
        }

        //  assertProcessInstanceCompleted(processInstance.getId(), ksession);
        System.out.println("Process instance completed");
    }

    public static void runRemoteApiGroupAssignmentEngineeringTest(RuntimeEngine runtimeEngine) throws Exception {
        KieSession ksession = runtimeEngine.getKieSession();
        TaskService taskService = runtimeEngine.getTaskService();
        ProcessInstance pi = ksession.startProcess(GROUP_ASSSIGNMENT_PROCESS_ID, null);
        assertNotNull(pi);
        assertEquals(ProcessInstance.STATE_ACTIVE, pi.getState());
        
        // assert the task
        TaskSummary taskSummary = getTaskSummary(taskService, pi.getId(), Status.Ready);
        assertNull(taskSummary.getActualOwner());
        assertNull(taskSummary.getPotentialOwners());
        assertEquals("Task 1", taskSummary.getName());

        // complete 'Task 1' as mary
        taskService.claim(taskSummary.getId(), MARY_USER);
        taskService.start(taskSummary.getId(), MARY_USER);
        taskService.complete(taskSummary.getId(), MARY_USER, null);
        
        // now make sure that the next task has been assigned to the
        // correct person. it should be mary.
        taskSummary = getTaskSummary(taskService, pi.getId(), Status.Reserved);
        assertNotNull( "No task found for Mary", taskSummary);
        assertEquals("Task 2", taskSummary.getName());
        assertEquals(MARY_USER, taskSummary.getActualOwner().getId());
        
        // complete 'Task 2' as john
        taskService.release(taskSummary.getId(), MARY_USER);
        taskService.claim(taskSummary.getId(), JOHN_USER);
        taskService.start(taskSummary.getId(), JOHN_USER);
        taskService.complete(taskSummary.getId(), JOHN_USER, null);
        
        // now make sure that the next task has been assigned to the
        // correct person. it should be john.
        taskSummary = getTaskSummary(taskService, pi.getId(), Status.Reserved);
        assertEquals("Task 3", taskSummary.getName());
        assertEquals(JOHN_USER, taskSummary.getActualOwner().getId());
        
        // complete 'Task 3' as john
        taskService.start(taskSummary.getId(), JOHN_USER);
        taskService.complete(taskSummary.getId(), JOHN_USER, null);
        
        // assert process finished
        pi = ksession.getProcessInstance(pi.getId());
        assertNull(pi);
    }
   
    private static TaskSummary getTaskSummary(TaskService taskService, long procInstId, Status status) {
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(status);
        List<TaskSummary> taskSumList = taskService.getTasksByStatusByProcessInstanceId(procInstId, statuses, "en-UK");
        TaskSummary result = null;
        for (TaskSummary krisTask : taskSumList) {
            if (krisTask.getProcessInstanceId() == procInstId) {
                result = krisTask;
            }
        }
        return result;
    }
}
