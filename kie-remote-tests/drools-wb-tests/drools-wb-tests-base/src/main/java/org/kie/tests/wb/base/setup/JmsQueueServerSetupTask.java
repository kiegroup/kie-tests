package org.kie.tests.wb.base.setup;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hornetq.api.core.management.ObjectNameBuilder;
import org.hornetq.api.jms.management.JMSServerControl;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsQueueServerSetupTask extends AbstractServerSetupTask implements ServerSetupTask {

    private static Logger logger = LoggerFactory.getLogger(JmsQueueServerSetupTask.class);
    
    public static final String HORNETQ_JMS_XML = "/hornetq-jms.xml";

    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        addJmsQueuesViaJbossManagement();
    }

    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        InetAddress loopbackAddr = InetAddress.getByName("127.0.0.1");
        final ModelControllerClient client = ModelControllerClient.Factory.create(loopbackAddr, 9999);

        String[] queueNames = { "KIE.TASK", "KIE.SESSION", "KIE.RESPONSE" };
        for (String queue : queueNames) {
            removeJmsQueue(client, queue);
            removeJmsQueue(client, queue + ".#");
        }

        /**
        final ModelNode reloadOperation = new ModelNode();
        reloadOperation.get("operation").set("reload");
        logger.info("Reload config:" + client.execute(reloadOperation));
        **/
        
        Thread.sleep(3000); 

        client.close();
    }

    private void addJmsQueuesViaJbossManagement() throws Exception { 
        InetAddress loopbackAddr = InetAddress.getByName("127.0.0.1");
        final ModelControllerClient client = ModelControllerClient.Factory.create(loopbackAddr, 9999);

        String[] queueNames = { "KIE.TASK", "KIE.SESSION", "KIE.RESPONSE" };
        for (String queue : queueNames) {
            addNormalQueue(client, queue);
            addWildcardQueue(client, queue);
        }

        /**
        final ModelNode reloadOperation = new ModelNode();
        reloadOperation.get("operation").set("reload");
        logger.info("Reload config:" + client.execute(reloadOperation));
        **/
        
        Thread.sleep(3000); 

        client.close();
    }
    
    private void addNormalQueue(ModelControllerClient client, String queue) throws Exception { 
        ModelNode op = new ModelNode();
        op.get(ClientConstants.OP).set(ClientConstants.ADD);
        
        ModelNode address = op.get(ClientConstants.OP_ADDR);
        address.add("subsystem", "messaging");
        address.add("hornetq-server", "default");
        address.add("jms-queue", queue );
        
        ModelNode entries = op.get("entries");
        entries.add("java:jboss/exported/jms/queue/" + queue);
        entries.add("java:/queue/" + queue + ".ALL");
        
        ModelNode attributes = op.get("attributes");
        attributes.add("temporary", "true");
        
        logger.info("Added queue " + queue + ": " + client.execute(op));
    }
    
    private void addWildcardQueue(ModelControllerClient client, String queue) throws Exception { 
        ModelNode op = new ModelNode();
        op.get(ClientConstants.OP).set(ClientConstants.ADD);
        
        ModelNode address = op.get(ClientConstants.OP_ADDR);
        address.add("subsystem", "messaging");
        address.add("hornetq-server", "default");
        address.add("jms-queue", queue + ".#" );
        
        ModelNode entries = op.get("entries");
        entries.add("java:/queue/" + queue );
        
        ModelNode attributes = op.get("attributes");
        attributes.add("temporary", "true");
        
        logger.info("Added queue " + queue + ": " + client.execute(op));
    }
    
    private void removeJmsQueue(ModelControllerClient client, String queue) throws Exception { 
        ModelNode op = new ModelNode();
        op.get(ClientConstants.OP).set(ClientConstants.REMOVE_OPERATION);
        
        ModelNode address = op.get(ClientConstants.OP_ADDR);
        address.add("subsystem", "messaging");
        address.add("hornetq-server", "default");
        address.add("jms-queue", queue );
        
        logger.info("Removed queue " + queue + ": " + client.execute(op));
    }

    private void createTemporaryDynamicJMSQueue(String queueName) { 
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName on = ObjectNameBuilder.DEFAULT.getJMSServerObjectName();
            
            String jndiBindings = "queue/"+queueName+",java:jboss/exported/jms/queue/" + queueName;
            Object [] params = { queueName, jndiBindings, null, false };
            String [] signature = { 
                    String.class.getName(), // name
                    String.class.getName(), // jndiBindings
                    String.class.getName(), // selector
                    boolean.class.getName() // durable
            };
            mBeanServer.invoke(on, "createQueue", params, signature);
        } catch (Exception e) {
            logger.error("Unable to create dynamic JMS queue via " + JMSServerControl.class.getSimpleName(), e);
        }
    }
}
