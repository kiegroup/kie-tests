package org.kie.tests.wb.live;

import static org.kie.services.client.serialization.SerializationConstants.DEPLOYMENT_ID_PROPERTY_NAME;
import static org.kie.services.client.serialization.SerializationConstants.SERIALIZATION_TYPE_PROPERTY_NAME;
import static org.kie.services.shared.ServicesVersion.VERSION;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Ignore;
import org.kie.api.command.Command;
import org.kie.api.task.model.Comment;
import org.kie.api.task.model.I18NText;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.PeopleAssignments;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskData;
import org.kie.remote.client.api.exception.MissingRequiredInfoException;
import org.kie.remote.client.api.exception.RemoteApiException;
import org.kie.remote.client.api.exception.RemoteCommunicationException;
import org.kie.remote.client.jaxb.ClientJaxbSerializationProvider;
import org.kie.remote.client.jaxb.ConversionUtil;
import org.kie.remote.client.jaxb.JaxbCommandsRequest;
import org.kie.remote.client.jaxb.JaxbCommandsResponse;
import org.kie.remote.jaxb.gen.AddTaskCommand;
import org.kie.remote.jaxb.gen.AuditCommand;
import org.kie.remote.jaxb.gen.GetTaskCommand;
import org.kie.remote.jaxb.gen.TaskCommand;
import org.kie.remote.jaxb.gen.Type;
import org.kie.services.client.serialization.JaxbSerializationProvider;
import org.kie.services.client.serialization.SerializationConstants;
import org.kie.services.client.serialization.SerializationException;
import org.kie.services.client.serialization.jaxb.impl.JaxbCommandResponse;
import org.kie.services.client.serialization.jaxb.rest.JaxbExceptionResponse;
import org.kie.tests.wb.base.util.TestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore // add Junit "Ping Succeed or Ignore" rule
public class AddTaskLiveTest {

    protected static final Logger logger = LoggerFactory.getLogger(AddTaskLiveTest.class);

    private final String USER = TestConstants.MARY_USER;
    private final String PASSWORD = TestConstants.MARY_PASSWORD;
    private final String DEPLOYMENT_ID = "org.test:kjar:1.0";

    private InitialContext context;
    private ConnectionFactory connectionFactory;
    private Queue sendQueue, responseQueue;

    private final String url = "http://localhost:8080/kie-wb/";

    public AddTaskLiveTest() {
        setup();
    }

    private void setup() {
        URL serverUrl = null;
        try {
            serverUrl = new URL(url);
        } catch( MalformedURLException murle ) {
            logger.error("Malformed URL for the server instance!", murle);
        }

        // Get JNDI context from server
        context = getRemoteJbossInitialContext(serverUrl, USER, PASSWORD);

        // Create JMS connection
        try {
            connectionFactory = (ConnectionFactory) context.lookup("jms/RemoteConnectionFactory");
            // connectionFactory = (ConnectionFactory) context.lookup("java:/ConnectionFactory");
        } catch( NamingException ne ) {
            throw new RuntimeException("Unable to lookup JMS connection factory.", ne);
        }

        // Setup queues
        try {
            sendQueue = (Queue) context.lookup("jms/queue/KIE.SESSION");
            responseQueue = (Queue) context.lookup("jms/queue/KIE.RESPONSE");
        } catch( NamingException ne ) {
            throw new RuntimeException("Unable to lookup send or response queue", ne);
        }
    }

