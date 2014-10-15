package org.kie.tests.drools.wb.base.methods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.kie.tests.drools.wb.base.methods.TestConstants.PASSWORD;
import static org.kie.tests.drools.wb.base.methods.TestConstants.USER;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;
import org.junit.Test;
import org.kie.workbench.common.services.shared.rest.AddRepositoryToOrganizationalUnitRequest;
import org.kie.workbench.common.services.shared.rest.CompileProjectRequest;
import org.kie.workbench.common.services.shared.rest.CreateOrCloneRepositoryRequest;
import org.kie.workbench.common.services.shared.rest.CreateOrganizationalUnitRequest;
import org.kie.workbench.common.services.shared.rest.CreateProjectRequest;
import org.kie.workbench.common.services.shared.rest.Entity;
import org.kie.workbench.common.services.shared.rest.JobResult;
import org.kie.workbench.common.services.shared.rest.JobStatus;
import org.kie.workbench.common.services.shared.rest.OrganizationalUnit;
import org.kie.workbench.common.services.shared.rest.RemoveRepositoryFromOrganizationalUnitRequest;
import org.kie.workbench.common.services.shared.rest.RepositoryRequest;
import org.kie.workbench.common.services.shared.rest.RepositoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * These are various tests for the drools-wb-rest module
 */
public class DroolsWbRestIntegrationTestMethods extends DroolsWbRestIntegrationTestHelperMethods {

    private static Logger logger = LoggerFactory.getLogger(DroolsWbRestIntegrationTestMethods.class);

    private final int maxTries = 10;
  
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
        ClientRequestFactory requestFactory = createBasicAuthRequestFactory(deploymentUrl, USER, PASSWORD);
        String urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/repositories").toExternalForm();
        ClientRequest restRequest = createRequest(requestFactory, urlString);
        ClientResponse<?> responseObj = checkResponse(restRequest.get());
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
        
        { 
            // rest/repositories POST
            urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/repositories").toExternalForm();
            restRequest = createRequest(requestFactory, urlString);
            RepositoryRequest newRepo = new RepositoryRequest();
            String repoName = UUID.randomUUID().toString();
            newRepo.setName(repoName);
            newRepo.setDescription("repo for testing rest services");
            newRepo.setRequestType("create");
            newRepo.setPassword("");
            newRepo.setUserName("");
            addToRequestBody(restRequest, newRepo);
            responseObj = checkTimeResponse(restRequest.post());

            CreateOrCloneRepositoryRequest createJobRequest = responseObj.getEntity(CreateOrCloneRepositoryRequest.class);
            logger.debug("]] " + convertObjectToJsonString(createJobRequest));
            assertNotNull( "create repo job request", createJobRequest);
            assertEquals( "job request status", JobStatus.ACCEPTED, createJobRequest.getStatus() );
            String jobId = createJobRequest.getJobId();

            // rest/jobs/{jobId} GET
            JobResult jobResult = waitForJobToComplete(deploymentUrl, jobId, createJobRequest.getStatus(), requestFactory);
            assertFalse( "Job did not fail: " + jobResult.getStatus(), JobStatus.SUCCESS.equals(jobResult.getStatus()) );
        }

        // rest/repositories POST
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/repositories").toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
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
        waitForJobToComplete(deploymentUrl, jobId, createJobRequest.getStatus(), requestFactory);
        
