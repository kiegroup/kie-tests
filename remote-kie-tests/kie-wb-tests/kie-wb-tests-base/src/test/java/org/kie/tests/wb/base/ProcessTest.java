package org.kie.tests.wb.base;

import static org.kie.tests.wb.base.methods.AbstractIntegrationTestMethods.runRuleTaskProcess;
import static org.kie.tests.wb.base.methods.TestConstants.GROUP_ASSSIGN_VAR_PROCESS_ID;
import static org.kie.tests.wb.base.methods.TestConstants.TASK_CONTENT_PROCESS_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.logging.Logger;

import org.kie.tests.wb.base.methods.JmsIntegrationTestMethods;
import org.kie.tests.wb.base.methods.RestIntegrationTestMethods;
import org.kie.tests.wb.base.methods.TestConstants;
import org.kie.tests.wb.base.test.objects.MyType;

public class ProcessTest extends JbpmJUnitBaseTestCase {

    protected static final Logger logger = LoggerFactory.getLogger(ProcessTest.class);

    public ProcessTest() {
        super(true, true, "org.jbpm.domain");
    }

    @Test
    public void runRuleTaskProcessTest() throws Exception {
        // setup
        Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
        resources.put("repo/test/ruleTask.bpmn2", ResourceType.BPMN2);
        resources.put("repo/test/ruleTask.drl", ResourceType.DRL);
        RuntimeManager runtimeManager = createRuntimeManager(resources);
        KieSession ksession = runtimeManager.getRuntimeEngine(null).getKieSession();

        // test
        runRuleTaskProcess(ksession, new CommandBasedAuditLogService(ksession));
    }

    @Test
    public void runUserTaskContentProcessTest() throws Exception {
        // setup
        Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
        resources.put("repo/test/userTask.bpmn2", ResourceType.BPMN2);
        RuntimeManager runtimeManager = createRuntimeManager(resources);

        RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(null);
        KieSession ksession = runtimeEngine.getKieSession();
        TaskService taskService = runtimeEngine.getTaskService();

        // test
        ProcessInstance procInst = ksession.startProcess(TASK_CONTENT_PROCESS_ID);
        long procInstId = procInst.getId();

        List<Long> taskIdList = taskService.getTasksByProcessInstanceId(procInstId);
        assertEquals(taskIdList.size(), 1);
        long taskId = taskIdList.get(0);

        Task task = taskService.getTaskById(taskId);
        TaskData taskData = task.getTaskData();
        long contentId = taskData.getDocumentContentId();
        Content content = taskService.getContentById(task.getTaskData().getDocumentContentId());
        assertNotNull("No content found!", content);
        assertTrue("Content is empty!", content.getContent().length > 0);
        Map<String, Object> contMap = (Map) ContentMarshallerHelper.unmarshall(content.getContent(), null);

        assertEquals("reviewer", contMap.get("GroupId"));
        assertEquals(3, contMap.keySet().size());
    }

    @Test
    public void runObjectParamProcessTest() throws Exception {
        // setup
        Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
        resources.put("repo/test/objectVariableProcess.bpmn2", ResourceType.BPMN2);
        RuntimeManager runtimeManager = createRuntimeManager(resources);

        RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(null);
        KieSession ksession = runtimeEngine.getKieSession();
        TestWih testWih = new TestWih();
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", testWih);

        Map<String, Object> params = new HashMap<String, Object>();
        String varId = "myobject";
        params.put(varId, new MyType("test", 10));
        ProcessInstance procInst = ksession.startProcess(TestConstants.OBJECT_VARIABLE_PROCESS_ID, params);
        long processInstanceId = procInst.getId();

        String varName = "type";
        Map<String, Object> varMap = ((WorkflowProcessInstanceImpl) procInst).getVariables();
        assertNotNull("Null variable instance found.", varMap);
        for (Entry<String, Object> entry : varMap.entrySet()) {
            logger.debug(entry.getKey() + " (" + entry.getValue().getClass().getSimpleName() + ") " + entry.getValue());
        }

        List<VariableInstanceLog> varLogs = new JPAAuditLogService(getEmf()).findVariableInstancesByName(varId, false);
        assertTrue(varLogs.size() > 0);
        assertEquals(varId, varLogs.get(0).getVariableId());

        ksession.getWorkItemManager().completeWorkItem(testWih.workItemList.poll().getId(), null);
        procInst = ksession.getProcessInstance(processInstanceId);
        assertNull(procInst);
    }

