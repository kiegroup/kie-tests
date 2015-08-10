package org.kie.perf.scenario.load;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.executor.ExecutorServiceFactory;
import org.jbpm.executor.impl.wih.AsyncWorkItemHandler;
import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.executor.ErrorInfo;
import org.kie.api.executor.ExecutorService;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.query.QueryContext;
import org.kie.perf.SharedMetricRegistry;
import org.kie.perf.annotation.KPKConstraint;
import org.kie.perf.annotation.KPKLimit;
import org.kie.perf.jbpm.JBPMController;
import org.kie.perf.jbpm.constant.ProcessStorage;
import org.kie.perf.scenario.IPerfTest;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

@KPKLimit(1)
@KPKConstraint({"jbpm.runtimeManagerStrategy=PerProcessInstance"})
public class L100MultiInstanceSubprocesses implements IPerfTest {

    private static final int LOOPS = 100;
    private static final int THREAD_POOL_SIZE = 9; 
    
    private JBPMController jc;

    private Meter completedProcess;
    
    private ExecutorService executorService;
    
    private List<Long> completedProcessIds = new ArrayList<Long>();
    
    @Override
    public void init() {
        jc = JBPMController.getInstance();
        jc.addProcessEventListener(new DefaultProcessEventListener(){
            @Override
            public void afterProcessCompleted(ProcessCompletedEvent event) {
                completedProcess.mark();
                completedProcessIds.add(event.getProcessInstance().getId());
            }
        });
        
        executorService = ExecutorServiceFactory.newExecutorService();
        executorService.setThreadPoolSize(THREAD_POOL_SIZE);
        executorService.setInterval(1);
        executorService.setRetries(5);
        executorService.init();
        
        jc.addWorkItemHandler("async", new AsyncWorkItemHandler(executorService));
        
        jc.createRuntimeManager(ProcessStorage.AsyncPrintTask.getPath(), ProcessStorage.MultiInstanceSubprocesses.getPath());
    }
    
    @Override
    public void initMetrics() {
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        completedProcess = metrics.meter(MetricRegistry.name(L100MultiInstanceSubprocesses.class, "scenario.process.completed"));
    }

    @Override
    public void execute() {
        RuntimeEngine runtimeEngine = jc.getRuntimeEngine(); 
        KieSession ksession = runtimeEngine.getKieSession();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("loops", LOOPS);
        ProcessInstance pi = ksession.startProcess(ProcessStorage.MultiInstanceSubprocesses.getProcessDefinitionId(), params);
        long pid = pi.getId();
        
        long maxTime = System.currentTimeMillis() + LOOPS*200;
        while (!completedProcessIds.contains(pid) && System.currentTimeMillis() < maxTime) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                
            }
        }
    }
    
    @Override
    public void close() {
        QueryContext qc = new QueryContext();
        qc.setCount(LOOPS);
        List<ErrorInfo> errors = executorService.getAllErrors(qc); // OptimisticLockExceptions are expected on PerProcessInstanceStrategy
        SharedMetricRegistry.getInstance().counter("scenario.executor.errors").inc(errors.size());
        
        executorService.destroy();
        jc.tearDown();
    }
    
}
