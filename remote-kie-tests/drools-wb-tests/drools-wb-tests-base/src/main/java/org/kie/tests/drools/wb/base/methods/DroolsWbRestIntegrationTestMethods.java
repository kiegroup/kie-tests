package org.kie.tests.drools.wb.base.methods;

import static org.kie.remote.tests.base.RestUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.kie.tests.drools.wb.base.util.TestConstants.PASSWORD;
import static org.kie.tests.drools.wb.base.util.TestConstants.USER;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.guvnor.rest.client.AddRepositoryToOrganizationalUnitRequest;
import org.guvnor.rest.client.CompileProjectRequest;
import org.guvnor.rest.client.CreateOrCloneRepositoryRequest;
import org.guvnor.rest.client.CreateOrganizationalUnitRequest;
import org.guvnor.rest.client.CreateProjectRequest;
import org.guvnor.rest.client.Entity;
import org.guvnor.rest.client.JobResult;
import org.guvnor.rest.client.JobStatus;
import org.guvnor.rest.client.OrganizationalUnit;
import org.guvnor.rest.client.RemoveRepositoryFromOrganizationalUnitRequest;
import org.guvnor.rest.client.RepositoryRequest;
import org.guvnor.rest.client.RepositoryResponse;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.exception.ResteasyRedirectException;
import org.junit.Test;
import org.kie.services.client.api.RestRequestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * These are various tests for the drools-wb-rest module
 */
public class DroolsWbRestIntegrationTestMethods extends DroolsWbRestIntegrationTestHelperMethods {

    private static Logger logger = LoggerFactory.getLogger(DroolsWbRestIntegrationTestMethods.class);

    private final int maxTries = 10;
    public final boolean useFormBaseAuthentiation;
   
    private final MediaType mediaType = MediaType.APPLICATION_JSON_TYPE;
    
    public DroolsWbRestIntegrationTestMethods() {
        this.useFormBaseAuthentiation = false;
    }
    
    public DroolsWbRestIntegrationTestMethods(boolean formBasedAuth) { 
        this.useFormBaseAuthentiation = formBasedAuth;
    }

    private RestRequestHelper getRestRequestHelper(URL deploymentUrl) { 
        return RestRequestHelper.newInstance(deploymentUrl, 
                USER, PASSWORD, 
                500, 
                MediaType.APPLICATION_JSON_TYPE, 
                this.useFormBaseAuthentiation);
    }
    
    /**
     * Tests the following REST urls: 
     * 
     * ../rest/repostitories GET
     * ../rest/repostitories POST
     * ../rest/jobs/{id} GET
     * ../rest/repositories/{repo}/projects POST
     * 
     * @param deploymentUrl URL of deployment
     * @throws Exception When things go wrong.. 
     */
    public void manipulatingRepositories(URL deploymentUrl) throws Exception {
        // rest/repositories GET
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl);
        ClientRequest restRequest = requestHelper.createRequest("repositories");
        ClientResponse<?> responseObj = get(restRequest, mediaType);
        Collection<RepositoryResponse> repoResponses = responseObj.getEntity(Collection.class);
        assertTrue( repoResponses.size() > 0 );
        String ufPlaygroundUrl = null;
        Iterator<?> iter = repoResponses.iterator();
        while( iter.hasNext() ) { 
            Map<String, String> repoRespMap = (Map<String, String>) iter.next();
            if( "uf-playground".equals(repoRespMap.get("name")) ) { 
                ufPlaygroundUrl = repoRespMap.get("gitURL");
            }
        }
        assertEquals( "UF-Playground Git URL", "git://uf-playground", ufPlaygroundUrl );
        
        // rest/repositories POST
        restRequest = requestHelper.createRequest("repositories");
        RepositoryRequest newRepo = new RepositoryRequest();
        String repoName = UUID.randomUUID().toString();
        newRepo.setName(repoName);
        newRepo.setDescription("repo for testing rest services");
        newRepo.setRequestType("new");
        addToRequestBody(restRequest, newRepo);
        responseObj = checkTimeResponse(restRequest.post());
        
