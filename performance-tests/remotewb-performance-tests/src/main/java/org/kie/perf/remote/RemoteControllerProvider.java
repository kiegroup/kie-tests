package org.kie.perf.remote;

public class RemoteControllerProvider {

    public static RemoteController getRemoteController(String deploymentId, Class<?>... classes) {
        String remoteAPI = KieWBTestConfig.getInstance().getRemoteAPI();
        RemoteController rc = null;
        if (remoteAPI.equals("REST")) {
            rc = getRestClient(deploymentId, classes);
        } else if (remoteAPI.equals("JMS")) {
            rc = getJMSClient(deploymentId, classes);
        }
        return rc;
    }

    public static RESTClient getRestClient(String deploymentId, Class<?>... classes) {
        RESTClient client = new RESTClient(deploymentId, classes);
        return client;
    }

    public static JMSClient getJMSClient(String deploymentId, Class<?>... classes) {
        JMSClient client = new JMSClient(deploymentId, classes);
        return client;
    }

}
