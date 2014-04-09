package org.kie.tests.drools.wb.base.async;

import static org.junit.Assert.*;

import java.net.SocketTimeoutException;
import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractThreadTest {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractThreadTest.class);
  
    private ClientExecutor executor;
    
    public AbstractThreadTest() { 
        int timeout = 1000;
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpParams params = httpClient.getParams();
        org.apache.http.params.HttpConnectionParams.setConnectionTimeout(params, 1000);
        HttpConnectionParams.setSoTimeout(params, 1000);
        
        executor = new ApacheHttpClient4Executor(httpClient);
    }
        
    public void asyncTest(URL deploymentUrl) throws Exception { 
        ClientRequest restRequest = executor.createRequest( deploymentUrl.toExternalForm() + "rest/thread/ping");
        restRequest.accept(MediaType.APPLICATION_JSON_TYPE);
        logger.info( "= " + restRequest.getUri());
        
        ClientResponse<JaxbPingResponse> restResp = null;
        try { 
            restResp = restRequest.get();
        } catch( SocketTimeoutException ste ) { 
            fail( "Request was not executed asynchronously.");
        }
        if( 200 != restResp.getStatus() ) { 
            logger.warn( "WTF!" + restResp.getEntity(String.class) );
            assertEquals( "Incorrect REST status", restResp.getStatus(), 200 );
        }
        
        JaxbPingResponse pingResp = restResp.getEntity(JaxbPingResponse.class);
        assertNotNull( "No ping response.", pingResp );
        
        while( ! isJobDone(deploymentUrl, pingResp.id) ) { 
            logger.info( ". wait");
            Thread.currentThread().sleep(500);
        }
    }
    
    private boolean isJobDone(URL deploymentUrl, int id) throws Exception { 
        ClientRequest restRequest = executor.createRequest(deploymentUrl.toExternalForm() + "rest/thread/pung/" + id);
        restRequest.accept(MediaType.APPLICATION_JSON_TYPE);
        logger.info( "= " + restRequest.getUri());
        
        ClientResponse<JaxbPingResponse> restResp = restRequest.get();
        assertEquals( "Incorrect REST status", restResp.getStatus(), 200 );
        
        JaxbPingResponse pingResp = restResp.getEntity(JaxbPingResponse.class);
        assertNotNull( "No ping response.", pingResp );
        return "DONE".equals(pingResp.status);
    }
}
