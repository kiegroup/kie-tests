package org.kie.perf.remote.constant;

public enum ProcessStorage {

    HumanTask("org.kie.perf.HumanTask"), GroupHumanTask("org.kie.perf.GroupHumanTask"), ParallelGatewayTwoTimes(
            "org.kie.perf.ParallelGatewayTwoTimes"), RuleTask("org.kie.perf.RuleTask"), StartEnd("org.kie.perf.StartEnd"), IntermediateTimer(
            "org.kie.perf.IntermediateTimer"), MultiInstanceSubprocesses("org.kie.perf.MultiInstanceSubprocesses"), MultiInstanceSubprocessesAsync(
            "org.kie.perf.MultiInstanceSubprocessesAsync"), AsyncPrintTask("org.kie.perf.AsyncPrintTask");

    private String processDefinitionId;

    private ProcessStorage(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

}
