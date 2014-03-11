package org.kie.tests.wb.eap.security;

import java.security.Principal;
import java.security.Provider;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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

        getGroupsForUser();
    }

    private Subject tryLogin() throws LoginException {
        String[] creds = { "mary", "mary123@" };
        CallbackHandler handler = new UserPassCallbackHandler(creds);
        LoginContext lc = new LoginContext("ApplicationRealm", handler);
        lc.login();
        return lc.getSubject();
    }

    private void exploreSubject(Subject subject) {
        if (subject == null) {
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
                                logger.info("role: " + groupPrincipal.getName());
                            }
                            break;
                        }
                    }
                    subject.getPublicCredentials().add("test");
                }
            } else {
                throw new RuntimeException("HEY! Where's my SUBJECT!");
            }
        } catch (Exception e) {
            logger.error("Error when getting user roles", e);
        }
        return roles;
    }

    // ServiceContainer ----------------------------------------------------------------------------------------------------------
   
    @PostConstruct
    public void lookAtJaccService() {
        ServiceContainer serviceContainerFromCurrent = CurrentServiceContainer.getServiceContainer();
        ServiceController<?> jaccService = null;
        if (serviceContainerFromCurrent != null) {
            for (ServiceName serviceName : serviceContainerFromCurrent.getServiceNames()) {
                if (serviceName.getSimpleName().endsWith("jboss.security.jacc")) {
                    jaccService = serviceContainerFromCurrent.getService(serviceName);
                }
            }
            Object valueObj = jaccService.getValue();
            System.out.println("value: " + (valueObj == null ? "null" : valueObj.getClass().getName()));
        } else {
            System.out.println("Could not get current service container!");
        }

        ServiceContainer serviceContainer = lookupServiceContainer();
        if (serviceContainer != null) {
            System.out.println("But could get an injected service container!");
            for (ServiceName serviceName : serviceContainer.getServiceNames()) {
                if (serviceName.getSimpleName().endsWith("jboss.security.jacc")) {
                    jaccService = serviceContainerFromCurrent.getService(serviceName);
                }
            }
            if (jaccService != null) {
                Object valueObj = jaccService.getValue();
                logger.info("value: " + (valueObj == null ? "null" : valueObj.getClass().getName()));
            } else {
                logger.warn("No JaccService instance found!");
            }
        } else {
            logger.info("..or just a normal service container");

        }
    }

    private ServiceContainer lookupServiceContainer() {
        BeanManager bm = getBeanManager();

        Set<Bean<?>> beanSet = (Set<Bean<?>>) bm.getBeans(ServiceContainer.class);
        if (beanSet != null && beanSet.size() > 0) {
            Bean<ServiceContainer> bean = (Bean<ServiceContainer>) beanSet.iterator().next();
            CreationalContext<ServiceContainer> ctx = bm.createCreationalContext(bean);
            return (ServiceContainer) bm.getReference(bean, ServiceContainer.class, ctx); // this could be inlined, but
                                                                                          // intentionally left this way
        } else {
            return null;
        }

    }

    private BeanManager getBeanManager() {
        try {
            InitialContext initialContext = new InitialContext();
            return (BeanManager) initialContext.lookup("java:comp/BeanManager");
        } catch (NamingException ne) {
            logger.error("Couldn't get BeanManager through JNDI", ne);
            return null;
        }
    }
}
