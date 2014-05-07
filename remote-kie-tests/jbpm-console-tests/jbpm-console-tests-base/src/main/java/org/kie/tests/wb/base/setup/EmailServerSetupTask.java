package org.kie.tests.wb.base.setup;

import java.net.InetAddress;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.kie.tests.wb.base.methods.RestIntegrationTestMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailServerSetupTask implements ServerSetupTask {

    private static Logger logger = LoggerFactory.getLogger(RestIntegrationTestMethods.class);
    
    /**
     * Name in JNDI to which the SMTP {@link javax.mail.Session} will be bound
     */
    String JNDI_BIND_NAME_MAIL_SESSION = "java:/mail/jbpmMailSession";
    
    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        /*
         * First configure a JavaMail Session for the Server to bind into JNDI; this
         * will be used by our MailService EJB.  In a production environment, we'll likely have configured
         * the server before it was started to point to a real SMTP server
         */

        final ModelControllerClient client = ModelControllerClient.Factory.create("localhost", 9999);

        /**
         * <outbound-socket-binding name="mail-smtp-25000">
         *   <remote-destination host="localhost" port="25000"/>
         * </outbound-socket-binding>
         */
        final ModelNode createSocketBindingOperation = new ModelNode();
        createSocketBindingOperation.get("operation").set("add");
        createSocketBindingOperation.get("host").set("localhost");
        createSocketBindingOperation.get("port").set(25000);
        final ModelNode socketBindingAddress = createSocketBindingOperation.get("address");
        socketBindingAddress.add("socket-binding-group", "standard-sockets");
        socketBindingAddress.add("remote-destination-outbound-socket-binding", "mail-smtp-25000");
        logger.info("Add remote outbound socket binding: " + client.execute(createSocketBindingOperation));

        /**
         * <mail-session jndi-name="java:/mail/jbpmMailSession" debug="true">
         * </mail-session>
         */
        final ModelNode createMailServiceOperation = new ModelNode();
        createMailServiceOperation.get("operation").set("add");
        createMailServiceOperation.get("jndi-name").set(JNDI_BIND_NAME_MAIL_SESSION);
        createMailServiceOperation.get("debug").set("true");
        final ModelNode smtpAddress = createMailServiceOperation.get("address");
        smtpAddress.add("subsystem", "mail");
        smtpAddress.add("mail-session", JNDI_BIND_NAME_MAIL_SESSION);
        logger.info("Add mail service:" + client.execute(createMailServiceOperation));

        /**
         * <mail-session jndi-name="java:/mail/jbpmMailSession" debug="true">
         * </mail-session>
         */
        final ModelNode createSocketBindingRefOp = new ModelNode();
        createSocketBindingRefOp.get("operation").set("add");
        createSocketBindingRefOp.get("outbound-socket-binding-ref").set("mail-smtp-25000");
        final ModelNode socketBindingRefAddress = createSocketBindingRefOp.get("address");
        socketBindingRefAddress.add("subsystem", "mail");
        socketBindingRefAddress.add("mail-session", JNDI_BIND_NAME_MAIL_SESSION);
        socketBindingRefAddress.add("server", "smtp");
        logger.info("Configure mail service w/ socket binding:" + client.execute(createSocketBindingRefOp));

        final ModelNode reloadOperation = new ModelNode();
        reloadOperation.get("operation").set("reload");
        logger.info("Reload config:" + client.execute(reloadOperation));

        Thread.sleep(3000); // Because the operation returns but then server reload continues in the BG
        // Find from the WildFly team a better notification mechanism upon which to wait
        // https://github.com/arquillian/continuous-enterprise-development/issues/66
        client.close();
        
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getLocalHost(), 9999);

        final ModelNode removeSocketBindingOperation = new ModelNode();
        removeSocketBindingOperation.get("operation").set("remove");
        final ModelNode socketBindingAddress = removeSocketBindingOperation.get("address");
        socketBindingAddress.add("socket-binding-group", "standard-sockets");
        socketBindingAddress.add("remote-destination-outbound-socket-binding", "mail-smtp-25000");
        logger.info("REMOVE SOCKETS" + client.execute(removeSocketBindingOperation));

        final ModelNode removeMailServiceOperation = new ModelNode();
        removeMailServiceOperation.get("operation").set("remove");
        final ModelNode smtpAddress = removeMailServiceOperation.get("address");
        smtpAddress.add("subsystem", "mail");
        smtpAddress.add("mail-session", JNDI_BIND_NAME_MAIL_SESSION);
        logger.info("REMOVE MAIL" + client.execute(removeMailServiceOperation));

        final ModelNode reloadOperation = new ModelNode();
        reloadOperation.get("operation").set("reload");
        logger.info("Reload config:" + client.execute(reloadOperation));
        Thread.sleep(3000); // Because the operation returns but then server reload continues in the BG
                    // Find from the WildFly team a better notification mechanism upon which to wait
                    // https://github.com/arquillian/continuous-enterprise-development/issues/66

        client.close();
        
    } 
    

}
