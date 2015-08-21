package org.kie.perf.scenario.load;

import java.util.ArrayList;
import java.util.List;

import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.perf.SharedMetricRegistry;
import org.kie.perf.annotation.KPKLimit;
import org.kie.perf.jbpm.JBPMController;
import org.kie.perf.jbpm.constant.UserStorage;
import org.kie.perf.scenario.IPerfTest;
import org.kie.perf.scenario.PrepareEngine;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

@KPKLimit(1000)
public class L1000HumanTasksQueryFor implements IPerfTest {

    private JBPMController jc;

    private TaskService taskService;

    private List<TaskSummary> tasks = new ArrayList<TaskSummary>();

    @Override
    public void init() {
        jc = JBPMController.getInstance();
        jc.createRuntimeManager();

        taskService = jc.getRuntimeEngine().getTaskService();

        PrepareEngine.createNewTasks(false, 1000, taskService);
    }

    @Override
    public void initMetrics() {
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        metrics.register(MetricRegistry.name(L1000HumanTasksQueryFor.class, "scenario.tasks.query.size"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return tasks.size();
            }
        });
    }

    @Override
    public void execute() {
        tasks = taskService.getTasksAssignedAsPotentialOwner(UserStorage.PerfUser.getUserId(), "en-UK");
    }

    @Override
    public void close() {
        jc.tearDown();
    }

}
