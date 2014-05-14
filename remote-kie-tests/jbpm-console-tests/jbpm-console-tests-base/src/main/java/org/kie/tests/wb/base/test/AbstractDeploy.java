package org.kie.tests.wb.base.test;

import static org.kie.tests.wb.base.methods.TestConstants.ARTIFACT_ID;
import static org.kie.tests.wb.base.methods.TestConstants.GROUP_ID;
import static org.kie.tests.wb.base.methods.TestConstants.KBASE_NAME;
import static org.kie.tests.wb.base.methods.TestConstants.KSESSION_NAME;
import static org.kie.tests.wb.base.methods.TestConstants.VERSION;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

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
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.kie.services.client.deployment.KieModuleDeploymentHelper;
import org.kie.tests.wb.base.test.objects.MyType;
import org.kie.tests.wb.base.test.objects.Person;
import org.kie.tests.wb.base.test.objects.Request;

public class AbstractDeploy {

    protected static void createAndDeployTestKJarToMaven() { 
        KieModuleDeploymentHelper.newFluentInstance()
       .addResourceFilePath("repo/test/")
       .addClass(MyType.class, Person.class, Request.class)
       .setGroupId(GROUP_ID).setArtifactId(ARTIFACT_ID).setVersion(VERSION)
       .setKBaseName(KBASE_NAME).setKieSessionname(KSESSION_NAME)
       .createKieJarAndDeployToMaven();
    }
    
    protected static ClientRequestFactory createBasicAuthRequestFactory(URL deploymentUrl, String user, String password) throws URISyntaxException { 
        BasicHttpContext localContext = new BasicHttpContext();
        HttpClient preemptiveAuthClient = createPreemptiveAuthHttpClient(user, password, 10, localContext);
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
