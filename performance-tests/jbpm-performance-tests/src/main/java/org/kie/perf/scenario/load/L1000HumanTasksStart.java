package org.kie.perf.scenario.load;

import org.jbpm.services.task.events.DefaultTaskEventListener;
import org.kie.api.task.TaskEvent;
import org.kie.api.task.TaskService;
import org.kie.perf.SharedMetricRegistry;
import org.kie.perf.annotation.KPKLimit;
import org.kie.perf.jbpm.JBPMController;
import org.kie.perf.jbpm.constant.UserStorage;
import org.kie.perf.scenario.IPerfTest;
import org.kie.perf.scenario.PrepareEngine;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

@KPKLimit(1000)
public class L1000HumanTasksStart implements IPerfTest {
    
    private JBPMController jc;
    
    private TaskService taskService;
    
    private Meter taskStarted;
    
    @Override
    public void init() {
        jc = JBPMController.getInstance();
        
        jc.addTaskEventListener(new DefaultTaskEventListener(){
            @Override
            public void afterTaskStartedEvent(TaskEvent event) {
                taskStarted.mark();
            }
        });
        
        jc.createRuntimeManager();
        
        taskService = jc.getRuntimeEngine().getTaskService();
        
        PrepareEngine.createNewTasks(false, 1000, taskService);
    }
    
    @Override
    public void initMetrics() {
        taskId = 1;
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        taskStarted = metrics.meter(MetricRegistry.name(L1000HumanTasksStart.class, "scenario.task.started"));
    }

    static int taskId = 1;
    
    @Override
    public void execute() {
        taskService.start(taskId, UserStorage.PerfUser.getUserId());
        taskId++;
    }
    
    @Override
    public void close() {
        jc.tearDown();
    }
    
}
