package org.kie.perf.scenario.load;

import java.util.HashMap;
import java.util.Map;

import org.kie.api.runtime.process.ProcessInstance;
import org.kie.perf.SharedMetricRegistry;
import org.kie.perf.jbpm.model.UserFact;
import org.kie.perf.remote.KieWBTestConfig;
import org.kie.perf.remote.RemoteController;
import org.kie.perf.remote.RemoteControllerProvider;
import org.kie.perf.remote.constant.ProcessStorage;
import org.kie.perf.scenario.IPerfTest;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class LRuleTaskProcess implements IPerfTest {
    
    private RemoteController rc;

    private Meter completedProcess;
    
    @Override
    public void init() {
        rc = RemoteControllerProvider.getRemoteController(KieWBTestConfig.DEPLOYMENT_ID);
    }
    
    @Override
    public void initMetrics() {
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        completedProcess = metrics.meter(MetricRegistry.name(LRuleTaskProcess.class, "scenario.process.completed"));
    }

    @Override
    public void execute() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("user", new UserFact("user", 15));
        ProcessInstance pi = rc.startProcess(ProcessStorage.RuleTask.getProcessDefinitionId(), params);
        if (pi != null && pi.getState() == ProcessInstance.STATE_COMPLETED) {
            completedProcess.mark();
        }
    }
    
    @Override
    public void close() {
        
    }
    
}
