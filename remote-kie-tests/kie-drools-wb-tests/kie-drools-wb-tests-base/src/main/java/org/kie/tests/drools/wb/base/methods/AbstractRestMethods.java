package org.kie.tests.drools.wb.base.methods;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.Inet6Address;
import java.net.URL;
import java.util.Random;
import java.util.UUID;
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
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

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
                assertEquals( "Incorrect response code: " + respStatus, status, respStatus );
                
                if( result == null && returnType != null ) { 
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
                        InputStream input = response.readEntity(InputStream.class);
                        ContentHandler handler = new BodyContentHandler();
                        boolean htmlParsed = false;
                        try { 
                            new HtmlParser().parse(input, handler, new Metadata(), new ParseContext());
                            htmlParsed = true;
                        } catch( Exception e2 ) { 
                            logger.error("]] Unable to unmarshal response: type " + responseType);
                        }
                        if( htmlParsed ) { 
                            String bodyContent = handler.toString();
                            logger.error("]] Unable to unmarshall content: \n" + bodyContent);
                            fail( "Unable to unmarshall response content [" + target.getUri().toString() + "]");
                        } 
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
        // authentication
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(userName, password));
        BasicScheme basicAuth = new BasicScheme();
        String contextId = UUID.randomUUID().toString();
        localContext.setAttribute(contextId, basicAuth);

        // user agent
        String hostname = "localhost";
        try {
            hostname = Inet6Address.getLocalHost().toString();
        } catch (Exception e) {
            // do nothing
        }
        String userAgent = "org.kie.tests.remote.client (" + idGen.getAndIncrement() + " / " + hostname + ")";

        // timeout
        timeout *= 1000;
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .setAuthenticationEnabled(true)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .setDefaultRequestConfig(config)
                .addInterceptorFirst(new PreemptiveAuth(contextId))
                .setUserAgent(userAgent)
                .build();

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
