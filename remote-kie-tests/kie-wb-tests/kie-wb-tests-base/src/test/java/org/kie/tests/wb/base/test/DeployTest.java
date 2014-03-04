package org.kie.tests.wb.base.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.kie.tests.wb.base.ProcessTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployTest {

    protected static final Logger logger = LoggerFactory.getLogger(DeployTest.class);
    
    @Test
    public void testDeployKjar() {
        AbstractDeploy.createAndDeployTestKJarToMaven();
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd / HH:mm:ss.SSS");

    @Test
    public void testDeployFileFlag() throws IOException {
      

    }
}
