package org.kie.perf.remote;

import java.net.MalformedURLException;
import java.net.URL;

import org.kie.remote.client.api.RemoteRestRuntimeEngineBuilder;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;

public class RESTClient extends AbstractClient {

    public static final String RestClientId = "RESTClient";

    private final String deploymentId;
    private final URL url;
    private final int timeout;
    private Class<?>[] classes;

    private String username;
    private String password;
    private Long processInstanceId;

    public RESTClient(String deploymentId, Class<?>... classes) {
        this(deploymentId, config.getApplicationUrl(), config.getUsername(), config.getPassword(), classes);
    }

    public RESTClient(String deploymentId, String username, String password, Class<?>... classes) {
        this(deploymentId, config.getApplicationUrl(), username, password, classes);
    }

    public RESTClient(String deploymentId, String url, String username, String password, Class<?>... classes) {
        this(deploymentId, url, username, password, 10, null, classes);
    }

    public RESTClient(String deploymentId, String url, String username, String password, int timeout, Class<?>... classes) {
        this(deploymentId, url, username, password, timeout, null, classes);
    }

    public RESTClient(String deploymentId, String url, String username, String password, int timeout, Long processInstanceId, Class<?>... classes) {
        this.deploymentId = deploymentId;
        this.timeout = timeout;
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL.", e);
        }
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
        return new RESTClient(deploymentId, url.toExternalForm(), username, password, timeout, processInstanceId, classes);
    }

    private void buildRuntimeEngine() {
        RemoteRestRuntimeEngineBuilder builder = RemoteRuntimeEngineFactory.newRestBuilder().addDeploymentId(deploymentId).addUrl(url)
                .addUserName(username).addPassword(password).addTimeout(timeout).addExtraJaxbClasses(classes);
        if (processInstanceId != null) {
            builder = builder.addProcessInstanceId(processInstanceId);
        }
        runtimeEngine = builder.build();
        kieSession = runtimeEngine.getKieSession();
        taskService = runtimeEngine.getTaskService();
        auditService = runtimeEngine.getAuditService();
    }

    @Override
    public String toString() {
        return RestClientId;
    }
}
