package org.kie.tests.wb.base.methods;

import static org.junit.Assert.*;

import java.util.List;

import org.kie.api.task.model.TaskSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractIntegrationTestMethods {

    protected static Logger logger = LoggerFactory.getLogger(AbstractIntegrationTestMethods.class);
   
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
    
}
