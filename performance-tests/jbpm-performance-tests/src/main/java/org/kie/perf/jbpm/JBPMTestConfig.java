package org.kie.perf.jbpm;

import java.util.Properties;

import org.kie.api.runtime.process.ProcessInstance;
import org.kie.perf.TestConfig;

public class JBPMTestConfig extends TestConfig {
    
    protected String version = ProcessInstance.class.getPackage().getImplementationVersion();

    protected String runtimeManagerStrategy;

    protected boolean persistence;

    protected boolean pessimisticLocking;

    protected int concurrentUsersCount;

    protected boolean humanTaskEager;

    protected JBPMTestConfig() {

    }

    public static JBPMTestConfig getInstance() {
        if (tc == null || !(tc instanceof JBPMTestConfig)) {
            tc = new JBPMTestConfig();
            try {
                tc.loadProperties();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return (JBPMTestConfig) tc;
    }

    @Override
    public Properties loadProperties() throws Exception {
        super.loadProperties();

        databaseName = System.getProperty("databaseName");
        properties.put("databaseName", databaseName);
        addTag(databaseName);

        runtimeManagerStrategy = System.getProperty("jbpm.runtimeManagerStrategy");
        properties.put("jbpm.runtimeManagerStrategy", runtimeManagerStrategy);
        addTag(runtimeManagerStrategy);

        persistence = Boolean.valueOf(System.getProperty("jbpm.persistence"));
        properties.put("jbpm.persistence", persistence);

        String locking = System.getProperty("jbpm.locking");
        pessimisticLocking = locking.toLowerCase().equals("pessimistic");
        properties.put("jbpm.pessimisticLocking", pessimisticLocking);

        concurrentUsersCount = Integer.valueOf(System.getProperty("jbpm.concurrentUsersCount"));
        properties.put("jbpm.concurrentUsersCount", concurrentUsersCount);

        humanTaskEager = Boolean.valueOf(System.getProperty("jbpm.ht.eager"));
        properties.put("jbpm.ht.eager", humanTaskEager);
        
        properties.put("suite.version", version);
        addTag(version);

        return properties;
    }

    public String getRuntimeManagerStrategy() {
        return runtimeManagerStrategy;
    }

    public boolean isPersistence() {
        return persistence;
    }

    public boolean isPessimisticLocking() {
        return pessimisticLocking;
    }

    public int getConcurrentUsersCount() {
        return concurrentUsersCount;
    }

    public boolean isHumanTaskEager() {
        return humanTaskEager;
    }

}
