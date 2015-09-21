package org.kie.perf.scenario.load.parametrized;

import java.util.HashMap;
import java.util.Map;

import org.kie.api.runtime.process.ProcessInstance;
import org.kie.perf.remote.KieWBTestConfig;
import org.kie.perf.remote.RemoteController;
import org.kie.perf.remote.RemoteControllerProvider;
import org.kie.perf.remote.constant.ProcessStorage;
import org.kie.perf.scenario.IPerfTest;

/**
 * http://docs.jboss.org/jbpm/v6.2/userguide/jBPMAsyncExecution.html#d0e21825
 */
public abstract class AbstractLRemoteAsyncStartJMSMultiInstanceSubProcesses implements IPerfTest {

    private RemoteController rc;
    
    public abstract int getLoops(); 

    @Override
    public void init() {
        // start of process may take a lot of time -> 30s timeout
        rc = RemoteControllerProvider.getRemoteController(KieWBTestConfig.DEPLOYMENT_ID, 30);
    }

    @Override
    public void initMetrics() {

    }

    @Override
    public void execute() {
        Map<String, Object> params = new HashMap<String, Object>();
        int loops = getLoops();
        params.put("loops", loops);
        ProcessInstance pi = rc.startProcess(ProcessStorage.MultiInstanceSubprocessesAsync.getProcessDefinitionId(), params);
        long pid = pi.getId();
        long maxTime = System.currentTimeMillis() + getLoops() * 200;
        int state = pi.getState();
        while (state != ProcessInstance.STATE_COMPLETED && System.currentTimeMillis() < maxTime) {
            try {
                Thread.sleep(750);
            } catch (InterruptedException e) {

            }
            state = rc.getAuditService().findProcessInstance(pid).getStatus();
        }
    }

    @Override
    public void close() {

    }

}
