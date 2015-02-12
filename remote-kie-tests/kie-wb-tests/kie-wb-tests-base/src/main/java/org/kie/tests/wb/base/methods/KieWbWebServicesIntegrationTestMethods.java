package org.kie.tests.wb.base.methods;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;
import static org.kie.tests.wb.base.util.TestConstants.SCRIPT_TASK_PROCESS_ID;

import java.net.URL;

import org.kie.remote.client.jaxb.JaxbCommandsRequest;
import org.kie.remote.client.jaxb.JaxbCommandsResponse;
import org.kie.remote.jaxb.gen.JaxbStringObjectPairArray;
import org.kie.remote.jaxb.gen.StartProcessCommand;
import org.kie.remote.jaxb.gen.util.JaxbStringObjectPair;
import org.kie.remote.services.ws.command.generated.CommandWebService;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandResponse;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessInstanceResponse;
import org.kie.tests.MyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWebServicesIntegrationTestMethods {

    private static Logger logger = LoggerFactory.getLogger(KieWbRestIntegrationTestMethods.class);
    
    public void startSimpleProcess(URL deploymentUrl) throws Exception {
        CommandWebService commandWebService = createClient(deploymentUrl); 
       
        logger.info("[Client] Webservice request.");
        // create request object
        StartProcessCommand cmd = new StartProcessCommand();
        cmd.setProcessId(SCRIPT_TASK_PROCESS_ID);
        JaxbStringObjectPairArray map = new JaxbStringObjectPairArray();
        JaxbStringObjectPair keyValue = new JaxbStringObjectPair();
        keyValue.setKey("myobject");
        keyValue.setValue(new MyType("variable", 29));
        map.getItems().add(keyValue);
        cmd.setParameter(map);
        JaxbCommandsRequest req = new JaxbCommandsRequest(KJAR_DEPLOYMENT_ID, cmd);
        
        // Get a response from the WebService
        final JaxbCommandsResponse response = commandWebService.execute(req);
        assertNotNull( "Null webservice response", response );
        assertFalse( "Empty webservice response", response.getResponses().isEmpty() );

        // check response
        JaxbCommandResponse<?> cmdResp = response.getResponses().get(0);
        assertNotNull( "Null command response", cmdResp );
        if( ! (cmdResp instanceof JaxbProcessInstanceResponse) ) { 
            System.out.println( "!!: " + cmdResp.getClass().getSimpleName() );
            assertTrue( "Incorrect cmd response type", cmdResp instanceof JaxbProcessInstanceResponse );
        }
        
        logger.info("[WebService] response: {} [{}]", 
                ((JaxbProcessInstanceResponse) cmdResp).getId(),
                ((JaxbProcessInstanceResponse) cmdResp).getProcessId()
                );
    }

    private CommandWebService createClient(URL deploymentUrl) throws Exception {
        final String user = "mary";
        final String pwd = "mary123@";
 
        CommandWebService client =
        RemoteRuntimeEngineFactory.newCommandWebServiceClientBuilder()
            .addServerUrl(deploymentUrl)
            .addUserName(user)
            .addPassword(pwd)
            .addDeploymentId(KJAR_DEPLOYMENT_ID)
            .addExtraJaxbClasses(MyType.class)
            .buildBasicAuthClient();
        
        return client;
    }
    
}