        CreateOrCloneRepositoryRequest createJobRequest = responseObj.getEntity(CreateOrCloneRepositoryRequest.class);
        logger.debug("]] " + convertObjectToJsonString(createJobRequest));
        assertNotNull( "create repo job request", createJobRequest);
        assertEquals( "job request status", JobStatus.ACCEPTED, createJobRequest.getStatus() );
        String jobId = createJobRequest.getJobId();
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, jobId, createJobRequest.getStatus(), requestHelper);
        
        // rest/repositories/{repoName}/projects POST
        restRequest = requestHelper.createRequest("repositories/" + repoName + "/projects");
        Entity project = new Entity();
        project.setDescription("test project");
        String testProjectName = "test-project";
        project.setName(testProjectName);
        addToRequestBody(restRequest, project);
        responseObj = checkTimeResponse(restRequest.post());
        CreateProjectRequest createProjectRequest = responseObj.getEntity(CreateProjectRequest.class);
        logger.debug("]] " + convertObjectToJsonString(createProjectRequest));
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, jobId, createProjectRequest.getStatus(), requestHelper);
    }
   
    /**
     * Tests the following REST urls: 
     * 
     * ../rest/repositories GET
     * ../rest/repositories/{repo}/projecst POST
     * ../rest/jobs/{id} GET
     *
     * @param deploymentUrl
     * @throws Exception
     */
    @Test
    public void mavenOperations(URL deploymentUrl) throws Exception { 
        // rest/repositories GET
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl);
        ClientRequest restRequest = requestHelper.createRequest("repositories");
        ClientResponse<?> responseObj = get(restRequest, mediaType);
        Collection<Map<String, String>> repoResponses = responseObj.getEntity(Collection.class);
        assertTrue( repoResponses.size() > 0 );
        String repoName = repoResponses.iterator().next().get("name");
       
        // rest/repositories/{repoName}/projects POST
        restRequest = requestHelper.createRequest("repositories/" + repoName + "/projects");
        Entity project = new Entity();
        project.setDescription("test project");
        String projectName = UUID.randomUUID().toString();
        project.setName(projectName);
        addToRequestBody(restRequest, project);
        responseObj = checkTimeResponse(restRequest.post());
        CreateProjectRequest createProjectRequest = responseObj.getEntity(CreateProjectRequest.class);
        logger.debug("]] " + convertObjectToJsonString(createProjectRequest));
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, createProjectRequest.getJobId(), createProjectRequest.getStatus(), requestHelper);

        // rest/repositories/{repoName}/projects POST
        restRequest = requestHelper.createRequest("repositories/" + repoName + "/projects/" + projectName + "/maven/compile");
        responseObj = checkTimeResponse(restRequest.post());
        CompileProjectRequest compileRequest = responseObj.getEntity(CompileProjectRequest.class);
        logger.debug("]] " + convertObjectToJsonString(compileRequest));
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, createProjectRequest.getJobId(), createProjectRequest.getStatus(), requestHelper);
       
        // TODO implement GET
        // rest/repositories/{repoName}/projects GET
        /** get projects, compare/verify that new project is in list **/
        
        // TODO implement DELETE
        // rest/repositories/{repoName}/projects DELETE
        /** delete projects, verify that list of projects is now one less */
    }
    
    private void waitForJobToComplete(URL deploymentUrl, String jobId, JobStatus jobStatus, RestRequestHelper requestHelper) throws Exception {
        assertEquals( "Initial status of request should be ACCEPTED", JobStatus.ACCEPTED, jobStatus );
        int wait = 0;
        while( jobStatus.equals(JobStatus.ACCEPTED) && wait < maxTries ) {
            ClientRequest restRequest = requestHelper.createRequest("jobs/" + jobId);
            ClientResponse<?> responseObj = get(restRequest, mediaType);
            JobResult jobResult = responseObj.getEntity(JobResult.class);
            logger.debug( "]] " + convertObjectToJsonString(jobResult) );
            assertEquals( jobResult.getJobId(), jobId );
            jobStatus = jobResult.getStatus();
            ++wait;
            Thread.sleep(3*1000);
        }
        assertTrue( "Too many tries!", wait < maxTries );
    }
   
    /**
     * Tests the following REST urls: 
     * 
     * ../rest/organizationalunits GET
     * ../rest/organizationalunits POST
     * 
     * @param deploymentUrl
     * @throws Exception
     */
    @Test
    public void manipulatingOUs(URL deploymentUrl) throws Exception { 
        // rest/organizaionalunits GET
        RestRequestHelper requestHelper = getRestRequestHelper(deploymentUrl);
        ClientRequest restRequest = requestHelper.createRequest("organizationalunits");
        ClientResponse<?> responseObj = get(restRequest, mediaType);
        Collection<OrganizationalUnit> orgUnits = responseObj.getEntity(Collection.class);
        int origUnitsSize = orgUnits.size();
        
        // rest/organizaionalunits POST
        restRequest = requestHelper.createRequest("organizationalunits");
        OrganizationalUnit orgUnit = new OrganizationalUnit();
        orgUnit.setDescription("Test OU");
        orgUnit.setName(UUID.randomUUID().toString());
        orgUnit.setOwner(this.getClass().getSimpleName());
        addToRequestBody(restRequest, orgUnit);
        responseObj = checkTimeResponse(restRequest.post());
        CreateOrganizationalUnitRequest createOURequest = responseObj.getEntity(CreateOrganizationalUnitRequest.class);

        // rest/jobs/{jobId}
        waitForJobToComplete(deploymentUrl, createOURequest.getJobId(), createOURequest.getStatus(), requestHelper);
       
        // rest/organizaionalunits GET
        restRequest = requestHelper.createRequest("organizationalunits");
        responseObj = get(restRequest, mediaType);
        orgUnits = responseObj.getEntity(Collection.class);
        assertEquals( "Exepcted an OU to be added.", origUnitsSize + 1, orgUnits.size());
        
        // rest/repositories POST
        restRequest = requestHelper.createRequest("repositories");
        RepositoryRequest newRepo = new RepositoryRequest();
        String repoName = UUID.randomUUID().toString();
        newRepo.setName(repoName);
        newRepo.setDescription("repo for testing rest services");
        newRepo.setRequestType("new");
        addToRequestBody(restRequest, newRepo);
        responseObj = checkTimeResponse(restRequest.post());
        
        CreateOrCloneRepositoryRequest createRepoRequest = responseObj.getEntity(CreateOrCloneRepositoryRequest.class);
        logger.debug("]] " + convertObjectToJsonString(createRepoRequest));
        assertNotNull( "create repo job request", createRepoRequest);
        assertEquals( "job request status", JobStatus.ACCEPTED, createRepoRequest.getStatus() );
                
        // rest/jobs/{jobId}
        waitForJobToComplete(deploymentUrl, createRepoRequest.getJobId(), createRepoRequest.getStatus(), requestHelper);
       
        // rest/organizationalunits/{ou}/repositories/{repoName} POST
        restRequest = requestHelper.createRequest("organizationalunits/" + orgUnit.getName() + "/repositories/" + repoName);
        responseObj = checkTimeResponse(restRequest.post());
        
        AddRepositoryToOrganizationalUnitRequest addRepoToOuRequest = responseObj.getEntity(AddRepositoryToOrganizationalUnitRequest.class);
        logger.debug("]] " + convertObjectToJsonString(addRepoToOuRequest));
        assertNotNull( "add repo to ou job request", addRepoToOuRequest);
        assertEquals( "job request status", JobStatus.ACCEPTED, addRepoToOuRequest.getStatus() );
        
        // rest/jobs/{jobId}
        waitForJobToComplete(deploymentUrl, addRepoToOuRequest.getJobId(), addRepoToOuRequest.getStatus(), requestHelper);
       
        // rest/organizationalunits/{ou} GET
        restRequest = requestHelper.createRequest("organizationalunits/" + orgUnit.getName() );
        responseObj = get(restRequest, mediaType);
        
        OrganizationalUnit orgUnitRequest = responseObj.getEntity(OrganizationalUnit.class);
        logger.debug("]] " + convertObjectToJsonString(orgUnitRequest));
        assertNotNull( "organizational unit request", orgUnitRequest);
        
        assertTrue( "repository has not been added to organizational unit", orgUnitRequest.getRepositories().contains(repoName));
        
        // rest/organizationalunits/{ou}/repositories/{repoName} DELETE
        restRequest = requestHelper.createRequest("organizationalunits/" + orgUnit.getName() + "/repositories/" + repoName);
        responseObj = checkTimeResponse(restRequest.delete());
        
        RemoveRepositoryFromOrganizationalUnitRequest remRepoFromOuRquest = responseObj.getEntity(RemoveRepositoryFromOrganizationalUnitRequest.class);
        logger.debug("]] " + convertObjectToJsonString(remRepoFromOuRquest));
        assertNotNull( "add repo to ou job request", remRepoFromOuRquest);
        assertEquals( "job request status", JobStatus.ACCEPTED, remRepoFromOuRquest.getStatus() );
        
        // rest/jobs/{jobId}
        waitForJobToComplete(deploymentUrl, remRepoFromOuRquest.getJobId(), remRepoFromOuRquest.getStatus(), requestHelper);
        
        // rest/organizationalunits/{ou} GET
        restRequest = requestHelper.createRequest("organizationalunits/" + orgUnit.getName() );
        responseObj = get(restRequest, mediaType);
        
        orgUnitRequest = responseObj.getEntity(OrganizationalUnit.class);
        logger.debug("]] " + convertObjectToJsonString(orgUnitRequest));
        assertNotNull( "organizational unit request", orgUnitRequest);
        
        assertFalse( "repository should have been deleted from organizational unit", orgUnitRequest.getRepositories().contains(repoName));
    }

}
