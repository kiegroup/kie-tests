package org.kie.perf.scenario.load;

import java.util.List;

import org.kie.api.runtime.manager.audit.AuditService;
import org.kie.api.runtime.manager.audit.VariableInstanceLog;
import org.kie.perf.SharedMetricRegistry;
import org.kie.perf.annotation.KPKLimit;
import org.kie.perf.jbpm.JBPMController;
import org.kie.perf.jbpm.constant.ProcessStorage;
import org.kie.perf.scenario.IPerfTest;
import org.kie.perf.scenario.PrepareEngine;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

@KPKLimit(100)
public class L1000VariablePIDQuery implements IPerfTest {

    private JBPMController jc;
    private AuditService auditService;
    private List<Long> pids;

    private Timer pidVarsDuration;
    private Timer valVarsDuration;
    /**private Meter pidVars;
    private Meter valVars;*/
    
    private int age;

    @Override
    public void init() {
        jc = JBPMController.getInstance();
        jc.createRuntimeManager(ProcessStorage.VariableHumanTask.getPath());
        auditService = jc.getRuntimeEngine().getAuditService();
    }

    @Override
    public void initMetrics() {
        age = 1;
        pids = PrepareEngine.createNewVariableHumanTask(100000, jc);
        
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        pidVarsDuration = metrics.timer(MetricRegistry.name(L1000VariablePIDQuery.class, "scenario.audit.variable.pid.duration"));
        valVarsDuration = metrics.timer(MetricRegistry.name(L1000VariablePIDQuery.class, "scenario.audit.variable.val.duration"));
        //pidVars = metrics.meter(MetricRegistry.name(L1000VariablePIDQuery.class, "scenario.audit.variable.pidcount"));
        //valVars = metrics.meter(MetricRegistry.name(L1000VariablePIDQuery.class, "scenario.audit.variable.valcount"));
    }

    @Override
    public void execute() {
        Timer.Context context;

        int rounds = 0;
        for (Long pid : pids) {
            context = pidVarsDuration.time();
            List<? extends VariableInstanceLog> piage = auditService.findVariableInstances(pid, "age");
            context.stop();
            /**for (VariableInstanceLog log : piage) {
                pidVars.mark();
            }*/
            
            context = valVarsDuration.time();
            List<? extends VariableInstanceLog> valage = auditService.findVariableInstancesByNameAndValue("age", String.valueOf(age), true);
            context.stop();
            /**for (VariableInstanceLog log : valage) {
                valVars.mark();
            }*/
            
            age++;
            rounds++;
            if (age > pids.size()) {
                age = 1;
            }
            if (rounds == 1000) {
                break;
            }
        }
    }

    @Override
    public void close() {
        jc.tearDown();
    }

}
