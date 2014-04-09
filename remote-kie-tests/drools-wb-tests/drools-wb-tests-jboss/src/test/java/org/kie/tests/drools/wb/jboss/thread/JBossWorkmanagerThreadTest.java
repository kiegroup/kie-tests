package org.kie.tests.drools.wb.jboss.thread;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.tests.drools.wb.base.async.AbstractThreadTest;
import org.kie.tests.drools.wb.base.async.ThreadManagerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunAsClient
@RunWith(Arquillian.class)
public class JBossWorkmanagerThreadTest extends AbstractThreadTest {

    protected static final Logger logger = LoggerFactory.getLogger(JBossWorkmanagerThreadTest.class);
    
    @Deployment(name="test")
    public static WebArchive createWar() { 
        URL webXmlUrl = JBossWorkmanagerThreadTest.class.getResource("/web.xml");
        assertNotNull("Could not find web.xml!", webXmlUrl);
       
        File [] weldDeps = Maven.resolver().offline().loadPomFromFile("pom.xml")
                .resolve( "log4j:log4j", "ch.qos.logback:logback-classic", "ch.qos.logback:logback-core",
                    "org.apache.httpcomponents:httpclient" )
                .withTransitivity().asFile();
          
        for( File dep : weldDeps ) { 
           logger.info( "' " + dep.getName()); 
        }
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "threadMgr.war")
                .addPackage(ThreadManagerResource.class.getPackage())
                .setWebXML(webXmlUrl)
                .addAsLibraries(weldDeps)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        
        return war;
    }
  
    @ArquillianResource
    URL deploymentUrl;
    
    @Test
    public void asyncTest() throws Exception {
        asyncTest(deploymentUrl);
    }
    
}