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

import static org.kie.api.runtime.EnvironmentName.ENTITY_MANAGER_FACTORY;
import static org.kie.api.runtime.EnvironmentName.TRANSACTION_MANAGER;

import java.util.HashMap;

import javax.persistence.EntityManagerFactory;

import org.drools.core.impl.EnvironmentFactory;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.kie.jbpm.test.util.PersistenceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.TransactionManagerServices;

@RunWith(BMUnitRunner.class)
public class SingleSessionConcurrencyTest extends Assert {

    private static Logger logger = LoggerFactory.getLogger(SingleSessionConcurrencyTest.class);

    protected static HashMap<String, Object> context;

    @Before
    public void setup() {
        context = PersistenceUtil.setupWithPoolingDataSource("org.jbpm.persistence.jpa");
    }

    @Test
    @BMScript(value = "tx-wait", dir = "byteman")
    public void testMinimalProcess() throws Exception {
        Thread.currentThread().setName("test");
        TestRunnable pausesBeforeTxSyncAfterCompletion = new TestRunnable();
        Thread pausesThread = new Thread(pausesBeforeTxSyncAfterCompletion);
        pausesThread.setName("pauses");
        
        TestRunnable runsBetweenCommitAndTxSyncAfterCompletion = new TestRunnable();
        Thread runsInBetweenThread = new Thread(runsBetweenCommitAndTxSyncAfterCompletion);
        runsInBetweenThread.setName("in-between");
        
        // Start the first process
        pausesThread.start();
       
        waitForExecuteMethodToComplete();
        runsInBetweenThread.start();
        
        continueWithSynchronizationAfterCompletionMethod();
    }

    public void createSSCSExecuteRendezvous() { 
        // byteman rule called
        
    }
    public void continueWithSynchronizationAfterCompletionMethod() { 
       // byteman rule called 
    }

    public void waitForExecuteMethodToComplete() { 
        // byteman rule called
    }
    
    public static class TestRunnable implements Runnable {

        @Override
        public void run() {
            KieBase kbase = createKnowledgeBase("MinimalProcess.bpmn2");
            KieSession ksession = createKnowledgeSession(kbase);
            ProcessInstance processInstance = ksession.startProcess("minimal");
            assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
        }

    }

    public static KieBase createKnowledgeBase(String... process) {
        Resource[] resources = new Resource[process.length];
        for (int i = 0; i < process.length; ++i) {
            String p = process[i];
            resources[i] = (ResourceFactory.newClassPathResource(p));
        }
        return createKnowledgeBaseFromResources(resources);
    }

    public static KieBase createKnowledgeBaseFromResources(Resource... process) {

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

    public static StatefulKnowledgeSession createKnowledgeSession(KieBase kbase) {
        KieSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        Environment env = PersistenceUtil.createEnvironment(context);
        StatefulKnowledgeSession result = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, conf, env);
        return result;
    }

    public static Environment createEnvironment(EntityManagerFactory emf) {
        Environment env = EnvironmentFactory.newEnvironment();
        env.set(ENTITY_MANAGER_FACTORY, emf);
        env.set(TRANSACTION_MANAGER, TransactionManagerServices.getTransactionManager());

        return env;
    }
    
}