    /**
     * Method to communicate with the backend via JMS.
     * 
     * @param command The {@link Command} object to be executed.
     * @return The result of the {@link Command} object execution.
     */
    private Object executeCommandViaJms( Command command, String user, String password, String deploymentId, Long processInstanceId, 
            ConnectionFactory factory, Queue sendQueue, boolean useSsl, Queue responseQueue, List<Class<?>> extraJaxbClasses,
            int timeoutInSecs) {
        JaxbCommandsRequest req = prepareCommandRequest(command, deploymentId, processInstanceId, user);

        boolean isTaskCommand = command instanceof TaskCommand;
        
        Connection connection = null;
        Session session = null;
        JaxbCommandsResponse cmdResponse = null;
        String corrId = UUID.randomUUID().toString();
        String selector = "JMSCorrelationID = '" + corrId + "'";
        try {

            // setup
            MessageProducer producer;
            MessageConsumer consumer;
            try {
                connection = factory.createConnection(user, password );
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                producer = session.createProducer(sendQueue);
                consumer = session.createConsumer(responseQueue, selector);

                connection.start();
            } catch( JMSException jmse ) {
                throw new RemoteCommunicationException("Unable to setup a JMS connection.", jmse);
            }

            // Create msg
            TextMessage textMsg;
            JaxbSerializationProvider serializationProvider;
            try {

                // serialize request
                boolean extraClasses = ( extraJaxbClasses != null && extraJaxbClasses.size() > 0 );
                if( extraClasses )  { 
                    serializationProvider = ClientJaxbSerializationProvider.newInstance(extraJaxbClasses);
                } else { 
                    serializationProvider = ClientJaxbSerializationProvider.newInstance();
                }
                            
                String xmlStr = serializationProvider.serialize(req);
                textMsg = session.createTextMessage(xmlStr);
                
                // set properties
                // 1. corr id
                textMsg.setJMSCorrelationID(corrId);
                // 2. serialization info
                textMsg.setIntProperty(SERIALIZATION_TYPE_PROPERTY_NAME, JaxbSerializationProvider.JMS_SERIALIZATION_TYPE);
                if( extraClasses ) { 
                    if( deploymentId == null ) {
                        throw new MissingRequiredInfoException(
                                "Deserialization of parameter classes requires a deployment id, which has not been configured.");
                    }
                    textMsg.setStringProperty(DEPLOYMENT_ID_PROPERTY_NAME, deploymentId);
                }
                // 3. user/pass for task operations
                String userName = user;
                if( isTaskCommand ) {
                    if( userName == null ) {
                        throw new RemoteCommunicationException(
                                "A user name is required when sending task operation requests via JMS");
                    }
                    if( password == null ) {
                        throw new RemoteCommunicationException(
                                "A password is required when sending task operation requests via JMS");
                    }
                    textMsg.setStringProperty("username", userName);
                    textMsg.setStringProperty("password", password);
                }
                // 4. process instance id
            } catch( JMSException jmse ) {
                throw new RemoteCommunicationException("Unable to create and fill a JMS message.", jmse);
            } catch( SerializationException se ) {
                throw new RemoteCommunicationException("Unable to deserialze JMS message.", se.getCause());
            }

            // send
            try {
                producer.send(textMsg);
            } catch( JMSException jmse ) {
                throw new RemoteCommunicationException("Unable to send a JMS message.", jmse);
            }

            // receive
            Message response;
            try {
                response = consumer.receive(timeoutInSecs * 1000);
            } catch( JMSException jmse ) {
                throw new RemoteCommunicationException("Unable to receive or retrieve the JMS response.", jmse);
            }

            if( response == null ) {
                logger.warn("Response is empty");
                return null;
            }
            // extract response
            assert response != null: "Response is empty.";
            try {
                String xmlStr = ((TextMessage) response).getText();
                cmdResponse = (JaxbCommandsResponse) serializationProvider.deserialize(xmlStr);
            } catch( JMSException jmse ) {
                throw new RemoteCommunicationException("Unable to extract " + JaxbCommandsResponse.class.getSimpleName()
                        + " instance from JMS response.", jmse);
            } catch( SerializationException se ) {
                throw new RemoteCommunicationException("Unable to extract " + JaxbCommandsResponse.class.getSimpleName()
                        + " instance from JMS response.", se.getCause());
            }
            assert cmdResponse != null: "Jaxb Cmd Response was null!";
        } finally {
            if( connection != null ) {
                try {
                    connection.close();
                    if( session != null ) {
                        session.close();
                    }
                } catch( JMSException jmse ) {
                    logger.warn("Unable to close connection or session!", jmse);
                }
            }
        }
        String version = cmdResponse.getVersion();
        if( version == null ) {
            version = "pre-6.0.3";
        }
        if( !version.equals(VERSION) ) {
            logger.info("Response received from server version [{}] while client is version [{}]! This may cause problems.",
                    version, VERSION);
        }
        List<JaxbCommandResponse<?>> responses = cmdResponse.getResponses();
        if( responses.size() > 0 ) {
            JaxbCommandResponse<?> response = responses.get(0);
            if( response instanceof JaxbExceptionResponse ) {
                JaxbExceptionResponse exceptionResponse = (JaxbExceptionResponse) response;
                throw new RemoteApiException(exceptionResponse.getMessage());
            } else {
                return response.getResult();
            }
        } else {
            assert responses.size() == 0: "There should only be 1 response, " + "not " + responses.size()
                    + ", returned by a command!";
            return null;
        }
    }

