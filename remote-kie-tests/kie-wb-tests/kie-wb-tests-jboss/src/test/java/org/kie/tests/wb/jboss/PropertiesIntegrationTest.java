package org.kie.tests.wb.jboss;

import static org.junit.Assert.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Map.Entry;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;

public class PropertiesIntegrationTest {

    @Test
    public void systemPropsTest() { 
        String [] propNames = { "jboss.server.port", "jboss.mgmt.server.port" };
        for( String name : propNames ) { 
            assertNotNull( "Null " + name, System.getProperty(name) );
        }
    }
}
