package org.kie.tests.wb.base.methods;

import java.net.URL;

import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.remote.client.jaxb.JaxbCommandsRequest;
import org.kie.remote.client.jaxb.JaxbCommandsResponse;

public interface IntegrationTestMethods {

    void implSpecificTestParamSerialization(URL deploymentUrl, String user, String password, RuntimeEngine engine, Object obj);
    
    JaxbCommandsResponse implSpecificSendCommandRequest(JaxbCommandsRequest req, String user, String password, boolean ksession) throws Exception;
    
    RuntimeEngine getRemoteRuntimeEngine(URL deploymentUrl, String user, String password);
    RuntimeEngine getCorrelationPropertiesRemoteEngine(URL deploymentUrl, String deploymentId, String user, String password, String... businessKeys);
}
