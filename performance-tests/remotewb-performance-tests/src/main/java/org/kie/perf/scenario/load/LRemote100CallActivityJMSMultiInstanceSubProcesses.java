package org.kie.perf.scenario.load;

import org.kie.perf.annotation.KPKConstraint;
import org.kie.perf.scenario.load.parametrized.AbstractLRemoteCallActivityJMSMultiInstanceSubProcesses;

@KPKConstraint({ "jbpm.runtimeManagerStrategy=PerProcessInstance" })
public class LRemote100CallActivityJMSMultiInstanceSubProcesses extends AbstractLRemoteCallActivityJMSMultiInstanceSubProcesses {
    
    @Override
    public int getLoops() {
        return 100;
    }

}