    private static JaxbCommandsRequest prepareCommandRequest( Command command, String deploymentId, Long processInstanceId, String username ) {
        if( deploymentId == null && !(command instanceof TaskCommand || command instanceof AuditCommand) ) {
            throw new MissingRequiredInfoException("A deployment id is required when sending commands involving the KieSession.");
        }
        JaxbCommandsRequest req;
        if( command instanceof AuditCommand ) {
            req = new JaxbCommandsRequest(command);
        } else {
            req = new JaxbCommandsRequest(deploymentId, command);
        }

        req.setProcessInstanceId(processInstanceId);
        req.setUser(username);
        req.setVersion(VERSION);

        return req;
    }
    
    private InitialContext getRemoteJbossInitialContext( URL url, String user, String password ) {
        Properties initialProps = new Properties();
        initialProps.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        String jbossServerHostName = url.getHost();
        initialProps.setProperty(InitialContext.PROVIDER_URL, "remote://" + jbossServerHostName + ":4447");
        initialProps.setProperty(InitialContext.SECURITY_PRINCIPAL, user);
        initialProps.setProperty(InitialContext.SECURITY_CREDENTIALS, password);
        // initialProps.setProperty(InitialContext.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");

        for( Object keyObj : initialProps.keySet() ) {
            String key = (String) keyObj;
            System.setProperty(key, (String) initialProps.get(key));
        }

        try {
            return new InitialContext(initialProps);
        } catch( NamingException e ) {
            throw new RemoteCommunicationException("Unable to create " + InitialContext.class.getSimpleName(), e);
        }
    }

    public Long addTask( org.kie.remote.jaxb.gen.Task task ) throws Exception {
        Long taskId = null;
        AddTaskCommand cmd = new AddTaskCommand();
        cmd.setTaskId(task.getId());
        cmd.setJaxbTask(task);

        // @formatter:off
        Object cmdRespObj = executeCommandViaJms(cmd, // cmd
                USER, PASSWORD, DEPLOYMENT_ID, null,  // user, password, dep id, proc inst id (for pre-process)
                connectionFactory, sendQueue, false, responseQueue,  // JMS stuff
                Collections.EMPTY_LIST, 5); // extra jaxb classes, timeout
        // @formatter:on
        taskId = (Long) cmdRespObj;
        return taskId;
    }

