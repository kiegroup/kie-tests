package org.custom.process.listeners;

import java.util.Map;
import java.util.Map.Entry;

import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.jbpm.workflow.instance.node.HumanTaskNodeInstance;
import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Task;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

public class CustomProcessEventListener extends DefaultProcessEventListener {

    private RuntimeManager runtimeManager;

    public CustomProcessEventListener(RuntimeManager runtimeManager) { 
        this.runtimeManager = runtimeManager;
    }
   
    @Override
    public void beforeNodeTriggered( ProcessNodeTriggeredEvent event ) {
        System.out.println( "ANT: [" + event.getNodeInstance().getNodeId() + "/" + event.getNodeInstance().getNodeName() +"]" );
        ProcessInstance procInst = event.getProcessInstance();
        
        long procInstId = procInst.getId();
       
        // This code should only be used with an active process (e.g. in a process event listener)
        // Actually, the .getVariables() and .getVariable(..) methods should probably to be promoted 
        // to the (kie-) API (and defensively wrapped) but engineering has not had time to do this yet. 
        Map<String,Object> procInstVariables = ((WorkflowProcessInstanceImpl) procInst).getVariables();
        
        sendProcessInformation(procInstId, procInstVariables);
    }

    private final static String [] notInputParameters = { 
        "NodeName",
        "Locale",
        "TaskName",
        "Comment", 
        "Priority",
        "Skippable",
        "ParentId",
        "CreatedBy",
        "DueDate",
        "Content"
    };
    
    @Override
    public void afterNodeTriggered( ProcessNodeTriggeredEvent event ) {
        System.out.println( "ANT: [" + event.getNodeInstance().getNodeId() + "/" + event.getNodeInstance().getNodeName() +"]" );
        
        NodeInstance nodeInstance = event.getNodeInstance();
        if( nodeInstance instanceof HumanTaskNodeInstance ) { 
            long procInstid = event.getProcessInstance().getId();
            WorkItem workItem = ((HumanTaskNodeInstance) nodeInstance).getWorkItem();
            if( runtimeManager != null ) { 
                RuntimeEngine runtime = runtimeManager.getRuntimeEngine(ProcessInstanceIdContext.get(procInstid));
                TaskService taskService = runtime.getTaskService();
                Task myTask = taskService.getTaskByWorkItemId(workItem.getId());
                Map<String, Object> inputMappings = workItem.getParameters();
                for( String notParamKey : notInputParameters ) { 
                    inputMappings.remove(notParamKey);
                }
                
                sendTaskInformation(myTask.getId(), inputMappings);
            }
        }
        
    }

    public void sendTaskInformation(long taskId, Map<String, Object> inputMappings) { 
       System.out.println( "task id: " + taskId );
       for( Entry<String, Object> entry : inputMappings.entrySet() ) { 
           System.out.println( "task var: " + entry.getKey() + "/" + entry.getValue().toString() );
       }
    }
    
    public void sendProcessInformation(long procInstId, Map<String, Object> procInstVariables) { 
       System.out.println( "process id: " + procInstId );
       for( Entry<String, Object> entry : procInstVariables.entrySet() ) { 
           System.out.println( "proc inst var: " + entry.getKey() + "/" + entry.getValue().toString() );
       }
    }
    

}
