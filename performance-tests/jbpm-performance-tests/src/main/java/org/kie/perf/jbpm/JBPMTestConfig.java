package org.kie.perf.jbpm;

import java.util.Properties;

import org.kie.perf.TestConfig;

public class JBPMTestConfig extends TestConfig {
    
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
        Properties props = super.loadProperties();
        
        runtimeManagerStrategy = props.getProperty("jbpm.runtimeManagerStrategy");

        persistence = Boolean.valueOf(props.getProperty("jbpm.persistence"));
        
        String locking = props.getProperty("jbpm.locking");
        pessimisticLocking = locking.toLowerCase().equals("pessimistic");
        
        concurrentUsersCount = Integer.valueOf(props.getProperty("jbpm.concurrentUsersCount"));

        humanTaskEager = Boolean.valueOf(props.getProperty("jbpm.ht.eager"));
        
        return props;
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
