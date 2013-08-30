/*
 * Copyright 2011 Red Hat Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.jbpm.test.concurrency;

import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message.Level;
import org.kie.api.io.Resource;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.persistence.jpa.JPAKnowledgeService;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleSessionConcurrencyTest {

    private static Logger logger = LoggerFactory.getLogger(SingleSessionConcurrencyTest.class);

    protected static EntityManagerFactory emf;
    
    @Test
    public void testMinimalProcess() throws Exception {
        KieBase kbase = createKnowledgeBase("BPMN2-MinimalProcess.bpmn2");
        KieSession ksession = createKnowledgeSession(kbase);
        ProcessInstance processInstance = ksession.startProcess("Minimal");
        assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
    }

    protected KieBase createKnowledgeBase(String... process) throws Exception {
        Resource[] resources = new Resource[process.length];
        for (int i = 0; i < process.length; ++i) {
            String p = process[i];
            resources[i] = (ResourceFactory.newClassPathResource(p));
        }
        return createKnowledgeBaseFromResources(resources);
    }

    protected KieBase createKnowledgeBaseFromResources(Resource... process) throws Exception {

        KieServices ks = KieServices.Factory.get();
        KieRepository kr = ks.getRepository();
        if (process.length > 0) {
            KieFileSystem kfs = ks.newKieFileSystem();

            for (Resource p : process) {
                kfs.write(p);
            }

            KieBuilder kb = ks.newKieBuilder(kfs);

            kb.buildAll(); // kieModule is automatically deployed to KieRepository
                           // if successfully built.

            if (kb.getResults().hasMessages(Level.ERROR)) {
                throw new RuntimeException("Build Errors:\n" + kb.getResults().toString());
            }
        }

        KieContainer kContainer = ks.newKieContainer(kr.getDefaultReleaseId());
        return kContainer.getKieBase();
    }

    protected StatefulKnowledgeSession createKnowledgeSession(KieBase kbase, KieSessionConfiguration conf, Environment env)
            throws Exception {
        StatefulKnowledgeSession result;
        if (conf == null) {
            conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        }

        if (env == null) {
            env = createEnvironment(emf);
        }
        result = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, conf, env);
        return result;
    }
}
