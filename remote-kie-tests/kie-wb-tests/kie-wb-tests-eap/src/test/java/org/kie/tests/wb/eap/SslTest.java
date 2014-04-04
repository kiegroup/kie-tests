package org.kie.tests.wb.eap;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedAction;

import javax.net.ssl.SSLContext;

import org.hornetq.core.remoting.impl.ssl.SSLSupport;
import org.hornetq.utils.ClassloadingUtil;
import org.junit.Test;

public class SslTest {

    @Test
    public void keyStoreLocation() throws Exception { 
        String keystorePath = this.getClass().getResource("/ssl/client_keystore.jks").getPath();
        String keystorePassword = "CLIENT_KEYSTORE_PASSWORD";
        String type = "JKS";
       
        System.out.println( type );
        System.out.println( keystorePath );

        KeyStore ks = KeyStore.getInstance(type);
        InputStream in = new FileInputStream(keystorePath);
        assertNotNull(in);
        ks.load(in, keystorePassword.toCharArray());
        
        SSLContext context = SSLSupport.createContext("ssl/client_keystore.jks", keystorePassword, null, null);
    }
    
    private URL validateUrl(String storePath) throws Exception { 
        // First see if this is a URL
        try
        {
           return new URL(storePath);
        }
        catch (MalformedURLException e)
        {
           File file = new File(storePath);
           if (file.exists() == true && file.isFile())
           {
              return file.toURI().toURL();
           }
           else
           {
              URL url = findResource(storePath);
              if (url != null)
              {
                 return url;
              }
           }
        }

        throw new Exception("Failed to find a store at " + storePath);
    }
    
    private static URL findResource(final String resourceName)
    {
       return AccessController.doPrivileged(new PrivilegedAction<URL>()
       {
          public URL run()
          {
             return ClassloadingUtil.findResource(resourceName);
          }
       });
    }
}
