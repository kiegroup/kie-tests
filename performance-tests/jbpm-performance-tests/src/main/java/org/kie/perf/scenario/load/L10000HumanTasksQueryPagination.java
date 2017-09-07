package org.kie.perf.scenario.load;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.kie.services.impl.RuntimeDataServiceImpl;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.task.audit.TaskAuditServiceFactory;
import org.jbpm.shared.services.impl.TransactionalCommandService;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.identity.IdentityProvider;
import org.kie.perf.SharedMetricRegistry;
import org.kie.perf.annotation.KPKLimit;
import org.kie.perf.jbpm.JBPMController;
import org.kie.perf.jbpm.constant.UserStorage;
import org.kie.perf.scenario.IPerfTest;
import org.kie.perf.scenario.PrepareEngine;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

@KPKLimit(10000)
public class L10000HumanTasksQueryPagination implements IPerfTest {

    private JBPMController jc;

    private RuntimeDataService runtimeDataService;
    private TaskService taskService;

    private List<TaskSummary> tasks = new ArrayList<TaskSummary>();

    @Override
    public void init() {
        jc = JBPMController.getInstance();
        jc.createRuntimeManager();

        taskService = jc.getRuntimeEngine().getTaskService();
        runtimeDataService = new RuntimeDataServiceImpl();
        ((RuntimeDataServiceImpl) runtimeDataService).setCommandService(new TransactionalCommandService(jc.getEmf()));
        ((RuntimeDataServiceImpl) runtimeDataService).setIdentityProvider(new TestIdentityProvider());
        ((RuntimeDataServiceImpl) runtimeDataService).setTaskService(taskService);
        ((RuntimeDataServiceImpl) runtimeDataService).setTaskAuditService(TaskAuditServiceFactory
                .newTaskAuditServiceConfigurator().setTaskService(taskService).getTaskAuditService());

        PrepareEngine.createNewTasks(false, 10000, taskService);
    }

    @Override
    public void initMetrics() {
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        metrics.register(MetricRegistry.name(L10000HumanTasksQueryPagination.class, "scenario.tasks.query.page.size"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return tasks.size();
                    }
                });
    }

    @Override
    public void execute() {
        String userId = UserStorage.PerfUser.getUserId();
        tasks = runtimeDataService.taskSummaryQuery(userId).maxResults(100).build().getResultList();
    }

    @Override
    public void close() {
        jc.tearDown();
    }

    public static class TestIdentityProvider implements IdentityProvider {

        private List<String> roles = new ArrayList<String>();

        public String getName() {
            return UserStorage.PerfUser.getUserId();
        }

        public List<String> getRoles() {
            return roles;
        }

        @Override
        public boolean hasRole(String role) {
            return roles.contains(role);
        }

        // just for testing
        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }

}
