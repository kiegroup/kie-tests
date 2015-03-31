package org.kie.tests.wb.base;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.manager.RuntimeManager;
import org.simpleframework.common.buffer.Allocator;
import org.simpleframework.common.buffer.FileAllocator;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerTransportProcessor;
import org.simpleframework.transport.SocketProcessor;
import org.simpleframework.transport.TransportProcessor;
import org.simpleframework.transport.TransportSocketProcessor;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeRestServer extends JbpmJUnitBaseTestCase implements Container {

    private static final Logger logger = LoggerFactory.getLogger(FakeRestServer.class);

    private final Connection connection;
    private final SocketAddress address;
    private final int port;
    
    private RuntimeManager runtimeManager;

    public FakeRestServer() throws Exception {
        super(true, true, "org.jbpm.persistence.jpa");
        
        this.port = AvailablePortFinder.getNextAvailable(1025);
        Allocator allocator = new FileAllocator();
        TransportProcessor processor = new ContainerTransportProcessor(this, allocator, 5);
        SocketProcessor server = new TransportSocketProcessor(processor);

        this.connection = new SocketConnection(server);
        this.address = new InetSocketAddress(port);
       
        Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
                resources.put( "repo/test/evaluation.bpmn2", ResourceType.BPMN2);
                resources.put( "repo/test/groupAssignmentHumanTask.bpmn2",ResourceType.BPMN2);
                resources.put( "repo/test/humanTask.bpmn2",ResourceType.BPMN2);
                resources.put( "repo/test/humanTaskVar.bpmn2",ResourceType.BPMN2);
                resources.put( "repo/test/humanTaskWithOwnType.bpmn2",ResourceType.BPMN2);
                resources.put( "repo/test/objectVariableProcess.bpmn2",ResourceType.BPMN2);
                resources.put( "repo/test/ruleTask.bpmn2",ResourceType.BPMN2);
                resources.put( "repo/test/ruleTask.drl",ResourceType.DRL);
                resources.put( "repo/test/scriptTask.bpmn2",ResourceType.BPMN2);
                resources.put( "repo/test/singleHumanTask.bpmn2",ResourceType.BPMN2);
                resources.put( "repo/test/singleHumanTaskGroupAssignment.bpmn2",ResourceType.BPMN2);
                resources.put( "repo/test/userTask.bpmn2",ResourceType.BPMN2);
                resources.put( "repo/test/varScriptTask.bpmn2", ResourceType.BPMN2);
        
        this.runtimeManager = createRuntimeManager(
                JbpmJUnitBaseTestCase.Strategy.SINGLETON,
                resources,
                "org.test:kjar:1.0");
    }

    public int getPort() { 
       return this.port; 
    }
    
    public void start() throws Exception {
        try {
            logger.debug("Starting redirect server");
            connection.connect(address);
        } finally {
            logger.debug("Started redirect server");
        }
    }

    public void stop() throws Exception {
        connection.close();
    }

    public void handle( Request req, Response resp ) {
        try {
            String address = req.getAddress().toString();
            logger.info( "> " + address);
          
            if( address.endsWith("/deployment/") ) { 
                
            }
            
            resp.setCode(HttpURLConnection.HTTP_BAD_REQUEST);

            PrintStream out = resp.getPrintStream(1024);
            out.print("");
            out.close();
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }

    public static String readInputStreamAsString( InputStream in ) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(in);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result = bis.read();
        while( result != -1 ) {
            byte b = (byte) result;
            buf.write(b);
            result = bis.read();
        }
        return buf.toString();
    }
}