    private static class TestWih implements WorkItemHandler {

        Queue<WorkItem> workItemList = new LinkedList<WorkItem>();

        @Override
        public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
            workItemList.add(workItem);
        }

        @Override
        public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
            throw new IllegalStateException("Not allowed to abort workitem in test (workitem " + workItem.getName() + ")");
        }

    }

    @Test
    public void runHumanTaskProcessTest() throws Exception {
        // setup
        Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
        resources.put("repo/test/humanTask.bpmn2", ResourceType.BPMN2);
        RuntimeManager runtimeManager = createRuntimeManager(resources);

        RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(null);
        KieSession ksession = runtimeEngine.getKieSession();
        TaskService taskService = runtimeEngine.getTaskService();

        // test
        ProcessInstance procInst = ksession.startProcess(TestConstants.HUMAN_TASK_PROCESS_ID);
        long procInstId = procInst.getId();

        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Ready);
        List<TaskSummary> taskSumList = taskService.getTasksByStatusByProcessInstanceId(procInstId, statuses, "en-UK");
        assertEquals("No tasks found for proc inst " + procInstId + " and status " + Status.Ready, taskSumList.size(), 1);
        TaskSummary taskSum = taskSumList.get(0);

        long taskId = taskSum.getId();
        assertNull(taskSum.getActualOwner());
    }

    @Test
    public void runHumanTaskGroupIdTest() throws Exception {
        Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
        resources.put("repo/test/evaluation.bpmn2", ResourceType.BPMN2);
        RuntimeManager runtimeManager = createRuntimeManager(resources);

        RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(null);
        
        JmsIntegrationTestMethods jmsTests = new JmsIntegrationTestMethods("blah", false, false);
        jmsTests.runHumanTaskGroupIdTest(runtimeEngine, runtimeEngine, runtimeEngine);
    }
    
    @Test
    public void runGroupAssignmentEngineeringTest() throws Exception {
        // setup
        Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
        resources.put("repo/test/groupAssignmentHumanTask.bpmn2", ResourceType.BPMN2);
        RuntimeManager runtimeManager = createRuntimeManager(resources);

        RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(null);

        JmsIntegrationTestMethods jmsTests = new JmsIntegrationTestMethods("blah", false, false);
        jmsTests.remoteApiGroupAssignmentEngineeringTest(runtimeEngine);
    }
    
    @Test
    public void runGroupAssignmentHumanTaskTest() throws Exception { 
        // setup
        Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
        resources.put("repo/test/singleHumanTaskGroupAssignment.bpmn2", ResourceType.BPMN2);
        RuntimeManager runtimeManager = createRuntimeManager(resources);

        RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(null);

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("certifiate", "test");
        ProcessInstance pi = runtimeEngine.getKieSession().startProcess(GROUP_ASSSIGN_VAR_PROCESS_ID, params);
        assertNotNull( "No ProcessInstance!", pi);
        long procInstId = pi.getId();
        
        List<Long> taskIds = runtimeEngine.getTaskService().getTasksByProcessInstanceId(procInstId);
        assertEquals( 1, taskIds.size());
    }
    
    @Test
    public void runHumanTaskWithOwnTypeTest() throws Exception { 
        // setup
        Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
        resources.put("repo/test/humanTaskWithOwnType.bpmn2", ResourceType.BPMN2);
        RuntimeManager runtimeManager = createRuntimeManager(resources);

        RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(null);

        new RestIntegrationTestMethods(null).runremoteApiHumanTaskOwnTypeTest(runtimeEngine);
    }
}