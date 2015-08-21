package org.kie.perf.scenario.load;

import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.perf.SharedMetricRegistry;
import org.kie.perf.jbpm.JBPMController;
import org.kie.perf.jbpm.constant.ProcessStorage;
import org.kie.perf.jbpm.wih.ManualTaskWorkItemHandler;
import org.kie.perf.scenario.IPerfTest;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class LParallelGatewayTenTimesProcess implements IPerfTest {

    private JBPMController jc;

    private Meter completedProcess;

    @Override
    public void init() {
        jc = JBPMController.getInstance();
        jc.addProcessEventListener(new DefaultProcessEventListener() {
            @Override
            public void afterProcessCompleted(ProcessCompletedEvent event) {
                completedProcess.mark();
            }
        });
        jc.addWorkItemHandler("Manual Task", new ManualTaskWorkItemHandler());

        jc.createRuntimeManager(ProcessStorage.ParallelGatewayTenTimes.getPath());
    }

    @Override
    public void initMetrics() {
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        completedProcess = metrics.meter(MetricRegistry.name(LParallelGatewayTenTimesProcess.class, "scenario.process.completed"));
    }

    @Override
    public void execute() {
        RuntimeEngine runtimeEngine = jc.getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        ksession.startProcess(ProcessStorage.ParallelGatewayTenTimes.getProcessDefinitionId());
    }

    @Override
    public void close() {
        jc.tearDown();
    }

}
