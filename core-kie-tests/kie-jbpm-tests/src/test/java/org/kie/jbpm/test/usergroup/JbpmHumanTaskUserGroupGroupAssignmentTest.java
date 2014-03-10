package org.kie.jbpm.test.usergroup;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.jbpm.test.usergroup.task.TestJaasUserGroupCallbackImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JbpmHumanTaskUserGroupGroupAssignmentTest extends JbpmJUnitBaseTestCase {

  private static final Logger logger = LoggerFactory.getLogger(JbpmHumanTaskUserGroupGroupAssignmentTest.class);
  
  private static String MARY = "mary";
  private static String JOHN = "john";
  
  // Copied from QE's RemoteRuntimeUserTaskGroupAssignmentTest (jbossqe/brms.git)
    @Test
    public void testProcess() {
        createRuntimeManager("bpmn2/groupAssignmentHumanTask.bpmn2");
        RuntimeEngine runtimeEngine = getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        TaskService taskService = runtimeEngine.getTaskService();
        
        ProcessInstance pi = ksession.startProcess("org.jboss.qa.bpms.GroupAssignmentHumanTask");
        assertNotNull( "Null process instance.", pi);
        assertEquals("Incorrect process state", ProcessInstance.STATE_ACTIVE, pi.getState());

        // assert the task
        TaskSummary taskSummary = getTaskSummary(taskService, pi.getId(), Status.Ready);
        assertNull("Actual owner", taskSummary.getActualOwner());
        assertNull("Potential owners", taskSummary.getPotentialOwners());
        assertEquals("Task name", "Task 1", taskSummary.getName());

        TestJaasUserGroupCallbackImpl.setCurrentUser(MARY);
        // complete 'Task 1' as mary
        taskService.claim(taskSummary.getId(), MARY);
        taskService.start(taskSummary.getId(), MARY);
        taskService.complete(taskSummary.getId(), MARY, null);

        // now make sure that the next task has been assigned to the
        // correct person. it should be mary.
        taskSummary = getTaskSummary(taskService, pi.getId(), Status.Reserved);
        assertEquals( "Assigned task name", "Task 2", taskSummary.getName());
        assertEquals( "Assigned to correct user", MARY, taskSummary.getActualOwner().getId());

        // complete 'Task 2' as john
        taskService.release(taskSummary.getId(), MARY);
        
        TestJaasUserGroupCallbackImpl.setCurrentUser(JOHN);
        taskService.claim(taskSummary.getId(), JOHN);
        taskService.start(taskSummary.getId(), JOHN);
        taskService.complete(taskSummary.getId(), JOHN, null);

        // now make sure that the next task has been assigned to the
        // correct person. it should be john.
        taskSummary = getTaskSummary(taskService, pi.getId(), Status.Reserved);
        assertEquals( "Reassigned task name", "Task 3", taskSummary.getName());
        assertEquals( "Reassigned to correct user", JOHN, taskSummary.getActualOwner().getId());

        // complete 'Task 3' as john
        taskService.start(taskSummary.getId(), JOHN);
        taskService.complete(taskSummary.getId(), JOHN, null);

        // assert process finished
        pi = ksession.getProcessInstance(pi.getId());
        assertNull( "Completed process instance", pi);
    }

    protected TaskSummary getTaskSummary(TaskService taskService, long processInstanceId, Status status) {
        List<Status> statusList = new ArrayList<Status>();
        statusList.add(status);
        List<TaskSummary> taskSummaryList = taskService.getTasksByStatusByProcessInstanceId(processInstanceId, statusList, "en-UK");
        assertEquals( "Incorrect task summary list size", 1, taskSummaryList.size());
        return taskSummaryList.get(0);
    }
}
