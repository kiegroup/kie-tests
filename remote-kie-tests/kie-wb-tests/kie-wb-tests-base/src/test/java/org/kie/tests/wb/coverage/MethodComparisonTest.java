package org.kie.tests.wb.coverage;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.kie.tests.wb.base.AbstractRemoteApiIntegrationTest;
import org.kie.tests.wb.base.methods.IntegrationTestMethods;
import org.kie.tests.wb.base.methods.KieWbJmsIntegrationTestMethods;
import org.kie.tests.wb.base.methods.KieWbRestIntegrationTestMethods;

public class MethodComparisonTest {

    private static final String [] skippedMethodsArr = { 
            "newBuilderInstance",
            "checkDeployFlagFile",
            "getRemoteInitialContext",
            "hashCode",
            "wait", "notify", "notifyAll",
            "getClass", "equals", "toString"
    };
    
    @Test
    public void testedOnBothJmsAndRest() { 
      
        Method [][] methods = new Method[2][];
        methods[0] = KieWbRestIntegrationTestMethods.class.getMethods();
        methods[1] = KieWbJmsIntegrationTestMethods.class.getMethods();
        
        Set<String> restMethodNames = new TreeSet<String>();
        Set<String> jmsMethodNames = new TreeSet<String>();
       
        Set [] methodNames = { restMethodNames, jmsMethodNames };
       
        Set<String> skippedMethods = new TreeSet<String>(Arrays.asList(skippedMethodsArr));
       
        for( Method method : IntegrationTestMethods.class.getMethods() ) { 
            skippedMethods.add(method.getName());
        }
        for( int i = 0; i < methods.length; ++i ) { 
           for( Method method : methods[i] ) { 
               String methodName = method.getName();
               if( skippedMethods.contains(methodName)) { 
                   continue;
               }
               methodNames[i].add(methodName);
           }
        }
       
        Set<String> restMethods = new TreeSet<String>();
        for( String methodName : restMethodNames ) { 
            if( methodName.startsWith("urls") ) { 
                restMethods.add(methodName);
                continue;
            }
           assertTrue( "[" + methodName + "] missing from JMS tests", jmsMethodNames.contains(methodName));
           restMethods.add(methodName);
        }
        
        Set<String> jmsMethods = new TreeSet<String>();
        for( String methodName : jmsMethodNames ) { 
            if( methodName.startsWith("queue") ) { 
                jmsMethods.add(methodName);
                continue;
            }
            if( "remoteApiInitiatorIdentity".equals(methodName) ) { 
                jmsMethods.add(methodName);
                continue;
            }
           assertTrue( "[" + methodName + "] missing from REST tests", restMethodNames.contains(methodName));
        }
      
        Set<String> integrationMethods = new TreeSet<String>();
        for( Method method : AbstractRemoteApiIntegrationTest.class.getMethods() ) { 
            String testName = method.getName();
            if( ! testName.startsWith("test") ) { 
               continue; 
            }
            String methodName = testName.replace("testJms", "");
            methodName = methodName.replace("testRest", "");
            methodName = methodName.substring(0,1).toLowerCase() + methodName.substring(1);
           
           assertTrue( "[" + testName + "] does not correlate to a INTEGRATION tests", restMethods.contains(methodName) || jmsMethods.contains(methodName) );
           integrationMethods.add(testName);
        }

        for( String methodName : restMethods ) { 
            if( "urlsDeployModuleForOtherTests".equals(methodName) ) { 
                continue;
            }
            String testName = "testRest" + methodName.substring(0,1).toUpperCase() + methodName.substring(1);
            assertTrue( "REST [" + methodName + "] not used in INTEGRATION tests", integrationMethods.contains(testName) );
        }
        for( String methodName : jmsMethods ) { 
            String testName = "testJms" + methodName.substring(0,1).toUpperCase() + methodName.substring(1);
            assertTrue( "JMS [" + methodName + "] not used in INTEGRATION tests", integrationMethods.contains(testName) );
        }
        
    }
}
