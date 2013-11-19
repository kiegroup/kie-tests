package org.kie.tests.wb.base;

import org.junit.Test;
import org.kie.tests.wb.base.deploy.TestKjarDeploymentLoader;

public class TestKjarDeploymentLoaderTest {

    @Test
    public void deployTest() { 
        TestKjarDeploymentLoader.deployKjarToMaven();
    }
}
