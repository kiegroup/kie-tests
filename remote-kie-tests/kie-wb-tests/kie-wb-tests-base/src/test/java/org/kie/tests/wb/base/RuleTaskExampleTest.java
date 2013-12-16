package org.kie.tests.wb.base;

import static org.kie.tests.wb.base.methods.AbstractIntegrationTestMethods.runRuleTaskProcess;

import org.jbpm.bpmn2.JbpmBpmn2TestCase;
import org.jbpm.process.audit.CommandBasedAuditLogService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

public class RuleTaskExampleTest extends JbpmBpmn2TestCase {

    public RuleTaskExampleTest() {
        super(true, false);
    }
    
    @BeforeClass
    public static void setup() throws Exception {
        setUpDataSource();
    }
    
    @Test
    public void runRuleTaskProcessTest() throws Exception {
        // setup
        KieBase kbase = createKnowledgeBaseWithoutDumper("repo/test/ruleTask.bpmn2", "repo/test/ruleTask.drl");
        KieSession ksession = createKnowledgeSession(kbase);

        runRuleTaskProcess(ksession, new CommandBasedAuditLogService(ksession));
    }

}
