package org.kie.perf.scenario.load;

import java.util.List;
import java.util.Random;

import org.kie.api.runtime.manager.audit.AuditService;
import org.kie.api.runtime.manager.audit.VariableInstanceLog;
import org.kie.perf.SharedMetricRegistry;
import org.kie.perf.jbpm.JBPMController;
import org.kie.perf.jbpm.constant.ProcessStorage;
import org.kie.perf.scenario.IPerfTest;
import org.kie.perf.scenario.PrepareEngine;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class L1000VariablePIDQuery implements IPerfTest {

    private JBPMController jc;
    private AuditService auditService;
    private List<Long> pids;

    private Timer pidVarsDuration;
    private Timer valVarsDuration;

    @Override
    public void init() {
        jc = JBPMController.getInstance();
        jc.createRuntimeManager(ProcessStorage.VariableHumanTask.getPath());
        auditService = jc.getRuntimeEngine().getAuditService();
        pids = PrepareEngine.createNewVariableHumanTask(10000, jc);
    }

    @Override
    public void initMetrics() {
        
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        pidVarsDuration = metrics.timer(MetricRegistry.name(L1000VariablePIDQuery.class, "scenario.audit.variable.pid.duration"));
        valVarsDuration = metrics.timer(MetricRegistry.name(L1000VariablePIDQuery.class, "scenario.audit.variable.val.duration"));
    }

    @Override
    public void execute() {
        Timer.Context context;

        int age = new Random().nextInt(10000) + 1;
        Long pid = pids.get(age-1);
        
        context = pidVarsDuration.time();
        List<? extends VariableInstanceLog> piage = auditService.findVariableInstances(pid, "name");
        context.stop();
        
        context = valVarsDuration.time();
        List<? extends VariableInstanceLog> valage = auditService.findVariableInstancesByNameAndValue("name", "Name-" + String.valueOf(age), true);
        context.stop();
    }

    @Override
    public void close() {
        jc.tearDown();
    }

}
