package org.kie.tests.wb.base.test;

import static org.kie.tests.wb.base.methods.TestConstants.ARTIFACT_ID;
import static org.kie.tests.wb.base.methods.TestConstants.CLASSPATH_ARTIFACT_ID;
import static org.kie.tests.wb.base.methods.TestConstants.GROUP_ID;
import static org.kie.tests.wb.base.methods.TestConstants.KBASE_NAME;
import static org.kie.tests.wb.base.methods.TestConstants.KSESSION_NAME;
import static org.kie.tests.wb.base.methods.TestConstants.VERSION;

import org.kie.api.builder.helper.KieModuleDeploymentHelper;
import org.kie.tests.wb.base.test.objects.MyType;
import org.kie.tests.wb.base.test.objects.Person;
import org.kie.tests.wb.base.test.objects.Request;

public class AbstractDeploy {

    protected static void createAndDeployTestKJarToMaven() { 
        KieModuleDeploymentHelper.newFluentInstance()
       .addResourceFilePath("repo/test/")
       .addClass(MyType.class, Person.class, Request.class)
       .setGroupId(GROUP_ID).setArtifactId(ARTIFACT_ID).setVersion(VERSION)
       .setKBaseName(KBASE_NAME).setKieSessionname(KSESSION_NAME)
       .createKieJarAndDeployToMaven();
        
        KieModuleDeploymentHelper.newFluentInstance()
        .addResourceFilePath("repo/classpath/")
        .addClass(MyType.class)
        .setGroupId(GROUP_ID).setArtifactId(CLASSPATH_ARTIFACT_ID).setVersion(VERSION)
        .setKBaseName(KBASE_NAME).setKieSessionname(KSESSION_NAME)
        .createKieJarAndDeployToMaven();
    }
    
}
