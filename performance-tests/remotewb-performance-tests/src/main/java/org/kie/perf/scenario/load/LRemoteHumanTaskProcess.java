package org.kie.perf.scenario.load;

import java.util.List;

import org.kie.api.runtime.manager.audit.ProcessInstanceLog;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.perf.SharedMetricRegistry;
import org.kie.perf.remote.KieWBTestConfig;
import org.kie.perf.remote.RemoteController;
import org.kie.perf.remote.RemoteControllerProvider;
import org.kie.perf.remote.constant.ProcessStorage;
import org.kie.perf.remote.constant.UserStorage;
import org.kie.perf.scenario.IPerfTest;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class LRemoteHumanTaskProcess implements IPerfTest {
    
    private RemoteController rc;

    private Timer startProcess;
    private Timer startTaskDuration;
    private Timer completeTaskDuration;
    private Timer queryProcessInstanceDuration;
    private Meter completedProcess;
    
    @Override
    public void init() {
        rc = RemoteControllerProvider.getRemoteController(KieWBTestConfig.DEPLOYMENT_ID);
    }
    
    @Override
    public void initMetrics() {
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        startProcess = metrics.timer(MetricRegistry.name(LRemoteHumanTaskProcess.class, "scenario.process.start.duration"));
        startTaskDuration = metrics.timer(MetricRegistry.name(LRemoteHumanTaskProcess.class, "scenario.task.start.duration"));
        completeTaskDuration = metrics.timer(MetricRegistry.name(LRemoteHumanTaskProcess.class, "scenario.task.complete.duration"));
        queryProcessInstanceDuration = metrics.timer(MetricRegistry.name(LRemoteHumanTaskProcess.class, "scenario.process.query.duration"));
        completedProcess = metrics.meter(MetricRegistry.name(LRemoteHumanTaskProcess.class, "scenario.process.completed"));
    }

    @Override
    public void execute() {
        Timer.Context context;

        context = startProcess.time();
        ProcessInstance pi = rc.startProcess(ProcessStorage.HumanTask.getProcessDefinitionId());
        context.stop();
        
        TaskService taskService = rc.getTaskService();
        List<Long> tasks = taskService.getTasksByProcessInstanceId(pi.getId());
        Long taskSummaryId = tasks.get(0);

        context = startTaskDuration.time();
        taskService.start(taskSummaryId, UserStorage.PerfUser.getUserId());
        context.stop();

        context = completeTaskDuration.time();
        taskService.complete(taskSummaryId, UserStorage.PerfUser.getUserId(), null);
        context.stop();

        context = queryProcessInstanceDuration.time();
        ProcessInstanceLog plog = rc.getAuditService().findProcessInstance(pi.getId());
        context.stop();
        
        if (plog != null && plog.getStatus() == ProcessInstance.STATE_COMPLETED) {
            completedProcess.mark();
        }
    }
    
    @Override
    public void close() {
        
    }
    
}
