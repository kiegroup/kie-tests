package org.kie.tests.wb.base.test;

import java.text.SimpleDateFormat;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployTest {

    protected static final Logger logger = LoggerFactory.getLogger(DeployTest.class);
    
    @Test
    public void testDeployKjar() {
        AbstractDeploy.createAndDeployTestKJarToMaven();
    }

}
