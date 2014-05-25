package org.kie.tests.wb.base.test;

import static org.kie.tests.wb.base.methods.TestConstants.ARTIFACT_ID;
import static org.kie.tests.wb.base.methods.TestConstants.CLASSPATH_ARTIFACT_ID;
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
        
        KieModuleDeploymentHelper.newFluentInstance()
        .addResourceFilePath("repo/classpath/")
        .addClass(MyType.class)
        .setGroupId(GROUP_ID).setArtifactId(CLASSPATH_ARTIFACT_ID).setVersion(VERSION)
        .setKBaseName(KBASE_NAME).setKieSessionname(KSESSION_NAME)
        .createKieJarAndDeployToMaven();
    }
    
}
