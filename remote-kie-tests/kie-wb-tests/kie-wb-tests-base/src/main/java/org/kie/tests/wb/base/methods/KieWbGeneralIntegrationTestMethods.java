package org.kie.tests.wb.base.methods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.logger;
import static org.kie.tests.wb.base.methods.KieWbGeneralIntegrationTestMethods.testParamSerialization;
import static org.kie.tests.wb.base.util.TestConstants.EVALUTAION_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.GROUP_ASSSIGNMENT_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.GROUP_ASSSIGN_VAR_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.HUMAN_TASK_OWN_TYPE_ID;
import static org.kie.tests.wb.base.util.TestConstants.HUMAN_TASK_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.JOHN_USER;
import static org.kie.tests.wb.base.util.TestConstants.KRIS_USER;
import static org.kie.tests.wb.base.util.TestConstants.MARY_USER;
import static org.kie.tests.wb.base.util.TestConstants.OBJECT_VARIABLE_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.SCRIPT_TASK_VAR_PROCESS_ID;

import java.lang.reflect.Field;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import org.drools.core.xml.jaxb.util.JaxbUnknownAdapter;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.audit.AuditService;
import org.kie.api.runtime.manager.audit.VariableInstanceLog;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.process.CorrelationAwareProcessRuntime;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.task.api.InternalTaskService;
import org.kie.remote.client.api.RemoteApiResponse;
import org.kie.remote.client.api.RemoteTaskService;
import org.kie.remote.client.api.RemoteApiResponse.RemoteOperationStatus;
import org.kie.remote.jaxb.gen.GetProcessIdsCommand;
import org.kie.services.client.api.command.RemoteRuntimeEngine;
import org.kie.services.client.serialization.jaxb.impl.runtime.JaxbCorrelationKeyFactory;
import org.kie.tests.MyType;
import org.kie.tests.Person;
import org.kie.tests.Request;
import org.kie.tests.wb.base.util.TestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbGeneralIntegrationTestMethods {

    protected static Logger logger = LoggerFactory.getLogger(KieWbGeneralIntegrationTestMethods.class);
   
    protected final static int MAX_TRIES = 5;
    
    public static long findTaskIdByProcessInstanceId(Long procInstId, List<TaskSummary> taskSumList) { 
        long taskId = -1;
        TaskSummary task = findTaskSummaryByProcessInstanceId(procInstId, taskSumList);
        if( task != null ) { 
            taskId = task.getId();
        }
        assertNotEquals("Could not determine taskId!", -1, taskId);
        return taskId;
    }
    
    public static TaskSummary findTaskSummaryByProcessInstanceId(Long procInstId, List<TaskSummary> taskSumList) { 
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
 
    public static void testClassSerialization(RuntimeEngine engine, IntegrationTestMethods testMethod) { 
        
        /**
         * MyType
         */
        MyType myType = new MyType("variable", 29);
        
        /**
         * List
         */
        List<String> myList = new ArrayList<String>(2);
        myList.add("a");
        myList.add("b");
       
        /**
         * Float
         */
        Float myFloat = new Float(23.01);
       
        /**
         * Float []
         */
        Float [] floatArr = new Float [] { 39.231f };
        
        /**
         * Set<primitives>
         */
        Set<String> simpleSet = new HashSet<String>();
        simpleSet.add("one");
        simpleSet.add("two");
        simpleSet.add("fool");
        
        /**
         * Set<primitives>
         */
        Set<MyType> typeSet = new HashSet<MyType>();
        typeSet.add(new MyType("one", 1));
        typeSet.add(new MyType("two", 2));
        typeSet.add(new MyType("fool", 44));
        
        /**
         * Map<primitive, primitive>
         */
        Map<String, Long> primMap = new HashMap<String, Long>();
        primMap.put("twentetroitio", 23l);
        primMap.put("centoichi", 101l);
        
        /**
         * Map<MyType, MyType>
         */
        Map<MyType, MyType> typeMap = new HashMap<MyType, MyType>();
        typeMap.put(new MyType("key", 1), new MyType("value", 100));
        typeMap.put(new MyType("keys", 100), new MyType("value", 100));
        
        /**
         * Map<MyType, Set<MyType>>
         */
        Map<MyType, Set<MyType>> typeSetMap = new HashMap<MyType, Set<MyType>>();
        typeSetMap.put(new MyType("keys", 2), new HashSet(typeMap.values()));
        typeSetMap.put(new MyType("values", 2), new HashSet(typeMap.keySet()));
        
        
        Object [][] inputs = { 
                { MyType.class.getSimpleName(), myType },
                { "List<String>", myList },
                { "Float", myFloat },
                { "Float []", floatArr },
                { "Set<String>", simpleSet },
                { "Set<MyType>", typeSet },
                { "Map<String, Long>", primMap },
                { "Map<MyType, MyType>", typeMap },
                { "Map<MyType, Set<MyType>>", typeSetMap }
        };
        
        for( Object [] obj : inputs ) { 
            logger.debug("Testing with an instance of [" + obj[0].toString() + "]");
            testMethod.implSpecificTestParamSerialization(engine, obj[1]);
        }
    }
    
    public static final String PARAM_SERIALIZATION_PARAM_NAME = "myobject";
    
    public static long testParamSerialization(RuntimeEngine  engine, Object param) { 
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(PARAM_SERIALIZATION_PARAM_NAME, param);
        KieSession ksession = engine.getKieSession();
        logger.info("Sending start-process-request");
        ProcessInstance procInst = ksession.startProcess(OBJECT_VARIABLE_PROCESS_ID, parameters);
        assertNotNull( "No process instance returned!", procInst);
        long procInstId = procInst.getId();
        
        /**
         * Check that MyType was correctly deserialized on server side
         */
        List<VariableInstanceLog> varLogList 
            = (List<VariableInstanceLog>) engine.getAuditService().findVariableInstancesByName("type", false);
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
        varLogList = (List<org.kie.api.runtime.manager.audit.VariableInstanceLog>) engine.getAuditService().findVariableInstances(procInstId, "type");
        assertNotNull("No variable log list retrieved!", varLogList);
        assertTrue("Variable log list is empty!", varLogList.size() > 0);
        
        return procInstId;
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
            TaskSummary task = findTaskSummaryByProcessInstanceId(procInstId, tasks);
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
            TaskSummary task = findTaskSummaryByProcessInstanceId(procInstId, tasks);
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
            TaskSummary task = findTaskSummaryByProcessInstanceId(procInstId, tasks);
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

    public static void runRemoteApiGroupAssignmentEngineeringTest(RuntimeEngine maryRuntime, RuntimeEngine johnRuntime) throws Exception {
        KieSession ksession = maryRuntime.getKieSession();
        TaskService taskService = maryRuntime.getTaskService();
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
        
        TaskService johnTaskService = johnRuntime.getTaskService();
        johnTaskService.claim(taskSummary.getId(), JOHN_USER);
        johnTaskService.start(taskSummary.getId(), JOHN_USER);
        johnTaskService.complete(taskSummary.getId(), JOHN_USER, null);
        
        // now make sure that the next task has been assigned to the
        // correct person. it should be john.
        taskSummary = getTaskSummary(taskService, pi.getId(), Status.Reserved);
        assertEquals("Task 3", taskSummary.getName());
        assertEquals(JOHN_USER, taskSummary.getActualOwner().getId());
        
        // complete 'Task 3' as john
        johnTaskService.start(taskSummary.getId(), JOHN_USER);
        johnTaskService.complete(taskSummary.getId(), JOHN_USER, null);
        
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
    
    public static void runHumanTaskGroupVarAssignTest( RuntimeEngine runtimeEngine, String user, String group ) { 
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("taskOwnerGroup", "HR");
        params.put("taskName", "Mary's Task");
        ProcessInstance pi = runtimeEngine.getKieSession().startProcess(GROUP_ASSSIGN_VAR_PROCESS_ID, params);
        assertNotNull("No ProcessInstance!", pi);
        long procInstId = pi.getId();

        List<Long> taskIds = runtimeEngine.getTaskService().getTasksByProcessInstanceId(procInstId);
        assertEquals(1, taskIds.size());

        List<String> processIds = (List<String>) runtimeEngine.getKieSession().execute(new GetProcessIdsCommand());
        assertTrue("No process ids returned.", !processIds.isEmpty() && processIds.size() > 5);
        
        TaskService taskService = runtimeEngine.getTaskService();
        long taskId = taskIds.get(0);
        taskService.claim(taskIds.get(0), user);
    }

    private static Random random = new Random();
    
    public static void runRemoteApiHumanTaskOwnTypeTest( RuntimeEngine runtimeEngine, AuditService auditLogService ) {
        MyType myType = new MyType("wacky", 123);
    
        ProcessInstance pi = runtimeEngine.getKieSession().startProcess(HUMAN_TASK_OWN_TYPE_ID);
        assertNotNull(pi);
        assertEquals(ProcessInstance.STATE_ACTIVE, pi.getState());
    
        TaskService taskService = runtimeEngine.getTaskService();
        List<Long> taskIds = taskService.getTasksByProcessInstanceId(pi.getId());
        assertFalse(taskIds.isEmpty());
        long taskId = taskIds.get(0);
    
        taskService.start(taskId, JOHN_USER);
 
        Map<String, Object> contentMap = new HashMap<String, Object>(3);
        contentMap.put("one",  UUID.randomUUID().toString());
        contentMap.put("two",  new Integer(random.nextInt(1024)));
        contentMap.put("thr",  new MyType("thr", 3));
        
        // BPMSPL-119 - add content
        if( runtimeEngine instanceof RemoteRuntimeEngine ) { 
            RemoteTaskService remoteTaskService = ((RemoteRuntimeEngine) runtimeEngine).getRemoteTaskService();
            RemoteApiResponse<Long> resp = remoteTaskService.addOutputContent(taskId, contentMap);
            assertEquals( "Add Output Content operation: " + resp.getStatusDetails(),
                    RemoteOperationStatus.SUCCESS, resp.getStatus());
            assertTrue( "Empty content id", resp.getResult() != null && resp.getResult() > 0 );
        } else { 
           ((InternalTaskService) taskService).addOutputContentFromUser(taskId, JOHN_USER, contentMap);
        }

        Map<String, Object> retrievedMap;
        if( runtimeEngine instanceof RemoteRuntimeEngine ) { 
            RemoteTaskService remoteTaskService = ((RemoteRuntimeEngine) runtimeEngine).getRemoteTaskService();
            RemoteApiResponse<Map<String, Object>> mapResp = remoteTaskService.getOutputContentMap(taskId);
            assertEquals( "Get Output Content Map operations: " + mapResp.getStatusDetails(), 
                    RemoteOperationStatus.SUCCESS, mapResp.getStatus());
            retrievedMap = mapResp.getResult();
        } else { 
           retrievedMap = ((InternalTaskService) taskService).getOutputContentMapForUser(taskId, JOHN_USER);
        }
        assertNotNull( "Result Map<String, Object> is null!", retrievedMap);
        assertEquals( "Retrieved Map<String, Object> size", contentMap.size(), retrievedMap.size());

        boolean myTypeRetrieved = false;
        for( Entry<String, Object> entry : contentMap.entrySet()  ) { 
            String key = entry.getKey();
            Object val = entry.getValue();
            if( val instanceof MyType ) { 
                MyType origType = (MyType) val;
                MyType copyType =  (MyType) retrievedMap.get(key);
                assertNotNull( "Entry: " + key, copyType );
                assertEquals( "Entry: " + key, origType.getData(), copyType.getData());
                assertEquals( "Entry: " + key, origType.getText(), copyType.getText());
                myTypeRetrieved = true;
            } else { 
                assertEquals( "Entry: " + key, val, retrievedMap.get(key));
            }
        }
        assertTrue( "Custom user object ('MyType') was not retrieved!", myTypeRetrieved );
        
        // the rest of this test..
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("outMyObject", myType);
        taskService.complete(taskId, JOHN_USER, data);
    
        Task task = taskService.getTaskById(taskId);
        assertEquals(Status.Completed, task.getTaskData().getStatus());
    
        List<VariableInstanceLog> vill = (List<VariableInstanceLog>) auditLogService.findVariableInstances(pi.getId(), "myObject");
        assertNotNull(vill);
        assertFalse("Empty list of variable instance logs", vill.isEmpty());
        assertEquals(myType.toString(), vill.get(0).getValue());
    }

    public static void runRemoteApiFunnyCharactersTest(RuntimeEngine runtimeEngine) throws Exception { 
        KieSession ksession = runtimeEngine.getKieSession();
    
        // verify that property is set on client side
        Field field = JaxbUnknownAdapter.class.getDeclaredField("ENCODE_STRINGS");
        field.setAccessible(true);
        Object fieldObj = field.get(null);
        assertTrue( "ENCODE_STRINGS field is a " + fieldObj.getClass().getName(), fieldObj instanceof Boolean );
        Boolean encodeStringsBoolean = (Boolean) fieldObj;
        assertTrue( "ENCODE_STRINGS is '" + encodeStringsBoolean, encodeStringsBoolean );
        
        String [] vals = { 
            "a long string containing spaces and other characters +ěš@#$%^*()_{}\\/.,",
            "Ampersand in the string &.",
            "\"quoted string\""
        };
        long [] procInstIds = new long[vals.length];
        for( int i = 0; i < vals.length; ++i ) { 
            procInstIds[i] = startScriptTaskVarProcess(ksession, vals[i]);
        }
    
        for( int i = 0; i < vals.length; ++i ) { 
            List<? extends VariableInstanceLog> varLogs = runtimeEngine.getAuditService().findVariableInstances(procInstIds[i]);
            for( VariableInstanceLog log : varLogs ) { 
               System.out.println( log.getVariableInstanceId() + ":"  + log.getVariableId() + ":["  + log.getValue() + "]" ); 
            }
        }
    }
    
    public static long startScriptTaskVarProcess(KieSession ksession, String val) { 
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "x", val );
        ProcessInstance procInst = ksession.startProcess(SCRIPT_TASK_VAR_PROCESS_ID, map);
        return procInst.getId();
    }
    
    public static void runRemoteApiCorrelationKeyTest( URL deploymentUrl, String user, String password, IntegrationTestMethods testMethods ) {
        RuntimeEngine runtimeEngine = testMethods.getRemoteRuntimeEngine(deploymentUrl, user, password);
        KieSession kieSession = runtimeEngine.getKieSession();

        // start process
        String businessKey = "taxes";
        CorrelationKey corrKey = JaxbCorrelationKeyFactory.getInstance().newCorrelationKey(businessKey);
        ProcessInstance procInst = ((CorrelationAwareProcessRuntime) kieSession).startProcess(HUMAN_TASK_PROCESS_ID, corrKey, null);
        assertNotNull( "Could not start process instance by correlation key!", procInst );
        long procInstId = procInst.getId();
        
        procInst = ((CorrelationAwareProcessRuntime) kieSession).getProcessInstance(corrKey);
        assertNotNull( "Could not get process instance by correlation key!", procInst );
        assertEquals( "Incorrect process instance retrieved", procInstId, procInst.getId());
        
        runtimeEngine = testMethods.getCorrelationPropertiesRemoteEngine(deploymentUrl, businessKey, user, password, businessKey);

        runtimeEngine.getKieSession().abortProcessInstance(procInstId);
    }
    
    public static void runRemoteApiProcessInstances(RuntimeEngine engine) { 
        KieSession ksession = engine.getKieSession();
        
        // start process
        ProcessInstance procInst = ksession.startProcess(HUMAN_TASK_PROCESS_ID);
    
        // retrieve process instance
        ProcessInstance retrievedProcessInstance = ksession.getProcessInstance(procInst.getId());
        assertNotNull("No process instance retrieved!", retrievedProcessInstance);
        
        // verify that there are proces instances
        Collection<ProcessInstance> processInstances = ksession.getProcessInstances();
        assertNotNull("Null process instance list!", processInstances);
        assertFalse("Empty process instance list!", processInstances.isEmpty());
    }
   
    public static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    
    public static void runRemoteApiHumanTaskCommentTest(RuntimeEngine runtimeEngine) { 
        // start process
        ProcessInstance pi = runtimeEngine.getKieSession().startProcess(HUMAN_TASK_OWN_TYPE_ID);
        assertNotNull(pi);
        assertEquals(ProcessInstance.STATE_ACTIVE, pi.getState());
   
        // get task
        TaskService taskService = runtimeEngine.getTaskService();
        List<Long> taskIds = taskService.getTasksByProcessInstanceId(pi.getId());
        assertFalse(taskIds.isEmpty());
        long taskId = taskIds.get(0);
    
        taskService.start(taskId, JOHN_USER);
  
        // test add comment
        String commentText = UUID.randomUUID().toString();
        String commentUser = MARY_USER;
        Long taskCommentId = taskService.addComment(taskId, commentUser, commentText);
        assertNotNull("Null task comment id!", taskCommentId);
        
        // test get comment
        org.kie.api.task.model.Comment comment = taskService.getCommentById(taskCommentId);
        assertEquals("Comment user", commentUser, comment.getAddedBy().getId());
        assertEquals("Comment text", commentText, comment.getText());
        Date commentDate = comment.getAddedAt();
        GregorianCalendar fiveMinAgoCal = new GregorianCalendar();
        fiveMinAgoCal.add(Calendar.MINUTE, -5);
        Date fiveMinAgo = fiveMinAgoCal.getTime();
        assertTrue( "Comment date: " + sdf.format(commentDate) + " [" + sdf.format(fiveMinAgo) + "]", fiveMinAgo.before(commentDate));

        // test get all comments
        String anotherCommentText = UUID.randomUUID().toString();
        String anotherCommentUser = KRIS_USER;
        Long anotherTaskCommentId = taskService.addComment(taskId, anotherCommentUser, anotherCommentText);

        assertNotNull("Null task comment id!", taskCommentId);
       
        List<org.kie.api.task.model.Comment> commentList = taskService.getAllCommentsByTaskId(taskId);
        for( org.kie.api.task.model.Comment kieComment : commentList ) { 
           if( kieComment.getId() == anotherTaskCommentId ) { 
               assertEquals("(Another) Comment user", anotherCommentUser, kieComment.getAddedBy().getId());
               assertEquals("(Another) Comment text", anotherCommentText, kieComment.getText());
           } else if( kieComment.getId() == taskCommentId ) { 
               assertEquals("Comment user", commentUser, kieComment.getAddedBy().getId());
               assertEquals("Comment text", commentText, kieComment.getText());
           } else { 
               fail( "Retrieved unknown comment for task! [task: " + taskId + "/comment: " + kieComment.getId() + "]" );
           }
        }
        int origCommentListSize = commentList.size();
        
        // test delete comments
        taskService.deleteComment(taskId, taskCommentId);
        commentList = taskService.getAllCommentsByTaskId(taskId);
        assertEquals( "Delete comment did not succeed", origCommentListSize-1, commentList.size());
        for( org.kie.api.task.model.Comment kieComment : commentList ) { 
           assertNotEquals("Deleted comment found", taskCommentId, kieComment.getId());
        }
    }

    public static void runRemoteApiHistoryVariablesTest(RuntimeEngine runtimeEngine) { 
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("myobject", 10l);
        runtimeEngine.getKieSession().startProcess(OBJECT_VARIABLE_PROCESS_ID, params);
        
        List<VariableInstanceLog> viLogs = (List<VariableInstanceLog>) runtimeEngine.getAuditService().findVariableInstancesByName("myobject", false);
        assertNotNull( "Null variable instance log list", viLogs);
        logger.info("vi logs: " + viLogs.size());
        assertTrue( "Variable instance log list is empty", ! viLogs.isEmpty() );
    }
}
