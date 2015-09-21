package org.kie.perf.scenario.load;

import org.kie.api.runtime.manager.audit.ProcessInstanceLog;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.perf.SharedMetricRegistry;
import org.kie.perf.remote.KieWBTestConfig;
import org.kie.perf.remote.RemoteController;
import org.kie.perf.remote.RemoteControllerProvider;
import org.kie.perf.remote.constant.ProcessStorage;
import org.kie.perf.scenario.IPerfTest;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class LRemoteIntermediateTimerProcess implements IPerfTest {

    private RemoteController rc;

    private Meter completedProcess;

    @Override
    public void init() {
        rc = RemoteControllerProvider.getRemoteController(KieWBTestConfig.DEPLOYMENT_ID);
    }

    @Override
    public void initMetrics() {
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        completedProcess = metrics.meter(MetricRegistry.name(LRemoteIntermediateTimerProcess.class, "scenario.process.completed"));
    }

    @Override
    public void execute() {

        rc = rc.newPerProcessInstanceController(null);
        ProcessInstance pi = rc.startProcess(ProcessStorage.IntermediateTimer.getProcessDefinitionId());
        rc = rc.newPerProcessInstanceController(pi.getId());
        long pid = pi.getId();

        long maxTime = System.currentTimeMillis() + 5000;
        int status = pi.getState();
        while (status != ProcessInstance.STATE_COMPLETED && System.currentTimeMillis() < maxTime) { // timer
                                                                                                    // event
                                                                                                    // should
                                                                                                    // wait
                                                                                                    // 1s
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
            ProcessInstanceLog plog = rc.getAuditService().findProcessInstance(pid);
            status = plog.getStatus();
        }
        if (status == ProcessInstance.STATE_COMPLETED) {
            completedProcess.mark();
        }
    }

    @Override
    public void close() {

    }

}
