package org.kie.tests.wb.base.methods;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.kie.tests.wb.base.util.TestConstants.HUMAN_TASK_PROCESS_ID;
import static org.kie.tests.wb.base.util.TestConstants.KJAR_DEPLOYMENT_ID;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.kie.api.command.Command;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Task;
import org.kie.internal.jaxb.StringKeyObjectValueMap;
import org.kie.remote.client.jaxb.JaxbCommandsRequest;
import org.kie.remote.client.jaxb.JaxbCommandsResponse;
import org.kie.remote.jaxb.gen.GetContentCommand;
import org.kie.remote.jaxb.gen.GetTaskCommand;
import org.kie.remote.jaxb.gen.GetTaskContentCommand;
import org.kie.remote.jaxb.gen.GetTasksByProcessInstanceIdCommand;
import org.kie.remote.jaxb.gen.JaxbStringObjectPairArray;
import org.kie.remote.jaxb.gen.StartProcessCommand;
import org.kie.remote.jaxb.gen.util.JaxbStringObjectPair;
import org.kie.remote.services.ws.command.generated.CommandWebService;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandResponse;
import org.kie.services.client.serialization.jaxb.impl.JaxbLongListResponse;
import org.kie.services.client.serialization.jaxb.impl.process.JaxbProcessInstanceResponse;
import org.kie.tests.MyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieWbWebServicesIntegrationTestMethods {

    private static Logger logger = LoggerFactory.getLogger(KieWbRestIntegrationTestMethods.class);
    
    public static void startSimpleProcess(URL deploymentUrl) throws Exception {
        CommandWebService commandWebService = createDefaultClient(deploymentUrl); 
        
        startSimpleProcess(commandWebService);
    }
    
    public static void startSimpleProcess(CommandWebService commandWebService ) throws Exception {
        // start process
        StartProcessCommand spc = new StartProcessCommand();
        spc.setProcessId(HUMAN_TASK_PROCESS_ID);
        JaxbStringObjectPairArray map = new JaxbStringObjectPairArray();
        JaxbStringObjectPair keyValue = new JaxbStringObjectPair();
        keyValue.setKey("myobject");
        keyValue.setValue(new MyType("variable", 29));
        map.getItems().add(keyValue);
        spc.setParameter(map);
        
        // webService
        JaxbProcessInstanceResponse jpir = doWebserviceRequest(commandWebService, spc, "start process", JaxbProcessInstanceResponse.class);
        long procInstId = ((JaxbProcessInstanceResponse) jpir).getId();
       
        // get task id
        GetTasksByProcessInstanceIdCommand gtbic = new GetTasksByProcessInstanceIdCommand();
        gtbic.setProcessInstanceId(procInstId);
       
        // webservice
        JaxbLongListResponse jllr = doWebserviceRequest(commandWebService, gtbic, "get tasks by", JaxbLongListResponse.class );
        List<Long> taskIds = jllr.getResult();
        
        assertFalse( "Empty task id list", taskIds.isEmpty());
        long taskId = taskIds.get(0);
       
        // get task and task content
        GetTaskCommand gtc = new GetTaskCommand();
        gtc.setTaskId(taskId);
        GetTaskContentCommand gtcc = new GetTaskContentCommand();
        gtcc.setTaskId(taskId);
        
        // webservice
        JaxbCommandsRequest req = new JaxbCommandsRequest(KJAR_DEPLOYMENT_ID, gtc);
        req.getCommands().add(gtcc);
        JaxbCommandsResponse response = commandWebService.execute(req);
        
        // task and content response
        Task task = (Task) response.getResponses().get(0).getResult();
        Map<String, Object> contentMap = (Map<String, Object>) response.getResponses().get(1).getResult();

        // get content
        GetContentCommand gcc = new GetContentCommand();
        gcc.setContentId(task.getTaskData().getDocumentContentId());
       
        // webservice
        req = new JaxbCommandsRequest(KJAR_DEPLOYMENT_ID, gcc);
        response = commandWebService.execute(req);
        
        // content response
        Content content = (Content) response.getResponses().get(0).getResult();
        Object contentMapObj = ContentMarshallerHelper.unmarshall(content.getContent(), null);
        if( contentMapObj != null ) { 
            contentMap = (Map<String, Object>) contentMapObj;
            for( Entry<String, Object> entry : contentMap.entrySet() ) { 
                logger.info(entry.getKey() + " -> "  +  entry.getValue()); 
            }
        }
        
    }

    private static <T> T doWebserviceRequest(CommandWebService service, Command<?> cmd, String oper, Class<T> respClass) throws Exception { 
        // Get a response from the WebService
        JaxbCommandsRequest req = new JaxbCommandsRequest(KJAR_DEPLOYMENT_ID, cmd);
        JaxbCommandsResponse response = service.execute(req);
        assertNotNull( oper + ": null response", response );
        assertFalse( oper + ": empty response", response.getResponses().isEmpty() );

        // check response
        JaxbCommandResponse<?> cmdResp = response.getResponses().get(0);
        assertNotNull( oper + ": null command response", cmdResp );
        assertTrue( oper + ": incorrect cmd response type: " + cmdResp.getClass(), respClass.isAssignableFrom(cmdResp.getClass()) );
        
        return (T) cmdResp;
    }
    
    private static CommandWebService createDefaultClient(URL deploymentUrl) throws Exception {
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
