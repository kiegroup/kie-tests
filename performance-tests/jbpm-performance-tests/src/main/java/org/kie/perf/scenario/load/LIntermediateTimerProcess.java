package org.kie.perf.scenario.load;

import java.util.ArrayList;
import java.util.List;

import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.perf.SharedMetricRegistry;
import org.kie.perf.jbpm.JBPMController;
import org.kie.perf.jbpm.constant.ProcessStorage;
import org.kie.perf.scenario.IPerfTest;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class LIntermediateTimerProcess implements IPerfTest {

    private JBPMController jc;

    private Meter completedProcess;
    private List<Long> completedProcessIds = new ArrayList<Long>();

    @Override
    public void init() {
        jc = JBPMController.getInstance();
        jc.addProcessEventListener(new DefaultProcessEventListener() {
            @Override
            public void afterProcessCompleted(ProcessCompletedEvent event) {
                completedProcess.mark();
                completedProcessIds.add(event.getProcessInstance().getId());
            }
        });

        jc.createRuntimeManager(ProcessStorage.IntermediateTimer.getPath());
    }

    @Override
    public void initMetrics() {
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        completedProcess = metrics.meter(MetricRegistry.name(LIntermediateTimerProcess.class, "scenario.process.completed"));
    }

    @Override
    public void execute() {
        RuntimeEngine runtimeEngine = jc.getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        ProcessInstance pi = ksession.startProcess(ProcessStorage.IntermediateTimer.getProcessDefinitionId());
        long pid = pi.getId();

        long maxTime = System.currentTimeMillis() + 5000;
        while (!completedProcessIds.contains(pid) && System.currentTimeMillis() < maxTime) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {

            }
        }
    }

    @Override
    public void close() {
        jc.tearDown();
    }

}
