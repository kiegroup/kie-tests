package org.kie.tests.wb.base.services.mail;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

/**
 * Test fixture; installs an embedded SMTP Server on startup, shuts it down on undeployment.
 * Allows for pluggable handling of incoming messages for use in testing.
 *
 * (Grabbed from https://github.com/arquillian/continuous-enterprise-development/ 
 *  and subsequently modified)
 * 
 * @author ALR
 */
@Singleton
@Startup
@LocalBean
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class TestSMTPServerService {

    private static final Logger log = Logger.getLogger(TestSMTPServerService.class.getName());
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final int BIND_PORT = 25000;

    private SMTPServer server;
    private final PluggableReceiveHandlerMessageListener listener = new PluggableReceiveHandlerMessageListener();
    
    private final List<JaxbMailMessage> messages = new ArrayList<JaxbMailMessage>();

    /**
     * Start the SMTP Embedded Server; called by the container during deployment
     *
     * @throws Exception
     */
    @PostConstruct
    public void startup() throws Exception {
        server = new SMTPServer(new SimpleMessageListenerAdapter(listener));
        server.setBindAddress(InetAddress.getLocalHost());
        server.setPort(BIND_PORT);
        server.start();
    }

    /**
     * Stop the SMTP Server; called by the container on undeployment
     *
     * @throws Exception
     */
    @PreDestroy
    public void shutdown() throws Exception {
        server.stop();
    }

    /**
     * {@link SimpleMessageListener} implementation allowing extensible handling of
     * incoming SMTP events
     */
    private class PluggableReceiveHandlerMessageListener implements SimpleMessageListener {

        @Override
        public boolean accept(String from, String recipient) {
            return true;
        }

        @Override
        public void deliver(final String from, final String to, final InputStream data)
            throws IOException {

            // Get contents as String
            byte[] buffer = new byte[4096];
            int read;
            final StringBuilder s = new StringBuilder();
            while ((read = data.read(buffer)) != -1) {
                s.append(new String(buffer, 0, read, CHARSET));
            }
            final String contents = s.toString();
            if (log.isLoggable(Level.INFO)) {
                log.info("Received SMTP event: " + contents);
            }

            // Pluggable handling
            synchronized( messages ) { 
                messages.add(new JaxbMailMessage(from, to, contents));
            } 
        }
    }

    public List<JaxbMailMessage> getMessages() {
        List<JaxbMailMessage> messagesCopy = null;
        synchronized( messages ) { 
            messagesCopy = new ArrayList<JaxbMailMessage>(messages);
        }
        return messagesCopy;
    }
}
