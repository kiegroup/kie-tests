package org.kie.perf.jbpm;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.InitialContext;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.drools.persistence.jta.JtaTransactionManager;
import org.jbpm.runtime.manager.impl.DefaultRegisterableItemsFactory;
import org.jbpm.runtime.manager.impl.jpa.EntityManagerFactoryManager;
import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironment;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.task.TaskLifeCycleEventListener;
import org.kie.api.task.UserGroupCallback;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;
import org.kie.perf.TestConfig;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class JBPMController {
    
    public enum Strategy {
        SINGLETON,
        PERREQUEST,
        PERPROCESSINSTANCE;
    }

    protected static boolean persistence = false;
    private String persistenceUnitName = "org.jbpm.persistence.jpa";

    private EntityManagerFactory emf;
    private PoolingDataSource ds;

    private RuntimeManagerFactory managerFactory = RuntimeManagerFactory.Factory.get();
    protected RuntimeManager manager;
    protected Strategy strategy;

    protected UserGroupCallback userGroupCallback = new JBossUserGroupCallbackImpl("classpath:/usergroups.properties");
    
    protected Map<String, WorkItemHandler> customHandlers = new HashMap<String, WorkItemHandler>();
    protected List<ProcessEventListener> customProcessListeners = new ArrayList<ProcessEventListener>();
    protected List<AgendaEventListener> customAgendaListeners = new ArrayList<AgendaEventListener>();
    protected List<TaskLifeCycleEventListener> customTaskListeners = new ArrayList<TaskLifeCycleEventListener>();
    protected Map<String, Object> customEnvironmentEntries = new HashMap<String, Object>();
    
    private static JBPMController instance;

    private JBPMController() {
        persistence = JBPMTestConfig.getInstance().isPersistence();
    }
    
    public static JBPMController getInstance() {
        if (instance == null) {
            instance = new JBPMController();
            try {
                instance.setUp();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return instance;
    }

    public void setUp() throws Exception {
        if (persistence) {
            ds = setupPoolingDataSource();
            emf = Persistence.createEntityManagerFactory(persistenceUnitName);
        }
        cleanupSingletonSessionId();

    }

    public void tearDown() {
        clearCustomEntries();
        disposeRuntimeManager();
        if (persistence) {
            if (emf != null) {
                emf.close();
                emf = null;
                EntityManagerFactoryManager.get().clear();

            }
            if (ds != null) {
                ds.close();
                ds = null;
            }
            try {
                InitialContext context = new InitialContext();
                UserTransaction ut = (UserTransaction) context.lookup( JtaTransactionManager.DEFAULT_USER_TRANSACTION_NAME );
                if( ut.getStatus() != Status.STATUS_NO_TRANSACTION ) {
                    ut.setRollbackOnly();
                    ut.rollback();
                }
            } catch( Exception e ) {
                // do nothing
            }
        }
    }
    
    public void addProcessEventListener(ProcessEventListener listener) {
        customProcessListeners.add(listener);
    }

    public void addAgendaEventListener(AgendaEventListener listener) {
        customAgendaListeners.add(listener);
    }

    public void addTaskEventListener(TaskLifeCycleEventListener listener) {
        customTaskListeners.add(listener);
    }

    public void addWorkItemHandler(String name, WorkItemHandler handler) {
        customHandlers.put(name, handler);
    }
    
    public void addEnvironmentEntry(String name, Object value) {
        customEnvironmentEntries.put(name, value);
    }
    
    public void setUserGroupCallback(UserGroupCallback userGroupCallback) {
        this.userGroupCallback = userGroupCallback;
    }

    public void clearCustomEntries() {
        this.customAgendaListeners.clear();
        this.customHandlers.clear();
        this.customProcessListeners.clear();
        this.customTaskListeners.clear();
        this.customEnvironmentEntries.clear();
        this.userGroupCallback = new JBossUserGroupCallbackImpl("classpath:/usergroups.properties");
    }
    
    protected PoolingDataSource setupPoolingDataSource() {
        PoolingDataSource pds = new PoolingDataSource();
        pds.setUniqueName("jdbc/jbpm-ds");
        pds.setClassName("bitronix.tm.resource.jdbc.lrc.LrcXADataSource");
        pds.setMaxPoolSize(5);
        pds.setAllowLocalTransactions(true);
        pds.getDriverProperties().put("user", "sa");
        pds.getDriverProperties().put("password", "");
        pds.getDriverProperties().put("url", "jdbc:h2:mem:jbpm-db;MVCC=true");
        pds.getDriverProperties().put("driverClassName", "org.h2.Driver");
        pds.init();
        return pds;
    }

    /**
     * Creates default configuration of <code>RuntimeManager</code> with SINGLETON strategy and all
     * <code>processes</code> being added to knowledge base.
     * <br/>
     * There should be only one <code>RuntimeManager</code> created during single test.
     * @param process - processes that shall be added to knowledge base
     * @return new instance of RuntimeManager
     */
    public RuntimeManager createRuntimeManager(String... process) {
        return createRuntimeManager(Strategy.valueOf(JBPMTestConfig.getInstance().getRuntimeManagerStrategy().toUpperCase()), null, process);
    }

    /**
     * Creates default configuration of <code>RuntimeManager</code> with given <code>strategy</code> and all
     * <code>processes</code> being added to knowledge base.
     * <br/>
     * There should be only one <code>RuntimeManager</code> created during single test.
     * @param strategy - selected strategy of those that are supported
     * @param identifier - identifies the runtime manager
     * @param process - processes that shall be added to knowledge base
     * @return new instance of RuntimeManager
     */
    public RuntimeManager createRuntimeManager(Strategy strategy, String identifier, String... process) {
        Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
        for (String p : process) {
            resources.put(p, ResourceType.BPMN2);
        }
        return createRuntimeManager(strategy, resources, identifier);
    }

    /**
     * Creates default configuration of <code>RuntimeManager</code> with SINGLETON strategy and all
     * <code>resources</code> being added to knowledge base.
     * <br/>
     * There should be only one <code>RuntimeManager</code> created during single test.
     * @param resources - resources (processes, rules, etc) that shall be added to knowledge base
     * @return new instance of RuntimeManager
     */
    public RuntimeManager createRuntimeManager(Map<String, ResourceType> resources) {
        return createRuntimeManager(Strategy.valueOf(JBPMTestConfig.getInstance().getRuntimeManagerStrategy().toUpperCase()), resources, null);
    }

    /**
     * Creates default configuration of <code>RuntimeManager</code> with SINGLETON strategy and all
     * <code>resources</code> being added to knowledge base.
     * <br/>
     * There should be only one <code>RuntimeManager</code> created during single test.
     * @param resources - resources (processes, rules, etc) that shall be added to knowledge base
     * @param identifier - identifies the runtime manager
     * @return new instance of RuntimeManager
     */
    protected RuntimeManager createRuntimeManager(Map<String, ResourceType> resources, String identifier) {
        return createRuntimeManager(Strategy.valueOf(JBPMTestConfig.getInstance().getRuntimeManagerStrategy().toUpperCase()), resources, identifier);
    }

    /**
     * Creates default configuration of <code>RuntimeManager</code> with given <code>strategy</code> and all
     * <code>resources</code> being added to knowledge base.
     * <br/>
     * There should be only one <code>RuntimeManager</code> created during single test.
     * @param strategy - selected strategy of those that are supported
     * @param resources - resources that shall be added to knowledge base
     * @return new instance of RuntimeManager
     */
    protected RuntimeManager createRuntimeManager(Strategy strategy, Map<String, ResourceType> resources) {
        return createRuntimeManager(strategy, resources, null);
    }

    /**
     * Creates default configuration of <code>RuntimeManager</code> with given <code>strategy</code> and all
     * <code>resources</code> being added to knowledge base.
     * <br/>
     * There should be only one <code>RuntimeManager</code> created during single test.
     * @param strategy - selected strategy of those that are supported
     * @param resources - resources that shall be added to knowledge base
     * @param identifier - identifies the runtime manager
     * @return new instance of RuntimeManager
     */
    protected RuntimeManager createRuntimeManager(Strategy strategy, Map<String, ResourceType> resources, String identifier) {
        if (manager != null) {
            throw new IllegalStateException("There is already one RuntimeManager active");
        }

        RuntimeEnvironmentBuilder builder = null;
        if (persistence) {
            builder = RuntimeEnvironmentBuilder.Factory.get()
                    .newDefaultBuilder()
            .entityManagerFactory(emf)
            .registerableItemsFactory(new DefaultRegisterableItemsFactory() {

                @Override
                public Map<String, WorkItemHandler> getWorkItemHandlers(RuntimeEngine runtime) {
                    Map<String, WorkItemHandler> handlers = new HashMap<String, WorkItemHandler>();
                    handlers.putAll(super.getWorkItemHandlers(runtime));
                    handlers.putAll(customHandlers);
                    return handlers;
                }

                @Override
                public List<ProcessEventListener> getProcessEventListeners(RuntimeEngine runtime) {
                    List<ProcessEventListener> listeners = super.getProcessEventListeners(runtime);
                    listeners.addAll(customProcessListeners);
                    return listeners;
                }

                @Override
                public List<AgendaEventListener> getAgendaEventListeners( RuntimeEngine runtime) {
                    List<AgendaEventListener> listeners = super.getAgendaEventListeners(runtime);
                    listeners.addAll(customAgendaListeners);
                    return listeners;
                }

                @Override
                public List<TaskLifeCycleEventListener> getTaskListeners() {
                    List<TaskLifeCycleEventListener> listeners = super.getTaskListeners();
                    listeners.addAll(customTaskListeners);
                    return listeners;
                }

            });
        } else {
            builder = RuntimeEnvironmentBuilder.Factory.get()
                    .newDefaultInMemoryBuilder()
                    .registerableItemsFactory(new DefaultRegisterableItemsFactory() {

                @Override
                public Map<String, WorkItemHandler> getWorkItemHandlers(RuntimeEngine runtime) {
                    Map<String, WorkItemHandler> handlers = new HashMap<String, WorkItemHandler>();
                    handlers.putAll(super.getWorkItemHandlers(runtime));
                    handlers.putAll(customHandlers);
                    return handlers;
                }

                @Override
                public List<ProcessEventListener> getProcessEventListeners(RuntimeEngine runtime) {
                    List<ProcessEventListener> listeners = super.getProcessEventListeners(runtime);
                    listeners.addAll(customProcessListeners);
                    return listeners;
                }

                @Override
                public List<AgendaEventListener> getAgendaEventListeners( RuntimeEngine runtime) {
                    List<AgendaEventListener> listeners = super.getAgendaEventListeners(runtime);
                    listeners.addAll(customAgendaListeners);
                    return listeners;
                }

                @Override
                public List<TaskLifeCycleEventListener> getTaskListeners() {
                    List<TaskLifeCycleEventListener> listeners = super.getTaskListeners();
                    listeners.addAll(customTaskListeners);
                    return listeners;
                }

            });
        }

        builder.addEnvironmentEntry(EnvironmentName.USE_PESSIMISTIC_LOCKING, JBPMTestConfig.getInstance().isPessimisticLocking());
        builder.userGroupCallback(userGroupCallback);
        
        for (Entry<String, Object> envEntry : customEnvironmentEntries.entrySet()) {
            builder.addEnvironmentEntry(envEntry.getKey(), envEntry.getValue());
        }

        for (Map.Entry<String, ResourceType> entry : resources.entrySet()) {
            builder.addAsset(ResourceFactory.newClassPathResource(entry.getKey()), entry.getValue());
        }

        return createRuntimeManager(strategy, resources, builder.get(), identifier);
    }

    /**
     * The lowest level of creation of <code>RuntimeManager</code> that expects to get <code>RuntimeEnvironment</code>
     * to be given as argument. It does not assume any particular configuration as it's considered manual creation
     * that allows to configure every single piece of <code>RuntimeManager</code>. <br/>
     * Use this only when you know what you do!
     * @param strategy - selected strategy of those that are supported
     * @param resources - resources that shall be added to knowledge base
     * @param environment - runtime environment used for <code>RuntimeManager</code> creation
     * @param identifier - identifies the runtime manager
     * @return new instance of RuntimeManager
     */
    protected RuntimeManager createRuntimeManager(Strategy strategy, Map<String, ResourceType> resources, RuntimeEnvironment environment, String identifier) {
        if (manager != null) {
            throw new IllegalStateException("There is already one RuntimeManager active");
        }

        this.strategy = strategy;
        switch (strategy) {
        case SINGLETON:
            if (identifier == null) {
                manager = managerFactory.newSingletonRuntimeManager(environment);
            } else {
                manager = managerFactory.newSingletonRuntimeManager(environment, identifier);
            }
            break;
        case PERREQUEST:
            if (identifier == null) {
                manager = managerFactory.newPerRequestRuntimeManager(environment);
            } else {
                manager = managerFactory.newPerRequestRuntimeManager(environment, identifier);
            }
            break;
        case PERPROCESSINSTANCE:
            if (identifier == null) {
                manager = managerFactory.newPerProcessInstanceRuntimeManager(environment);
            } else {
                manager = managerFactory.newPerProcessInstanceRuntimeManager(environment, identifier);
            }
            break;
        default:
            if (identifier == null) {
                manager = managerFactory.newSingletonRuntimeManager(environment);
            } else {
                manager = managerFactory.newSingletonRuntimeManager(environment, identifier);
            }
            break;
        }

        return manager;
    }
    
    public RuntimeEngine getRuntimeEngine() {
        return getRuntimeEngine(null);
    }
    
    public RuntimeEngine getRuntimeEngine(Long pid) {
        if (strategy == Strategy.PERPROCESSINSTANCE) {
            if (pid == null) {
                return manager.getRuntimeEngine(ProcessInstanceIdContext.get());
            }
            return manager.getRuntimeEngine(ProcessInstanceIdContext.get(pid));
        }
        return manager.getRuntimeEngine(EmptyContext.get());
    }
    
    public EntityManagerFactory getEmf() {
        return emf;
    }

    public void disposeRuntimeManager() {
        if (manager != null) {
            manager.close();
            manager = null;
        }
    }
    
    protected static void cleanupSingletonSessionId() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        if (tempDir.exists()) {

            String[] jbpmSerFiles = tempDir.list(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {

                    return name.endsWith("-jbpmSessionId.ser");
                }
            });
            for (String file : jbpmSerFiles) {

                new File(tempDir, file).delete();
            }
        }
    }
    
}
