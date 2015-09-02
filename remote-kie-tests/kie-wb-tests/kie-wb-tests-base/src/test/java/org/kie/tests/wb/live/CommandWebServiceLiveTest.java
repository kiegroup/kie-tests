package org.kie.tests.wb.live;

import java.net.URL;

import org.junit.Test;
import org.kie.remote.tests.base.unit.GetIgnoreRule.IgnoreIfGETFails;
import org.kie.tests.wb.base.methods.KieWbWebServicesIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandWebServiceLiveTest {

   protected static final Logger logger = LoggerFactory.getLogger(CommandWebServiceLiveTest.class);
    
    private final boolean weblogic;
    
    private URL deploymentUrl;
    
    public CommandWebServiceLiveTest() { 
        weblogic = false;
        {
            // Modify this string to match your kie-wb/BPMS installation
            String urlString;
            if( weblogic ) { 
                urlString = "http://localhost:7001/kie-wb-6.2.1-SNAPSHOT-weblogic12/";
            } else { 
                urlString = "http://localhost:8080/kie-wb/";
            }
            try { 
                deploymentUrl = new URL(urlString);
            } catch( Exception e ) { 
                System.err.println( "The following URL is not a valid URL: '" + urlString + "'");
                e.printStackTrace();
            }
        }
    }

    protected void printTestName() { 
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println( "-=> " + testName );
    }
    
    @Test
    @IgnoreIfGETFails(getUrl="http://localhost:8080/kie-wb/rest/deployment")
    public void wsTest() throws Exception { 
        KieWbWebServicesIntegrationTestMethods wsTests = new KieWbWebServicesIntegrationTestMethods(); 
        wsTests.startSimpleProcess(deploymentUrl);
    }
}
