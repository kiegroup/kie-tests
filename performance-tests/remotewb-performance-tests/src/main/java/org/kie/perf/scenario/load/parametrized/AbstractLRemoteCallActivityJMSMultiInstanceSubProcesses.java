package org.kie.perf.scenario.load.parametrized;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.api.runtime.manager.audit.ProcessInstanceLog;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.perf.SharedMetricRegistry;
import org.kie.perf.remote.KieWBTestConfig;
import org.kie.perf.remote.RemoteController;
import org.kie.perf.remote.RemoteControllerProvider;
import org.kie.perf.remote.constant.ProcessStorage;
import org.kie.perf.scenario.IPerfTest;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

/**
 * http://docs.jboss.org/jbpm/v6.2/userguide/jBPMAsyncExecution.html#d0e21825
 */
public abstract class AbstractLRemoteCallActivityJMSMultiInstanceSubProcesses implements IPerfTest {

    private RemoteController rc;

    private Histogram completedSubprocess;
    
    public abstract int getLoops(); 

    @Override
    public void init() {
        // start of process may take a lot of time -> 30s timeout
        rc = RemoteControllerProvider.getRemoteController(KieWBTestConfig.DEPLOYMENT_ID, 30);
    }

    @Override
    public void initMetrics() {
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        completedSubprocess = metrics.histogram(MetricRegistry.name(AbstractLRemoteCallActivityJMSMultiInstanceSubProcesses.class, "scenario.subprocess.completed"));
    }

    @Override
    public void execute() {
        Map<String, Object> params = new HashMap<String, Object>();
        int loops = getLoops();
        params.put("loops", loops);
        ProcessInstance pi = rc.startProcess(ProcessStorage.MultiInstanceSubprocesses.getProcessDefinitionId(), params);
        long pid = pi.getId();
        long maxTime = System.currentTimeMillis() + getLoops() * 200;
        int completed = 0;
        while (completed != loops && System.currentTimeMillis() < maxTime) {
            try {
                Thread.sleep(750);
            } catch (InterruptedException e) {

            }
            List<? extends ProcessInstanceLog> subprocesses = rc.getAuditService().findSubProcessInstances(pid);
            completed = 0;
            for (ProcessInstanceLog pil : subprocesses) {
                if (pil.getStatus() == ProcessInstance.STATE_COMPLETED) {
                    completed++;
                }
            }
            completedSubprocess.update(completed);
        }
    }

    @Override
    public void close() {

    }

}
