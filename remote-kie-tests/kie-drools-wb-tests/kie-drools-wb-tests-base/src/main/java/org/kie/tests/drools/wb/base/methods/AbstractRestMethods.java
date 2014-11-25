package org.kie.tests.drools.wb.base.methods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.Inet6Address;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRestMethods {
 
    private static Logger logger = LoggerFactory.getLogger(AbstractRestMethods.class);

    private Client client;
    private static final AtomicInteger idGen = new AtomicInteger(new Random().nextInt());
    
    public abstract MediaType getMediaType();
    
    public abstract URL getDeploymentUrl();
    
    private enum Operation { 
        GET, DELETE, POST;
    }
   
    // GET
    
    public <T,E> T get(String relativeUrl, Class<T> returnType) { 
        return get(relativeUrl, 200, returnType);
    }
    
    public <T,E> T get(String relativeUrl, Class<T> returnType, Class<E> genericType) { 
       return get(relativeUrl, 200, returnType, genericType);
    }
    
    public <T,E> T get(String relativeUrl, int status, Class<T> returnType) { 
        return get(relativeUrl, status, returnType, null);
    }
   
    public <T,E> T get(String relativeUrl, int status, final Class<T> returnType, final Class<E> genericType) { 
        return process(Operation.GET, relativeUrl, status, returnType, genericType);
    }
    
   // POST
    
    public <T> void post(String relativeUrl, int status, T input) { 
       post(relativeUrl, status, input, null);
    }
    
    public <I,R> R post(String relativeUrl, int status, I inputObject, Class<R> returnType) { 
        return post(relativeUrl, status, inputObject, returnType, Integer.MAX_VALUE);
    }
    
    public <I,R> R post(String relativeUrl, int status, Class<R> returnType, int timeout) { 
        return post(relativeUrl, status, null, returnType, timeout);
    }
   
    public <I,R> R post(String relativeUrl, int status, I inputObject, Class<R> returnType, int timeout) { 
        return process(Operation.POST, relativeUrl, status, inputObject, returnType, null, timeout);
    }
   
    // DELETE
    
    public <T,E> T delete(String relativeUrl, int status, Class<T> returnType) { 
        return process(Operation.DELETE, relativeUrl, status, returnType, null);
    }
    
    public <T,E> T delete(String relativeUrl, int status, Class<T> returnType, int timeout) { 
        return process(Operation.DELETE, relativeUrl, status, null, returnType, null, timeout);
    }

    // process
    
    public <T,E> T process(Operation op, String relativeUrl, int status, final Class<T> returnType, final Class<E> genericType) { 
        return process(op, relativeUrl, status, null, returnType, genericType, Integer.MAX_VALUE);
    }
    
    public <T,I,E> T process(Operation op, String relativeUrl, int status, I inputObject, final Class<T> returnType, final Class<E> genericType, int timeout) { 
        T result = null;
       
        ParameterizedType paramType = null;
        if( genericType != null ) { 
            paramType = new ParameterizedType() {
                @Override
                public Type getRawType() { return returnType; }

                @Override
                public Type[] getActualTypeArguments() {
                    Type [] typeArgs = { genericType };
                    return typeArgs;
                }

                @Override
                public Type getOwnerType() { return null; }
            };
        } 
        
        StringBuilder urlBuilder = new StringBuilder(getDeploymentUrl().toExternalForm());
        urlBuilder.append("rest/").append(relativeUrl);
        WebTarget target = client.target(urlBuilder.toString());

        Invocation.Builder linkBuilder;
        if( returnType != null ) { 
            linkBuilder = target.request(getMediaType());
        } else { 
            linkBuilder = target.request();
        }


     
        Response response = null;
        try {
            try { 
                switch(op) { 
                case GET:
                    logger.info("]] [GET] " + target.getUri().toString() );
                    response = linkBuilder.get();
                    break;
                case POST:
                    logger.info("]] [POST] " + target.getUri().toString() );
                    if( inputObject != null ) { 
                        Entity entity = Entity.entity(inputObject, getMediaType());
                        if( returnType == null ) { 
                            response = linkBuilder.post(entity);
                        } else { 
                            if( genericType != null ) { 
                                result = (T) linkBuilder.post(entity, new GenericType(paramType));
                            } else { 
                                result = linkBuilder.post(entity, returnType);
                            }
                        }
                    } else { 
                        response = linkBuilder.post(null);
                    }
                    break;
                case DELETE:
                    logger.info("]] [DELETE] " + target.getUri().toString() );
                    response = linkBuilder.delete();
                    break;
                default: 
                    throw new IllegalStateException( "Unknown operation: " + op );
                }
            } catch( Exception e ) { 
                e.printStackTrace(); 
            }

            if( response != null ) { 
                int respStatus = response.getStatus();
                if( respStatus >= 300 && response.getMediaType().toString().startsWith(MediaType.TEXT_HTML) ) { 
                    String bodyContent = getHtmlBodyContent(response);
                    logger.error("]] Request failed:\n" + bodyContent);
                    fail( "Request failed with status [" + respStatus + "], see log." );
                } else if( result == null && returnType != null ) { 
                    assertEquals( "Incorrect response code: " + respStatus, status, respStatus );
                    try { 
                        response.bufferEntity();
                        if( paramType != null ) { 
                            result = (T) response.readEntity(new GenericType(paramType));
                        } else { 
                            result = (T) response.readEntity(returnType);
                        }
                    } catch( Exception e ) { 
                        MediaType responseType = response.getMediaType();
                        if( responseType.toString().startsWith(MediaType.TEXT_HTML) ) { 
                            String bodyContent = getHtmlBodyContent(response);
                            logger.error("]] Unable to unmarshall content: \n" + bodyContent);
                            fail( "Unable to unmarshall response content [" + target.getUri().toString() + "]");
                        }
                        else { 
                            fail( "Unable to unmarshall response content [" + target.getUri().toString() + "] : " + e.getMessage() );
                        }
                    }
                }
            }
        } finally { 
            if( response != null ) { 
                response.close();  
            }
        }
        return result;
    }

    private String getHtmlBodyContent(Response response) { 
        String bodyContent = response.readEntity(String.class);
        boolean htmlParsed = false;
        try { 
            Document doc = Jsoup.parse(bodyContent);
            bodyContent = doc.body().text();
        } catch( Exception e2 ) { 
            logger.error("]] Unable to unmarshal response: type " + response.getMediaType());
        } 
        return bodyContent;
    }
    
    /**
     * Creates an request factory that authenticates using the given username and password
     *
     * @param url
     * @param username
     * @param password
     * @param timeout
     *
     * @return A request factory that can be used to send (authenticating) requests to REST services
     */
    public void setupClient(String username, String password, int timeout) {
        BasicHttpContext localContext = new BasicHttpContext();
        HttpClient preemptiveAuthClient = createPreemptiveAuthHttpClient(username, password, timeout, localContext);

        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(preemptiveAuthClient, localContext);
        client = new ResteasyClientBuilder().httpEngine(engine).build();
    }

    /**
     * This method is used in order to create the authenticating REST client factory.
     *
     * @param userName
     * @param password
     * @param timeout
     * @param localContext
     *
     * @return A {@link DefaultHttpClient} instance that will authenticate using the given username and password.
     */
    private static HttpClient createPreemptiveAuthHttpClient(String userName, String password, int timeout, HttpContext localContext) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        BasicHttpParams httpParams = new BasicHttpParams();
        
        String hostname = "localhost";
        try {
            hostname = Inet6Address.getLocalHost().toString();
        } catch (Exception e) {
            // do nothing
        }
        
        // authentication
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(new HttpHost(hostname, 8080), basicAuth);
        localContext.setAttribute(ClientContext.AUTH_CACHE, authCache);

        // user agent
        String userAgent = "org.kie.tests.remote.client (" + idGen.getAndIncrement() + " / " + hostname + ")";
        
        // credentials 
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(userName, password));
        httpClient.setCredentialsProvider(credsProvider);
       
        // timeouts
        timeout *= 1000;
        httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
        httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
        httpParams.setParameter(CoreProtocolPNames.USER_AGENT, userAgent);

        httpClient.setParams(httpParams);
        return httpClient; 
    }
    
    /**
     * This class is used in order to effect preemptive authentication in the REST request factory.
     */
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
