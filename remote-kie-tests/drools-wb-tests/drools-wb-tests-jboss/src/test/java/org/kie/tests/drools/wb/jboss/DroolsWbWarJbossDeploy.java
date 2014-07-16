package org.kie.tests.drools.wb.jboss;

import static org.kie.tests.drools.wb.base.methods.TestConstants.PROJECT_VERSION;

import java.io.File;
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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DroolsWbWarJbossDeploy {

    private static final String classifier = "jboss-as7";

    private static Logger logger = LoggerFactory.getLogger(DroolsWbWarJbossDeploy.class);
    
    protected static WebArchive createWarWithTestDeploymentLoader(String deployName) {
        // Import kie-wb war
        File[] warFile = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("org.drools:drools-wb-distribution-wars:war:" + classifier + ":" + PROJECT_VERSION)
                .withoutTransitivity().asFile();
        
        ZipImporter zipWar = ShrinkWrap.create(ZipImporter.class, deployName + ".war").importFrom(warFile[0]);
        WebArchive war = zipWar.as(WebArchive.class);

        String [][] jarsToReplace = { 
                { "org.drools", "drools-wb-rest" },
                { "org.kie.workbench.services", "kie-wb-common-services-backend" }
        };
        
        // Replace kie-services-remote jar with the one we just generated
        for( int i = 0; i < jarsToReplace.length; ++i ) { 
           logger.info( "Deleting " + jarsToReplace[i][1] + "-" + PROJECT_VERSION + ".jar");
            war.delete("WEB-INF/lib/" + jarsToReplace[i][1] + "-" + PROJECT_VERSION + ".jar");
        }
        String [] jarsToAdd = new String[jarsToReplace.length];
        for( int i = 0; i < jarsToReplace.length; ++i ) { 
           jarsToAdd[i] = jarsToReplace[i][0] + ":" + jarsToReplace[i][1];
        }
        File [] kieRemoteDeps = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve(jarsToAdd)
                .withoutTransitivity()
                .asFile();
        for( File depFile : kieRemoteDeps ) { 
           logger.info( "Replacing with " + depFile.getName());
        }
        war.addAsLibraries(kieRemoteDeps);
        
        return war;
    }

}