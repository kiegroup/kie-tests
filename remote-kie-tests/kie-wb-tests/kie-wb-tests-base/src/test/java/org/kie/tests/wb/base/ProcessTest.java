package org.kie.tests.wb.base;

import static org.kie.tests.wb.base.methods.AbstractIntegrationTestMethods.runRuleTaskProcess;
import static org.kie.tests.wb.base.methods.TestConstants.EVALUTAION_PROCESS_ID;
import static org.kie.tests.wb.base.methods.TestConstants.TASK_CONTENT_PROCESS_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.jbpm.process.audit.CommandBasedAuditLogService;
import org.jbpm.process.audit.JPAAuditLogService;
import org.jbpm.process.audit.VariableInstanceLog;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.junit.Test;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskData;
import org.kie.api.task.model.TaskSummary;
import org.kie.tests.wb.base.methods.JmsIntegrationTestMethods;
import org.kie.tests.wb.base.methods.TestConstants;
import org.kie.tests.wb.base.test.objects.MyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        // setup
        Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
        resources.put("repo/test/evaluation.bpmn2", ResourceType.BPMN2);
        RuntimeManager runtimeManager = createRuntimeManager(resources);

        RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(null);
        KieSession ksession = runtimeEngine.getKieSession();
        TaskService taskService = runtimeEngine.getTaskService();

        // start a new process instance
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("employee", "krisv");
        params.put("reason", "Yearly performance evaluation");
        ProcessInstance processInstance = ksession.startProcess(EVALUTAION_PROCESS_ID, params);
        long procInstId = processInstance.getId();

        // complete Self Evaluation
        {
            String user = "krisv";
            List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(user, "en-UK");
            TaskSummary task = getProcessInstanceTask(tasks, procInstId);
            assertNotNull("Unable to find " + user + "'s task", task);
            logger.debug("'" + user + "' completing task " + task.getName() + ": " + task.getDescription());
            taskService.start(task.getId(), user);
            Map<String, Object> results = new HashMap<String, Object>();
            results.put("performance", "exceeding");
            taskService.complete(task.getId(), user, results);
        }

        // john from HR
        {
            String user = "john";
            Environment env = ksession.getEnvironment();
            List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(user, "en-UK");
            TaskSummary task = getProcessInstanceTask(tasks, procInstId);
            assertNotNull("Unable to find " + user + "'s task", task);
            logger.debug("'john' completing task " + task.getName() + ": " + task.getDescription());
            // taskService.claim(task.getId(), user);
            taskService.start(task.getId(), user);
            Map<String, Object> results = new HashMap<String, Object>();
            results.put("performance", "acceptable");
            taskService.complete(task.getId(), user, results);
        }

        // mary from PM
        {
            String user = "mary";
            List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner(user, "en-UK");
            logger.debug(tasks.size() + " tasks retrieved for potential owner " + user);
            TaskSummary task = getProcessInstanceTask(tasks, procInstId);
            assertNotNull("Unable to find " + user + "'s task", task);
            logger.debug("'" + user + "' completing task " + task.getName() + ": " + task.getDescription());
            // taskService.claim(task.getId(), user);
            taskService.start(task.getId(), user);
            Map<String, Object> results = new HashMap<String, Object>();
            results.put("performance", "outstanding");
            taskService.complete(task.getId(), user, results);
        }

        // assertProcessInstanceCompleted(processInstance.getId(), ksession);
        logger.debug("Process instance completed");
    }

    private TaskSummary getProcessInstanceTask(List<TaskSummary> tasks, long procInstId) {
        TaskSummary result = null;
        for (TaskSummary krisTask : tasks) {
            if (krisTask.getProcessInstanceId() == procInstId) {
                result = krisTask;
            }
        }
        return result;
    }

    @Test
    public void runGroupAssignmentEngineeringTest() throws Exception {
        // setup
        Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
        resources.put("repo/test/groupAssignmentHumanTask.bpmn2", ResourceType.BPMN2);
        RuntimeManager runtimeManager = createRuntimeManager(resources);

        RuntimeEngine runtimeEngine = runtimeManager.getRuntimeEngine(null);

        JmsIntegrationTestMethods jmsTests = new JmsIntegrationTestMethods("blah", false);
        jmsTests.remoteApiGroupAssignmentEngineeringTest(runtimeEngine);
    }
}