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

public class LRemoteGroupHumanTaskProcess implements IPerfTest {

    private RemoteController rc;

    private Timer startProcess;
    private Timer queryTaskDuration;
    private Timer claimTaskDuration;
    private Timer startTaskDuration;
    private Timer completeTaskDuration;
    private Meter completedProcess;

    @Override
    public void init() {
        rc = RemoteControllerProvider.getRemoteController(KieWBTestConfig.DEPLOYMENT_ID);
        rc.setCredentials(UserStorage.EngUser.getUserId(), UserStorage.EngUser.getPassword());
    }

    @Override
    public void initMetrics() {
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        completedProcess = metrics.meter(MetricRegistry.name(LRemoteGroupHumanTaskProcess.class, "scenario.process.completed"));
        startProcess = metrics.timer(MetricRegistry.name(LRemoteGroupHumanTaskProcess.class, "scenario.process.start.duration"));
        queryTaskDuration = metrics.timer(MetricRegistry.name(LRemoteGroupHumanTaskProcess.class, "scenario.task.query.duration"));
        claimTaskDuration = metrics.timer(MetricRegistry.name(LRemoteGroupHumanTaskProcess.class, "scenario.task.claim.duration"));
        startTaskDuration = metrics.timer(MetricRegistry.name(LRemoteGroupHumanTaskProcess.class, "scenario.task.start.duration"));
        completeTaskDuration = metrics.timer(MetricRegistry.name(LRemoteGroupHumanTaskProcess.class, "scenario.task.complete.duration"));
    }

    @Override
    public void execute() {
        Timer.Context context;

        rc = rc.newPerProcessInstanceController(null);

        context = startProcess.time();
        ProcessInstance pi = rc.startProcess(ProcessStorage.GroupHumanTask.getProcessDefinitionId());
        context.stop();
        
        rc = rc.newPerProcessInstanceController(pi.getId());

        context = queryTaskDuration.time();
        TaskService taskService = rc.getTaskService();
        List<Long> tasks = taskService.getTasksByProcessInstanceId(pi.getId());
        Long taskSummaryId = tasks.get(0);
        context.stop();

        context = claimTaskDuration.time();
        taskService.claim(taskSummaryId, UserStorage.EngUser.getUserId());
        context.stop();

        context = startTaskDuration.time();
        taskService.start(taskSummaryId, UserStorage.EngUser.getUserId());
        context.stop();

        context = completeTaskDuration.time();
        taskService.complete(taskSummaryId, UserStorage.EngUser.getUserId(), null);
        context.stop();

        ProcessInstanceLog plog = rc.getAuditService().findProcessInstance(pi.getId());

        if (plog != null && plog.getStatus() == ProcessInstance.STATE_COMPLETED) {
            completedProcess.mark();
        }
    }

    @Override
    public void close() {

    }

}
