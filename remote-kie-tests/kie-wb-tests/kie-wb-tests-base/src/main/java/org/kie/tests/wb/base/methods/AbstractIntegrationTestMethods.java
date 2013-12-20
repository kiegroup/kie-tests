package org.kie.tests.wb.base.methods;

import static org.junit.Assert.*;
import static org.kie.tests.wb.base.methods.TestConstants.HUMAN_TASK_PROCESS_ID;
import static org.kie.tests.wb.base.methods.TestConstants.OBJECT_VARIABLE_PROCESS_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.process.audit.AuditLogService;
import org.jbpm.process.audit.CommandBasedAuditLogService;
import org.jbpm.process.audit.VariableInstanceLog;
import org.jbpm.services.task.commands.GetTaskCommand;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.services.client.api.RemoteRestRuntimeFactory;
import org.kie.services.client.api.command.RemoteRuntimeEngine;
import org.kie.tests.wb.base.test.objects.MyType;
import org.kie.tests.wb.base.test.objects.Person;
import org.kie.tests.wb.base.test.objects.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractIntegrationTestMethods {

    protected static Logger logger = LoggerFactory.getLogger(AbstractIntegrationTestMethods.class);
   
    protected final static int MAX_TRIES = 5;
    
    protected long findTaskId(long procInstId, List<TaskSummary> taskSumList) { 
        long taskId = -1;
        for( TaskSummary task : taskSumList ) { 
            if( task.getProcessInstanceId() == procInstId ) {
                taskId = task.getId();
            }
        }
        assertNotEquals("Could not determine taskId!", -1, taskId);
        return taskId;
    }
    
    protected TaskSummary findTaskSummary(long procInstId, List<TaskSummary> taskSumList) { 
        for( TaskSummary task : taskSumList ) { 
            if( task.getProcessInstanceId() == procInstId ) {
                return task;
            }
        }
        fail( "Unable to find task summary for process instance " + procInstId); 
        return null;
    }
 
    /**
     * Shared tests
     */
    
    protected void testExtraJaxbClassSerialization(RemoteRuntimeEngine engine) {
        
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
    
    protected void testParamSerialization(RemoteRuntimeEngine  engine, Object param) { 
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("myobject", param);
        long procInstId = engine.getKieSession().startProcess(OBJECT_VARIABLE_PROCESS_ID, parameters).getId();
        
        /**
         * Check that MyType was correctly deserialized on server side
         */
        List<VariableInstanceLog> varLogList = engine.getAuditLogService().findVariableInstancesByName("type", false);
        VariableInstanceLog thisProcInstVarLog = null;
        for( VariableInstanceLog varLog : varLogList ) {
            if( varLog.getProcessInstanceId() == procInstId ) { 
                thisProcInstVarLog = varLog;
            }
        }
        assertEquals( "type", thisProcInstVarLog.getVariableId() );
        assertEquals( "De/serialization of Kjar type did not work.", param.getClass().getName(), thisProcInstVarLog.getValue() );
    }
    
    public static void runRuleTaskProcess(KieSession ksession, AuditLogService auditLogService) { 
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
        ksession.fireAllRules();
        
        // Check
//        assertEquals("Poor customer", ((Request)ksession.getObject(factHandle)).getInvalidReason());
        assertNull(ksession.getProcessInstance(pi.getId()));
        
        List<VariableInstanceLog> varLogs = auditLogService.findVariableInstancesByName("requestReason", false);
        for( VariableInstanceLog varLog : varLogs ) { 
            if( varLog.getProcessInstanceId() == pi.getId() ) { 
                assertEquals( "Poor customer", varLog.getValue() );
            }
        }
    }


}
