package org.kie.tests.wb.eap;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedAction;

import javax.net.ssl.SSLContext;

import org.hornetq.core.client.HornetQClientLogger;
import org.hornetq.core.remoting.impl.ssl.SSLSupport;
import org.hornetq.utils.ClassloadingUtil;
import org.jboss.logging.Logger;
import org.junit.Test;

public class SslTest {

    @Test
    public void logging() { 
        System.out.println( 
                System.getProperty("java.util.logging.config.file") + " >" );
        HornetQClientLogger.LOGGER.warn("TEST!");
        System.out.println( " <" );
    }
    
    @Test
    public void keyStoreLocation() throws Exception { 
        String keystorePath = this.getClass().getResource("/ssl/client_keystore.jks").getPath();
        String keystorePassword = "CLIENT_KEYSTORE_PASSWORD";
        String type = "JKS";
       
        KeyStore ks = KeyStore.getInstance(type);
        InputStream in = new FileInputStream(keystorePath);
        assertNotNull(in);
        ks.load(in, keystorePassword.toCharArray());
        
        SSLContext context = SSLSupport.createContext("ssl/client_keystore.jks", keystorePassword, null, null);
    }
    
}
