package org.kie.tests.wb.base;

import static org.kie.tests.wb.base.methods.AbstractIntegrationTestMethods.runRuleTaskProcess;
import static org.kie.tests.wb.base.methods.TestConstants.TASK_CONTENT_PROCESS_ID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.process.audit.CommandBasedAuditLogService;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.Test;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskData;

public class ProcessTest extends JbpmJUnitBaseTestCase {

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
        assertNotNull( "No content found!", content );
        assertTrue( "Content is empty!", content.getContent().length > 0);
        Map<String, Object> contMap = (Map) ContentMarshallerHelper.unmarshall(content.getContent(), null);
       
        assertEquals("reviewer", contMap.get("GroupId"));
        assertEquals( 3, contMap.keySet().size());
    }
}
