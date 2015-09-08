package org.kie.perf.scenario.load;

import org.kie.perf.annotation.KPKConstraint;
import org.kie.perf.scenario.load.parametrized.AbstractLRemoteAsyncStartJMSMultiInstanceSubProcesses;

@KPKConstraint({ "jbpm.runtimeManagerStrategy=PerProcessInstance" })
public class LRemote100AsyncStartJMSMultiInstanceSubProcesses extends AbstractLRemoteAsyncStartJMSMultiInstanceSubProcesses {
    
    @Override
    public int getLoops() {
        return 100;
    }

}
