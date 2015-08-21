package org.kie.perf.remote;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.remote.client.api.RemoteJmsRuntimeEngineBuilder;
import org.kie.remote.client.api.RemoteRuntimeEngineFactory;

public class JMSClient extends AbstractClient {

    public static final String JmsClientId = "JMSClient";

    private String deploymentId;
    private String host;
    private int remotingPort;
    private Class<?>[] classes;

    private String username;
    private String password;
    private Long processInstanceId;

    private String sessionQueueName;
    private String taskQueueName;
    private String responseQueueName;

    public JMSClient(String deploymentId, Class<?>... classes) {
        this(deploymentId, config.getUsername(), config.getPassword(), classes);
    }

    public JMSClient(String deploymentId, String username, String password, Class<?>... classes) {
        this(deploymentId, config.getHost(), username, password, classes);
    }

    public JMSClient(String deploymentId, String host, String username, String password, Class<?>... classes) {
        this(deploymentId, host, config.getRemotingPort(), username, password, classes);
    }

    public JMSClient(String deploymentId, String host, int remotingPort, String username, String password, Class<?>... classes) {
        this(deploymentId, host, remotingPort, username, password, config.getKieSessionQueue(), config.getKieTaskQueue(), config
                .getKieResponseQueue(), classes);
    }

    public JMSClient(String deploymentId, String host, int remotingPort, String username, String password, String sessionQueueName,
            String taskQueueName, String responseQueueName, Class<?>... classes) {
        this(deploymentId, host, remotingPort, username, password, sessionQueueName, taskQueueName, responseQueueName, null, classes);
    }

    public JMSClient(String deploymentId, String host, int remotingPort, String username, String password, String sessionQueueName,
            String taskQueueName, String responseQueueName, Long processInstanceId, Class<?>... classes) {

        this.deploymentId = deploymentId;
        this.host = host;
        this.remotingPort = remotingPort;
        this.sessionQueueName = sessionQueueName;
        this.taskQueueName = taskQueueName;
        this.responseQueueName = responseQueueName;
        this.processInstanceId = processInstanceId;
        this.classes = classes;

        setCredentials(username, password);
    }

    @Override
    public void setCredentials(String userId, String password) {
        this.username = userId;
        this.password = password;
        buildRuntimeEngine();
    }

    @Override
    public RemoteController newPerProcessInstanceController(Long processInstanceId) {
        return new JMSClient(deploymentId, host, remotingPort, username, password, sessionQueueName, taskQueueName, responseQueueName,
                processInstanceId, classes);
    }

    private void buildRuntimeEngine() {
        try {
            Map<String, String> props = new HashMap<String, String>();
            if (config.isSslEnabled()) {
                props.put("jboss.naming.client.remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "true");
                props.put("jboss.naming.client.connect.options.org.xnio.Options.SSL_STARTTLS", "true");
            }
            InitialContext context = getInitialContext(host, remotingPort, username, password, props);
            ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(config.getConnectionFactory());

            RemoteJmsRuntimeEngineBuilder builder = RemoteRuntimeEngineFactory.newJmsBuilder().addDeploymentId(deploymentId)
                    .addConnectionFactory(connectionFactory).addUserName(username).addPassword(password).addTimeout(20).addExtraJaxbClasses(classes);

            Queue sessionQueue = (Queue) context.lookup(sessionQueueName);
            Queue taskQueue = (Queue) context.lookup(taskQueueName);
            Queue responseQueue = (Queue) context.lookup(responseQueueName);

            builder = builder.addKieSessionQueue(sessionQueue).addTaskServiceQueue(taskQueue).addResponseQueue(responseQueue);

            if (processInstanceId != null) {
                builder = builder.addProcessInstanceId(processInstanceId);
            }

            // SSL enabled
            if (config.isSslEnabled()) {
                builder = builder.addKeystoreLocation(config.getKeystoreLocation()).addKeystorePassword(config.getKeystorePassword())
                        .addTruststorePassword(config.getKeystorePassword()).useKeystoreAsTruststore().useSsl(true).addHostName(host)
                        .addJmsConnectorPort(config.getMessagingPort());
            } else {
                builder = builder.disableTaskSecurity();
            }

            runtimeEngine = builder.build();
            kieSession = runtimeEngine.getKieSession();
            taskService = runtimeEngine.getTaskService();
            auditService = runtimeEngine.getAuditService();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create client.", e);
        }
    }

    @Override
    public ProcessInstance startProcess(String processId) {
        return super.startProcess(processId);
    }

    @Override
    public ProcessInstance startProcess(String processId, Map<String, Object> parameters) {
        return super.startProcess(processId, parameters);
    }

    @Override
    public KieSession getKieSession() {
        return super.getKieSession();
    }

    @Override
    public String toString() {
        return JmsClientId;
    }

    public static InitialContext getInitialContext(String host, int port, String username, String password,
            Map<? extends Object, ? extends Object> additionalEnvironmentProps) {
        Properties initialProps = new Properties();
        initialProps.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        initialProps.setProperty(Context.PROVIDER_URL, String.format("%s://%s:%d", "remote", host, port));
        initialProps.setProperty(Context.SECURITY_PRINCIPAL, username);
        initialProps.setProperty(Context.SECURITY_CREDENTIALS, password);
        if (additionalEnvironmentProps != null) {
            initialProps.putAll(additionalEnvironmentProps);
        }

        try {
            return new InitialContext(initialProps);
        } catch (NamingException e) {
            throw new RuntimeException("Unable to create " + InitialContext.class.getName(), e);
        }
    }
}
