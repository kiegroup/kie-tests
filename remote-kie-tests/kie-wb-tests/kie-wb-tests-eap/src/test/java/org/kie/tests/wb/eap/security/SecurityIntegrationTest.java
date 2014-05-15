/*
 * JBoss, Home of Professional Open Source
 * 
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.tests.wb.eap.security;

import static org.kie.tests.wb.base.methods.TestConstants.projectVersion;

import java.io.File;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@RunWith(Arquillian.class)
@Ignore
public class SecurityIntegrationTest {

    protected static final Logger logger = LoggerFactory.getLogger(SecurityIntegrationTest.class);
    
    @Deployment(name = "kie-wb-security")
    public static Archive<?> createWar() {
        return createTestWar("eap-6_1");
    }

    static WebArchive createTestWar(String classifier) {
        logger.info( "] import");
        // Import kie-wb war
        File [] warFile = 
                Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("org.kie:kie-wb-distribution-wars:war:" + classifier + ":" + projectVersion )
                .withoutTransitivity()
                .asFile();
        ZipImporter zipWar = ShrinkWrap.create(ZipImporter.class, "test.war").importFrom(warFile[0]);
        
        logger.info( "] transform");
        WebArchive war = zipWar.as(WebArchive.class);
        
        // Add kjar deployer
        logger.info( "] add classes");
        war.addClasses(SecurityBean.class, UserPassCallbackHandler.class);
     
        boolean addDeps = false;
        if( addDeps ) { 
            // Add dependencies
            logger.info( "] add dependencies");
            // Replace kie-services-remote jar with the one we just generated
            String [][] jarsToReplace = { 
                    { "org.jboss.as", "jboss-as-server" }
            };
            String [] jarsArg = new String[jarsToReplace.length];
            for( int i = 0; i < jarsToReplace.length; ++i ) { 
                jarsArg[i] = jarsToReplace[i][0] + ":" + jarsToReplace[i][1];
            }

            File [] secTestDeps = Maven.resolver()
                    .loadPomFromFile("pom.xml")
                    .resolve(jarsArg)
                    .withTransitivity()
                    .asFile();
            for( File file : secTestDeps ) { 
                logger.info( "Adding " + file.getName());
            }
            war.addAsLibraries(secTestDeps);
        }
      
        logger.info( "] done");
        return war;
    }
   
    @EJB
    private SecurityBean securityBean;
    
    @Test
    public void securityTest() throws Exception { 
        logger.info("-->");
        securityBean.explore();
        logger.info("<--");
    }
    
    
}
