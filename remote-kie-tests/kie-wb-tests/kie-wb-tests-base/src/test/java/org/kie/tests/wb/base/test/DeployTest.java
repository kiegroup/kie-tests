package org.kie.tests.wb.base.test;

import org.junit.Test;

public class DeployTest {

    @Test
    public void testDeployKjar() { 
       AbstractDeploy.createAndDeployTestKJarToMaven(); 
    }
}
