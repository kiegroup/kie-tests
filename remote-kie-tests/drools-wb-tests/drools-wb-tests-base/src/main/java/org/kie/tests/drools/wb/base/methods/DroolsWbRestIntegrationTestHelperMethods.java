package org.kie.tests.drools.wb.base.methods;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DroolsWbRestIntegrationTestHelperMethods {

    private static Logger logger = LoggerFactory.getLogger(DroolsWbRestIntegrationTestMethods.class);
   
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    
    protected static ClientResponse<?> checkTimeResponse(ClientResponse<?> responseObj) throws Exception {
        long start = System.currentTimeMillis();
        try { 
            return checkResponse(responseObj, 202); 
        } finally { 
           long duration = System.currentTimeMillis() - start;
           assertTrue( "Rest call took too long: " + duration + "ms", duration < 500);
           logger.info("Op time : " + sdf.format(new Date(duration)));
        }
    }
    
    protected static ClientResponse<?> checkResponse(ClientResponse<?> responseObj) throws Exception {
        return checkResponse(responseObj, 200);
    }
    
    protected static ClientResponse<?> checkResponse(ClientResponse<?> responseObj, int expStatus) throws Exception {
        logger.debug("<< Response received");
        responseObj.resetStream();
        int status = responseObj.getStatus(); 
        if( status != expStatus ) { 
            logger.warn("Response with exception:\n" + responseObj.getEntity(String.class));
            assertEquals( "Status ACCEPTED", expStatus, status);
        } 
        return responseObj;
    }
    
    protected static ClientRequest createRequest(ClientRequestFactory requestFactory, String urlString) { 
        ClientRequest restRequest = requestFactory.createRequest(urlString);
        restRequest.accept(MediaType.APPLICATION_JSON_TYPE);
        logger.debug( ">> " + urlString);
        return restRequest;
    }
    
    protected static void addToRequestBody(ClientRequest restRequest, Object obj) throws Exception { 
        String body = convertObjectToJsonString(obj);
        logger.debug( "]] " + body );
        restRequest.body(MediaType.APPLICATION_JSON_TYPE, body);
    }
    
    private static ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS);
    }
        
    protected static String convertObjectToJsonString(Object object) throws JsonGenerationException, JsonMappingException, IOException {
        return mapper.writeValueAsString(object);
    }
        
    protected static Object convertJsonStringToObject(String jsonStr, Class<?> type) throws JsonParseException, JsonMappingException, IOException {
        return mapper.readValue(jsonStr, type);
    }
    
    protected static ClientRequestFactory createBasicAuthRequestFactory(URL deploymentUrl, String user, String password) throws URISyntaxException { 
        BasicHttpContext localContext = new BasicHttpContext();
        HttpClient preemptiveAuthClient = createPreemptiveAuthHttpClient(user, password, 15, localContext);
        ClientExecutor clientExecutor = new ApacheHttpClient4Executor(preemptiveAuthClient, localContext);
        return new ClientRequestFactory(clientExecutor, deploymentUrl.toURI());
    }
    
    protected static DefaultHttpClient createPreemptiveAuthHttpClient(String userName, String password, int timeout, BasicHttpContext localContext) {
        BasicHttpParams params = new BasicHttpParams();
        int timeoutMilliSeconds = timeout * 1000;
        HttpConnectionParams.setConnectionTimeout(params, timeoutMilliSeconds);
        HttpConnectionParams.setSoTimeout(params, timeoutMilliSeconds);
        DefaultHttpClient client = new DefaultHttpClient(params);

        if (userName != null && !"".equals(userName)) {
            client.getCredentialsProvider().setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(userName, password));
            // Generate BASIC scheme object and stick it to the local execution context
            BasicScheme basicAuth = new BasicScheme();
            String contextId = UUID.randomUUID().toString();
            localContext.setAttribute(contextId, basicAuth);

            // Add as the first request interceptor
            client.addRequestInterceptor(new PreemptiveAuth(contextId), 0);
        }

        // set the following user agent with each request
        String userAgent = "ArtifactoryBuildClient/" + 1;
        HttpProtocolParams.setUserAgent(client.getParams(), userAgent);
        return client;
    }
    
    static class PreemptiveAuth implements HttpRequestInterceptor {
        
        private final String contextId;
        public PreemptiveAuth(String contextId) { 
            this.contextId = contextId;
        }
        
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {

            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

            // If no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context.getAttribute(contextId);
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                    if (creds == null) {
                        throw new HttpException("No credentials for preemptive authentication");
                    }
                    authState.update(authScheme, creds);
                }
            }
        }
    }
}