    public org.kie.remote.jaxb.gen.Task createNewTask( String taskName, String taskComment, List<String> actorId, String creatorId, String Version,
            int priority, Date expirationTime ) throws Exception {
        org.kie.remote.jaxb.gen.Task task = new org.kie.remote.jaxb.gen.Task();
        
        if( taskName != null ) {
            // Set task Name
            org.kie.remote.jaxb.gen.I18NText text = new org.kie.remote.jaxb.gen.I18NText();
            text.setText(taskName);
            text.setLanguage("en-UK");
            task.getNames().add(text);
            task.getSubjects().add(text);
        }
        if( Version != null ) {
            // Setting Descriptions
            org.kie.remote.jaxb.gen.I18NText text = new org.kie.remote.jaxb.gen.I18NText();
            text.setText(Version);
            text.setLanguage("en-UK");
            task.getDescriptions().add(text);
        }

        task.setPriority(priority);
        org.kie.remote.jaxb.gen.TaskData taskData = new org.kie.remote.jaxb.gen.TaskData();
        task.setTaskData(taskData);
        taskData.setSkipable(false); // REQUIRED
        taskData.setWorkItemId(1l);
        taskData.setCreatedOn(ConversionUtil.convertDateToXmlGregorianCalendar(new Date()));
        taskData.setExpirationTime(ConversionUtil.convertDateToXmlGregorianCalendar(new Date()));

        // Set task comments
        if( taskComment != null ) {
            org.kie.remote.jaxb.gen.Comment comment = new org.kie.remote.jaxb.gen.Comment();
            comment.setAddedAt(ConversionUtil.convertDateToXmlGregorianCalendar(new Date()));
            comment.setAddedBy(creatorId);
            comment.setText(taskComment);
            taskData.getComments().add(comment);
        }

        org.kie.remote.jaxb.gen.PeopleAssignments assignments = new org.kie.remote.jaxb.gen.PeopleAssignments();
        List<org.kie.remote.jaxb.gen.OrganizationalEntity> potentialOwners = new ArrayList<org.kie.remote.jaxb.gen.OrganizationalEntity>();

        if( actorId != null ) {
            // String[] actorIds = actorId.split(",");
            String[] actorIds = actorId.toArray(new String[actorId.size()]);
            for( String id : actorIds ) {
                org.kie.remote.jaxb.gen.OrganizationalEntity orgEnt = new org.kie.remote.jaxb.gen.OrganizationalEntity();
                orgEnt.setId(id);
                orgEnt.setType(Type.GROUP);
                potentialOwners.add(orgEnt);
            }
        }
        assignments.getPotentialOwners().addAll(potentialOwners);

        org.kie.remote.jaxb.gen.OrganizationalEntity orgEnt = new org.kie.remote.jaxb.gen.OrganizationalEntity();
        orgEnt.setId("bpmsAdmin");
        orgEnt.setType(Type.USER);
        assignments.getBusinessAdministrators().add(orgEnt);

        task.setPeopleAssignments(assignments);

        task.setTaskData(taskData);

        return task;
    }


    public Task getTaskData( long taskId ) {
        GetTaskCommand cmd = new GetTaskCommand();
        cmd.setTaskId(taskId);
        
        // @formatter:off
        Object cmdRespObj = executeCommandViaJms(cmd,  // cmd
                USER, PASSWORD, DEPLOYMENT_ID, null,  // user, password, dep id, proc inst id (for pre-process)
                connectionFactory, sendQueue, false, responseQueue,  // JMS stuff
                Collections.EMPTY_LIST, 5); // extra jaxb classes, timeout
        // @formatter:on
        Task task = (Task) cmdRespObj;
        return task;
    }

    public static void main( String[] args ) throws Exception {
        AddTaskLiveTest test = new AddTaskLiveTest();
        String taskName = "New Task";
        String taskComment = "This is a new comment";

        List<String> actorId = new ArrayList<String>();
        actorId.add("test");

        String creatorId = "test";
        String version = "version";
        int priority = 1;
        Date expirationTime = new Date();
        org.kie.remote.jaxb.gen.Task task = test.createNewTask(taskName, taskComment, actorId, creatorId, version, priority, expirationTime);
        Long createdTaskId = test.addTask(task);

        Task response = test.getTaskData(createdTaskId);
        TaskData taskData = response.getTaskData();

        // task name
        String name = task.getNames().get(0).getText();
        System.out.println("This is the task name: " + name);

        // task comment
        List<Comment> comments = taskData.getComments();
        if( comments.size() > 0 ) {
            System.out.println("This is the task comment: " + comments.get(0).getText());
        } else {
            System.out.println("There are no comments");
        }

        // actor id
        org.kie.remote.jaxb.gen.PeopleAssignments peopleAssignments = task.getPeopleAssignments();
        List<org.kie.remote.jaxb.gen.OrganizationalEntity> organizationalEntities = peopleAssignments.getPotentialOwners();
        System.out.println("This is the actorid: " + organizationalEntities.get(0).toString());

        // priority
        System.out.println("This the priority: " + task.getPriority());
        // expiration
        System.out.println("This is the expiration time: " + taskData.getExpirationTime());
        // created date
        System.out.println("This is the created date: " + taskData.getCreatedOn());
    }
}