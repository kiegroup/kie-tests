package org.kie.tests.wb.eap.security;


import java.security.Principal;
import java.security.Provider;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.jacc.PolicyContext;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class SecurityBean {

    @Resource
    private SessionContext ctx;

    @Inject
    public ServiceContainer serviceContainer;
    
    private Logger logger = LoggerFactory.getLogger(SecurityBean.class);

    public void explore() throws Exception {
        logger.info("Caller principal: " + ctx.getCallerPrincipal().getName());
        logger.info("Policy context id: " + PolicyContext.getContextID());
        Set handlerKeys = PolicyContext.getHandlerKeys();
        if (handlerKeys != null) {
            for (Object key : handlerKeys) {
                logger.info("> handler key: " + key.getClass().getName() + "| " + key.toString());
            }
        }

        lookAtJaccService();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Configuration getConfiguration() {
        Configuration config = (Configuration) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            public Object run() {
                return Configuration.getConfiguration();
            }
        });
        return config;
    }

    private Subject tryLogin() throws LoginException {
        String[] creds = { "mary", "mary123@" };
        CallbackHandler handler = new UserPassCallbackHandler(creds);
        LoginContext lc = new LoginContext("ApplicationRealm", handler);
        lc.login();
        return lc.getSubject();
    }

    private void exploreSubject(Subject subject) {
        if( subject == null ) { 
            throw new RuntimeException("Subject is null!");
        }

        logger.info("Read-only? : " + subject.isReadOnly());
        logger.info("Principals: ");
        for (Principal prin : subject.getPrincipals()) {
            logger.info("> " + prin.getName());
            if ("Roles".equals(prin.getName())) {
                getGroups(prin);
            }
        }
    }

    private void getGroups(Principal principal) {
        if (principal instanceof Group && "Roles".equalsIgnoreCase(principal.getName())) {
            Enumeration<? extends Principal> groups = ((Group) principal).members();

            while (groups.hasMoreElements()) {
                Principal groupPrincipal = (Principal) groups.nextElement();
                logger.info(">> " + groupPrincipal.getName());
            }
        }
    }

    private void exploreConfigAndProvider() {
        Configuration config = getConfiguration();
        Provider provider = config.getProvider();
        if (provider != null) {
            logger.info("Name   : " + provider.getName());
            logger.info("Version: " + provider.getVersion());
            logger.info("Info   : " + provider.getInfo());
            logger.info("Class  : " + provider.getClass().getName());
        } else {
            logger.warn("No provider could be found!");
        }
        logger.info("Config type: " + config.getType());
        if (config.getParameters() != null) {
            logger.info(config.getParameters().getClass().getName());
        }
    }

    private List<String> getGroupsForUser() {
        List<String> roles = new ArrayList<String>();
        try {
            Subject subject = (Subject) PolicyContext.getContext("javax.security.auth.Subject.container");

            if (subject != null) {
                Set<Principal> principals = subject.getPrincipals();

                if (principals != null) {
                    for (Principal principal : principals) {
                        if (principal instanceof Group && "Roles".equalsIgnoreCase(principal.getName())) {
                            Enumeration<? extends Principal> groups = ((Group) principal).members();

                            while (groups.hasMoreElements()) {
                                Principal groupPrincipal = (Principal) groups.nextElement();
                                roles.add(groupPrincipal.getName());

                            }
                            break;

                        }

                    }
                }
            } else {
                throw new RuntimeException("HEY! Where's my SUBJECT!");
            }
        } catch (Exception e) {
            logger.error("Error when getting user roles", e);
        }
        return roles;
    }

    private void lookAtJaccService() { 
        ServiceContainer serviceContainerFromCurrent = CurrentServiceContainer.getServiceContainer();
        ServiceController<?> jaccService = null;
        if( serviceContainerFromCurrent != null ) { 
            for( ServiceName serviceName : serviceContainerFromCurrent.getServiceNames() ) { 
                if( serviceName.getSimpleName().endsWith("jboss.security.jacc")) {
                    jaccService = serviceContainer.getService(serviceName);
                }
            }
            Object valueObj = jaccService.getValue();
            System.out.println( "value: " + (valueObj == null ? "null" : valueObj.getClass().getName()));
        } else { 
            System.out.println( "Could not get current service container!");
        }
        
        if( serviceContainer != null ) { 
            System.out.println( "But could get an injected service container!");
            for( ServiceName serviceName : serviceContainer.getServiceNames() ) { 
                if( serviceName.getSimpleName().endsWith("jboss.security.jacc")) {
                    jaccService = serviceContainerFromCurrent.getService(serviceName);
                }
            }
            if( jaccService != null ) { 
                Object valueObj = jaccService.getValue();
                System.out.println( "value: " + (valueObj == null ? "null" : valueObj.getClass().getName()));
            } else { 
                System.out.println( "No JaccService instance found!");
            }
        } else { 
            System.out.println( "..or just a normal service container");
            
        }
    }

}
