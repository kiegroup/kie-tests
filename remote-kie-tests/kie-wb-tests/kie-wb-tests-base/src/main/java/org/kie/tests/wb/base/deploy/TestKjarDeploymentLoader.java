package org.kie.tests.wb.base.deploy;

import static org.kie.tests.wb.base.methods.TestConstants.*;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jbpm.kie.services.api.Kjar;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.kie.internal.deployment.DeployedUnit;
import org.kie.internal.deployment.DeploymentService;
import org.kie.internal.deployment.DeploymentUnit.RuntimeStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class was originally used to deploy a KieModule upon start up of the arquillian tests. 
 * However, now we have the /rest/deployment methods, so we use those instead.
 * 
 *
 */
@Singleton
@Startup
public class TestKjarDeploymentLoader {

    private static final Logger logger = LoggerFactory.getLogger(TestKjarDeploymentLoader.class);

    @Inject
    @Kjar
    private DeploymentService deploymentService;

    @PostConstruct
    public void init() throws Exception {
        KModuleDeploymentUnit deploymentUnit = new KModuleDeploymentUnit(GROUP_ID, ARTIFACT_ID, VERSION, KBASE_NAME, KSESSION_NAME);
        deploymentUnit.setStrategy(RuntimeStrategy.SINGLETON);

        DeployedUnit alreadyDeployedUnit = deploymentService.getDeployedUnit(deploymentUnit.getIdentifier());
        if (alreadyDeployedUnit == null) {
            deploymentService.deploy(deploymentUnit);
        }
        logger.info("Deployed [" + deploymentUnit.getIdentifier() + "]");
    }

}