        // rest/repositories/{repoName}/projects POST
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/repositories/" + repoName + "/projects").toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
        Entity project = new Entity();
        project.setDescription("test project");
        String testProjectName = "test-project";
        project.setName(testProjectName);
        addToRequestBody(restRequest, project);
        responseObj = checkTimeResponse(restRequest.post());
        CreateProjectRequest createProjectRequest = responseObj.getEntity(CreateProjectRequest.class);
        logger.debug("]] " + convertObjectToJsonString(createProjectRequest));
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, jobId, createProjectRequest.getStatus(), requestFactory);
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
        ClientRequestFactory requestFactory = createBasicAuthRequestFactory(deploymentUrl, USER, PASSWORD);
        String urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/repositories").toExternalForm();
        ClientRequest restRequest = createRequest(requestFactory, urlString);
        ClientResponse<?> responseObj = checkResponse(restRequest.get());
        Collection<Map<String, String>> repoResponses = responseObj.getEntity(Collection.class);
        assertTrue( repoResponses.size() > 0 );
        String repoName = repoResponses.iterator().next().get("name");
       
        // rest/repositories/{repoName}/projects POST
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/repositories/" + repoName + "/projects").toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
        Entity project = new Entity();
        project.setDescription("test project");
        String projectName = UUID.randomUUID().toString();
        project.setName(projectName);
        addToRequestBody(restRequest, project);
        responseObj = checkTimeResponse(restRequest.post());
        CreateProjectRequest createProjectRequest = responseObj.getEntity(CreateProjectRequest.class);
        logger.debug("]] " + convertObjectToJsonString(createProjectRequest));
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, createProjectRequest.getJobId(), createProjectRequest.getStatus(), requestFactory);

        // rest/repositories/{repoName}/projects POST
        String mavenOperBase = "rest/repositories/" + repoName + "/projects/" + projectName + "/maven/";
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + mavenOperBase + "compile").toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
        responseObj = checkTimeResponse(restRequest.post());
        CompileProjectRequest compileRequest = responseObj.getEntity(CompileProjectRequest.class);
        logger.debug("]] " + convertObjectToJsonString(compileRequest));
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, createProjectRequest.getJobId(), createProjectRequest.getStatus(), requestFactory);
       
        // TODO implement GET
        // rest/repositories/{repoName}/projects GET
        /** get projects, compare/verify that new project is in list **/
        
        // TODO implement DELETE
        // rest/repositories/{repoName}/projects DELETE
        /** delete projects, verify that list of projects is now one less */
    }
    
    private JobResult waitForJobToComplete(URL deploymentUrl, String jobId, JobStatus jobStatus, ClientRequestFactory requestFactory) throws Exception {
        assertEquals( "Initial status of request should be ACCEPTED", JobStatus.ACCEPTED, jobStatus );
        int wait = 0;
        JobResult jobResult = null;
        while( jobStatus.equals(JobStatus.ACCEPTED) && wait < maxTries ) {
            String urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/jobs/" + jobId).toExternalForm();
            ClientRequest restRequest = createRequest(requestFactory, urlString);
            ClientResponse<?> responseObj = checkResponse(restRequest.get());
            jobResult = responseObj.getEntity(JobResult.class);
            logger.debug( "]] " + convertObjectToJsonString(jobResult) );
            assertEquals( jobResult.getJobId(), jobId );
            jobStatus = jobResult.getStatus();
            ++wait;
            Thread.sleep(3*1000);
        }
        assertTrue( "Too many tries!", wait < maxTries );
        
        return jobResult;
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
        ClientRequestFactory requestFactory = createBasicAuthRequestFactory(deploymentUrl, USER, PASSWORD);
        String urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/organizationalunits").toExternalForm();
        ClientRequest restRequest = createRequest(requestFactory, urlString);
        ClientResponse<?> responseObj = checkResponse(restRequest.get());
        Collection<OrganizationalUnit> orgUnits = responseObj.getEntity(Collection.class);
        int origUnitsSize = orgUnits.size();
        
        // rest/organizaionalunits POST
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/organizationalunits").toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
        OrganizationalUnit orgUnit = new OrganizationalUnit();
        orgUnit.setDescription("Test OU");
        orgUnit.setName(UUID.randomUUID().toString());
        orgUnit.setOwner(this.getClass().getSimpleName());
        addToRequestBody(restRequest, orgUnit);
        responseObj = checkTimeResponse(restRequest.post());
        CreateOrganizationalUnitRequest createOURequest = responseObj.getEntity(CreateOrganizationalUnitRequest.class);

        // rest/jobs/{jobId}
        waitForJobToComplete(deploymentUrl, createOURequest.getJobId(), createOURequest.getStatus(), requestFactory);
       
        // rest/organizaionalunits GET
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/organizationalunits").toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
        responseObj = checkResponse(restRequest.get());
        orgUnits = responseObj.getEntity(Collection.class);
        assertEquals( "Exepcted an OU to be added.", origUnitsSize + 1, orgUnits.size());
        
        // rest/repositories POST
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/repositories").toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
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
        waitForJobToComplete(deploymentUrl, createRepoRequest.getJobId(), createRepoRequest.getStatus(), requestFactory);
       
        // rest/organizationalunits/{ou}/repositories/{repoName} POST
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/organizationalunits/" + orgUnit.getName() + "/repositories/" + repoName).toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
        responseObj = checkTimeResponse(restRequest.post());
        
        AddRepositoryToOrganizationalUnitRequest addRepoToOuRequest = responseObj.getEntity(AddRepositoryToOrganizationalUnitRequest.class);
        logger.debug("]] " + convertObjectToJsonString(addRepoToOuRequest));
        assertNotNull( "add repo to ou job request", addRepoToOuRequest);
        assertEquals( "job request status", JobStatus.ACCEPTED, addRepoToOuRequest.getStatus() );
        
        // rest/jobs/{jobId}
        waitForJobToComplete(deploymentUrl, addRepoToOuRequest.getJobId(), addRepoToOuRequest.getStatus(), requestFactory);
       
        // rest/organizationalunits/{ou} GET
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/organizationalunits/" + orgUnit.getName() ).toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
        responseObj = checkResponse(restRequest.get());
        
        OrganizationalUnit orgUnitRequest = responseObj.getEntity(OrganizationalUnit.class);
        logger.debug("]] " + convertObjectToJsonString(orgUnitRequest));
        assertNotNull( "organizational unit request", orgUnitRequest);
        
        assertTrue( "repository has not been added to organizational unit", orgUnitRequest.getRepositories().contains(repoName));
        
        // rest/organizationalunits/{ou}/repositories/{repoName} DELETE
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/organizationalunits/" + orgUnit.getName() + "/repositories/" + repoName).toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
        responseObj = checkTimeResponse(restRequest.delete());
        
        RemoveRepositoryFromOrganizationalUnitRequest remRepoFromOuRquest = responseObj.getEntity(RemoveRepositoryFromOrganizationalUnitRequest.class);
        logger.debug("]] " + convertObjectToJsonString(remRepoFromOuRquest));
        assertNotNull( "add repo to ou job request", remRepoFromOuRquest);
        assertEquals( "job request status", JobStatus.ACCEPTED, remRepoFromOuRquest.getStatus() );
        
        // rest/jobs/{jobId}
        waitForJobToComplete(deploymentUrl, remRepoFromOuRquest.getJobId(), remRepoFromOuRquest.getStatus(), requestFactory);
        
        // rest/organizationalunits/{ou} GET
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/organizationalunits/" + orgUnit.getName() ).toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
        responseObj = checkResponse(restRequest.get());
        
        orgUnitRequest = responseObj.getEntity(OrganizationalUnit.class);
        logger.debug("]] " + convertObjectToJsonString(orgUnitRequest));
        assertNotNull( "organizational unit request", orgUnitRequest);
        
        assertFalse( "repository should have been deleted from organizational unit", orgUnitRequest.getRepositories().contains(repoName));
    }

}
