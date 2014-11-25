package org.kie.tests.wb.base.methods;

import static org.junit.Assert.fail;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.map.ObjectMapper;
import org.guvnor.rest.client.CreateOrCloneRepositoryRequest;
import org.guvnor.rest.client.CreateOrganizationalUnitRequest;
import org.guvnor.rest.client.InstallProjectRequest;
import org.guvnor.rest.client.JobRequest;
import org.guvnor.rest.client.JobResult;
import org.guvnor.rest.client.JobStatus;
import org.guvnor.rest.client.OrganizationalUnit;
import org.guvnor.rest.client.RemoveRepositoryRequest;
import org.guvnor.rest.client.RepositoryRequest;
import org.jboss.resteasy.client.ClientRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.remote.common.rest.KieRemoteHttpRequest;
import org.kie.remote.common.rest.KieRemoteHttpResponse;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentJobResult;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentUnit;
import org.kie.services.client.serialization.jaxb.impl.deploy.JaxbDeploymentUnit.JaxbDeploymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * With thanks to Ivo Bek, Radovan Synek, Marek Baluch, Jiri Locker, Luask Petrovicky.
 * </p> 
 * Copied from the RestWorkbenchClient and BusinessCentral classes and then modified. 
 */
public class RepositoryDeploymentUtil {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryDeploymentUtil.class);

   
    public static final RuntimeStrategy strategy = RuntimeStrategy.SINGLETON;
    
    private RequestCreator requestCreator;
    
    public RepositoryDeploymentUtil(URL deploymentUrl, String user, String password) { 
        requestCreator = new RequestCreator(deploymentUrl, user, password, MediaType.APPLICATION_JSON_TYPE);
    }
    
    public void createAndDeployRepository(String repoUrl, String repositoryName, String project, String deploymentId, String orgUnit, String user, int sleepSecs) { 
        try {
            deleteRepository(repositoryName);
        } catch (Exception ex) {
            // just ignore, we only need to have working
            // environment created by the steps below
        }

        JobRequest createRepoJob = createRepository(repositoryName, repoUrl);
        JobRequest createOrgUnitJob = createOrganizationalUnit(orgUnit, user, repositoryName);
        waitForJobsToFinish(sleepSecs, createRepoJob, createOrgUnitJob);
        
        JaxbDeploymentJobResult deployJob = createDeploymentUnit(deploymentId, strategy);
        JaxbDeploymentUnit deployUnit = deployJob.getDeploymentUnit();    
        waitForDeploymentToFinish(sleepSecs, deployUnit);
    }
   
    // submethods ------------------------------------------------------------------------------------------------------------
   
    /**
     * Delete the repository with the given repository name
     * @param repositoryName
     * @return A {@link JobRequest} instance returned by the request with the initial status of the request
     */
    private JobRequest deleteRepository(String repositoryName) { 
        logger.info("Deleting repository '{}'", repositoryName);
        RemoveRepositoryRequest entity 
            = delete(createRequest("repositories/" + repositoryName), RemoveRepositoryRequest.class);
        if (entity.getStatus() == JobStatus.ACCEPTED || entity.getStatus() == JobStatus.SUCCESS) {
            return entity;
        } else {
            throw new IllegalStateException("Delete request failed with status " +  entity.getStatus() );
        }
    }
   
    /**
     * Clone a repository in kie-wb with the given name from the given URL
     * @param repositoryName The name of the repository
     * @param cloneRepoUrl The location of the repository
     * @return A {@link JobRequest} instance returned by the request with the initial status of the request
     */
    private JobRequest createRepository(String repositoryName, String cloneRepoUrl) {
        logger.info("Cloning repo '{}' from URL '{}'", repositoryName, cloneRepoUrl);
        RepositoryRequest repoRequest = new RepositoryRequest();
        repoRequest.setName(repositoryName);
        repoRequest.setRequestType("clone");
        repoRequest.setGitURL(cloneRepoUrl);
        String input = serializeToJsonString(repoRequest);
        KieRemoteHttpRequest request = createRequest("repositories/", input);
        return post( request, CreateOrCloneRepositoryRequest.class);
    }
  
    /**
     * Create an organizational unit in order to manage the repository
     * @param name The name of the organizational unit
     * @param owner The owner of the organizational unit
     * @param repositories The list of repositories that the org unit should own
     * @return A {@link JobRequest} instance returned by the request with the initial status of the request
     */
    private JobRequest createOrganizationalUnit(String name, String owner, String... repositories) {
        logger.info("Creating organizational unit '{}' owned by '{}' containing [{}]", name, owner, repositories);
        OrganizationalUnit ou = new OrganizationalUnit();
        ou.setRepositories(new ArrayList<String>());
        for (int i = 0; repositories != null && i < repositories.length; ++i) {
            ou.getRepositories().add(repositories[i]);
        }
        ou.setName(name);
        ou.setOwner(owner);
        String input = serializeToJsonString(ou);
        return post(createRequest("organizationalunits/", input), CreateOrganizationalUnitRequest.class);
    }

    /**
     * Serialize an object to a JSON string
     * @param object The object to be serialized
     * @return The JSON {@link String} instance
     */
    private String serializeToJsonString(Object object) { 
        String input = null;
        try {
            input = new ObjectMapper().writeValueAsString(object);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize " + object.getClass().getSimpleName(), e);
        }
        return input;
    }
   
    /**
     * Do a "maven install" operation on the given project in the given repository
     * @param repositoryName The name of the repository that the project is located in
     * @param project The project to be installed
     * @return A {@link JobRequest} instance returned by the request with the initial status of the request
     */
    private JobRequest installProject(String repositoryName, String project) {
        logger.info("Installing project '{}' from repo '{}'", project, repositoryName);
        KieRemoteHttpRequest request = createMavenOperationRequest(repositoryName, project, "install");
        return post(request, InstallProjectRequest.class);
    }

    /**
     * Create a {@link ClientRequest} to do a maven operation 
     * @param repositoryName The name of the repository where the project is located
     * @param project The project to do the maven operation on 
     * @param operation The maven operation to be executed
     * @return The {@link ClientRequest} to be called
     */
    private KieRemoteHttpRequest createMavenOperationRequest(String repositoryName, String project, String operation) {
        logger.info("Calling maven '{}' operation on project '{}' in repo '{}'", operation, project, repositoryName);
        return createRequest("repositories/" + repositoryName + "/projects/" + project + "/maven/" + operation);
    }
    
    /**
     * Remove (undeploy) the deployment unit specificed
     * @param deploymentId The deployment unit id
     * @return A {@link JaxbDeploymentJobResult} with the initial status of the request
     */
    private JaxbDeploymentJobResult removeDeploymentUnit(String deploymentId) {
        logger.info("Undeploying '{}'", deploymentId);
        KieRemoteHttpRequest request = createRequest("deployment/" + deploymentId + "/undeploy");
        return post(request, JaxbDeploymentJobResult.class);
    }

    /**
     * Create (deploy) the deployment unit specified
     * @param deploymentId The deployment unit id
     * @param strategy The strategy to deploy the deployment unit with 
     * @return A {@link JaxbDeploymentJobResult} with the initial status of the request
     */
    private JaxbDeploymentJobResult createDeploymentUnit(String deploymentId, RuntimeStrategy strategy) { 
        logger.info("Deploying '{}'", deploymentId);
        String opUrl = "deployment/" + deploymentId + "/deploy";
        if (strategy != null ) { 
            opUrl += "?strategy=" + strategy.toString();
        }
       
        KieRemoteHttpRequest request = createRequest(opUrl);
        JaxbDeploymentJobResult jr = post(request, JaxbDeploymentJobResult.class); 
        
        return jr;
    }
 
    // With java 8, this would be SOOOO much shorter and easier.. :/ 
    private <R,S> void waitForJobsToFinish(int sleepSecs, JobRequest ...requests ) { 
       Map<String, JobStatus> requestStatusMap = new HashMap<String, JobStatus>();
     
       int totalTries = 10;
       int allDone = 0;
       int tryCount = 0;
       while( allDone < requests.length && tryCount < totalTries ) { 
           for( JobRequest request : requests ) { 
               String jobId = request.getJobId();
               JobStatus jobStatus  = requestStatusMap.get(jobId);
               if( JobStatus.SUCCESS.equals(jobStatus) ) { 
                  ++allDone;
                  continue;
               }
               KieRemoteHttpRequest restRequest = createRequest( "jobs/" + jobId);
               JobResult jobResult = get(restRequest, JobResult.class);
               requestStatusMap.put(jobId, jobResult.getStatus());
           }
           ++tryCount;
           try { 
               Thread.sleep(sleepSecs*1000);
           } catch( Exception e ) { 
               logger.error("Unable to sleep: " + e.getMessage(), e);
           }
       }
    }
   
    // With java 8, this would be SOOOO much shorter and easier.. :/ 
    private void waitForDeploymentToFinish(int sleepSecs, JaxbDeploymentUnit ...deployUnits ) { 
        Map<String, JaxbDeploymentStatus> requestStatusMap = new HashMap<String, JaxbDeploymentStatus>();
      
        int totalTries = 10;
        int allDone = 0;
        int tryCount = 0;
        while( allDone < deployUnits.length && tryCount < totalTries ) { 
            for( JaxbDeploymentUnit deployUnit : deployUnits ) { 
                String deployId = deployUnit.getIdentifier();
                JaxbDeploymentStatus jobStatus  = requestStatusMap.get(deployId);
                if( JaxbDeploymentStatus.DEPLOYED.equals(jobStatus) ) { 
                   ++allDone;
                   continue;
                }
                KieRemoteHttpRequest restRequest = createRequest("deployment/" + deployId);
                JaxbDeploymentUnit requestedDeployUnit = get(restRequest, JaxbDeploymentUnit.class);
                requestStatusMap.put(deployId, requestedDeployUnit.getStatus());
            }
            ++tryCount;
            try { 
                Thread.sleep(sleepSecs*1000);
            } catch( Exception e ) { 
                logger.error("Unable to sleep: " + e.getMessage(), e);
            }
        }
     }
    
    // Helper methods -------------------------------------------------------------------------------------------------------------
   
    /**
     * Create a {@link ClientRequest} to be called
     * @param relativeUrl The url of the REST call to be made, relative to the ../rest/ base
     * @return
     */
    private KieRemoteHttpRequest createRequest(String relativeUrl) { 
       return requestCreator.createRequest(relativeUrl);
    }
    
    private KieRemoteHttpRequest createRequest(String resourcePath, String body) {
        return createRequest(resourcePath).body(body);
    }
  
    private static final int GET = 0;
    private static final int POST = 1;
    private static final int DELETE = 2;

    private <T extends Object> T get(KieRemoteHttpRequest request, Class<T> returnType) {
        return process(request, GET, returnType);
    }

    private <T extends Object> T post(KieRemoteHttpRequest request, Class<T> returnType) {
        return process(request, POST, returnType);
    }

    private <T extends Object> T delete(KieRemoteHttpRequest request, Class<T> returnType) {
        return process(request, DELETE, returnType);
    }
    
    private <T extends Object> T process(KieRemoteHttpRequest request, int method, Class<T> returnType) { 
        KieRemoteHttpResponse response = null;
        try {
            switch (method) {
            case GET:
                logger.info("]] [GET] " + request.getUri().toString());
                response = request.get().response();
                break;
            case POST:
                logger.info("]] [POST] " + request.getUri().toString());
                response = request.post().response();
                break;
            case DELETE:
                logger.info("]] [DELETE] " + request.getUri().toString());
                response = request.delete().response();
                break;
            default:
                throw new AssertionError();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        String responseBody = response.body();
        try {
            String contentType = response.contentType();
            if (MediaType.APPLICATION_JSON.equals(contentType) || MediaType.APPLICATION_XML.equals(contentType)) {
                try {
                    checkResponse(response, responseBody);
                } catch (Exception ex) { 
                    // TODO add throws to all the operations
                    ex.printStackTrace();
                    fail(  "Unable to do REST operation: " + ex.getMessage() ); 
                }
                T res = new ObjectMapper().readValue(responseBody, returnType);
                return res;
            } else if( contentType.startsWith(MediaType.TEXT_HTML) ) {
                // now that we know that the result is wrong, try to identify the reason
                Document doc = Jsoup.parse(responseBody);
                String errorBody = doc.body().text();
                logger.error("Failed cloning repository. Full response body on DEBUG.");
                logger.debug("Repository cloning response body:\n {}", errorBody);
                throw new IllegalStateException("Failed cloning repository.");
            } else { 
                throw new IllegalStateException("Unexpected content-type: " + contentType);
            }
        } catch (Exception ex) {
            logger.error("Bad entity: {}", responseBody );
            throw new IllegalStateException(ex);
        } finally {
            request.disconnect();
        }
    }

    private void checkResponse(KieRemoteHttpResponse response, String body) throws Exception {
        int status = response.code();
        if (status == Status.BAD_REQUEST.ordinal()) {
            throw new BadRequestException(response.body());
        } else if (status != Status.OK.getStatusCode()
                && status != Status.ACCEPTED.getStatusCode()
                && status != Status.NOT_FOUND.getStatusCode()) {
            throw new IllegalStateException("Request operation failed. Response status = " + status + "\n\n" + response.body());
        } else {
            logger.info("Response entity: [{}]", body);
        }
    }

}
