package org.kie.tests.wb.base.setup;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasourceServerSetupTask extends AbstractServerSetupTask implements ServerSetupTask {

    private static Logger logger = LoggerFactory.getLogger(DatasourceServerSetupTask.class);

    public static final String JBPM_DS_XML = "/jbpm-ds.xml";

    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        deployResource(managementClient, containerId, JBPM_DS_XML, "Deploying jbpm datasource");
    }

    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        undeployResource(managementClient, containerId, JBPM_DS_XML, "Undeployed jbpm datasource");
    }

}
