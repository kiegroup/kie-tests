package org.kie.perf.scenario.load;

import org.kie.perf.annotation.KPKConstraint;
import org.kie.perf.scenario.load.parametrized.AbstractLRemoteCallActivityJMSMultiInstanceSubProcesses;

@KPKConstraint({ "jbpm.runtimeManagerStrategy=PerProcessInstance" })
public class LRemote300CallActivityJMSMultiInstanceSubProcesses extends AbstractLRemoteCallActivityJMSMultiInstanceSubProcesses {
    
    @Override
    public int getLoops() {
        return 300;
    }

}
