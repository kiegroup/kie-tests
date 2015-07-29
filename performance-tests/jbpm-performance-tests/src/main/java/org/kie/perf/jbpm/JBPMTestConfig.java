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
        
        runtimeManagerStrategy = System.getProperty("jbpm.runtimeManagerStrategy");
        props.put("jbpm.runtimeManagerStrategy", runtimeManagerStrategy);

        persistence = Boolean.valueOf(System.getProperty("jbpm.persistence"));
        props.put("jbpm.persistence", persistence);
        
        String locking = System.getProperty("jbpm.locking");
        pessimisticLocking = locking.toLowerCase().equals("pessimistic");
        props.put("jbpm.pessimisticLocking", pessimisticLocking);
        
        concurrentUsersCount = Integer.valueOf(System.getProperty("jbpm.concurrentUsersCount"));
        props.put("jbpm.concurrentUsersCount", concurrentUsersCount);

        humanTaskEager = Boolean.valueOf(System.getProperty("jbpm.ht.eager"));
        props.put("jbpm.ht.eager", humanTaskEager);
        
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
