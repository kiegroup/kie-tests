package org.kie.tests.drools.wb.base.methods;

import static org.junit.Assert.*;
import static org.kie.tests.drools.wb.base.methods.TestConstants.PASSWORD;
import static org.kie.tests.drools.wb.base.methods.TestConstants.USER;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.ClientResponse;
import org.junit.Test;
import org.kie.workbench.common.services.shared.rest.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestIntegrationTestMethods extends RestIntegrationTestHelperMethods {

    private static Logger logger = LoggerFactory.getLogger(RestIntegrationTestMethods.class);
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
   
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
        
        // rest/repositories POST
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/repositories").toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
        RepositoryRequest newRepo = new RepositoryRequest();
        String repoName = UUID.randomUUID().toString();
        newRepo.setName(repoName);
        newRepo.setDescription("repo for testing rest services");
        newRepo.setRequestType("new");
        addToRequestBody(restRequest, newRepo);
        logger.info("Before op: " + sdf.format(new Date(System.currentTimeMillis())));
        responseObj = checkResponse(restRequest.post());
        logger.info("After op : " + sdf.format(new Date(System.currentTimeMillis())));
        
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
        logger.info("Before op: " + sdf.format(new Date(System.currentTimeMillis())));
        responseObj = checkResponse(restRequest.post());
        logger.info("After op : " + sdf.format(new Date(System.currentTimeMillis())));
        CreateProjectRequest createProjectRequest = responseObj.getEntity(CreateProjectRequest.class);
        logger.debug("]] " + convertObjectToJsonString(createProjectRequest));
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, jobId, createProjectRequest.getStatus(), requestFactory);
    }
    
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
        logger.info("Before op: " + sdf.format(new Date(System.currentTimeMillis())));
        responseObj = checkResponse(restRequest.post());
        logger.info("After op : " + sdf.format(new Date(System.currentTimeMillis())));
        CreateProjectRequest createProjectRequest = responseObj.getEntity(CreateProjectRequest.class);
        logger.debug("]] " + convertObjectToJsonString(createProjectRequest));
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, createProjectRequest.getJobId(), createProjectRequest.getStatus(), requestFactory);

        // rest/repositories/{repoName}/projects POST
        String mavenOperBase = "rest/repositories/" + repoName + "/projects/" + projectName + "/maven/";
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + mavenOperBase + "compile").toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
        logger.info("Before op: " + sdf.format(new Date(System.currentTimeMillis())));
        responseObj = checkResponse(restRequest.post());
        logger.info("After op : " + sdf.format(new Date(System.currentTimeMillis())));
        CompileProjectRequest compileRequest = responseObj.getEntity(CompileProjectRequest.class);
        logger.debug("]] " + convertObjectToJsonString(compileRequest));
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, createProjectRequest.getJobId(), createProjectRequest.getStatus(), requestFactory);
    }
    
    private void waitForJobToComplete(URL deploymentUrl, String jobId, JobStatus jobStatus, ClientRequestFactory requestFactory) throws Exception {
        while( jobStatus.equals(JobStatus.ACCEPTED) ) {
            String urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/jobs/" + jobId).toExternalForm();
            ClientRequest restRequest = createRequest(requestFactory, urlString);
            ClientResponse<?> responseObj = checkResponse(restRequest.get());
            JobResult jobResult = responseObj.getEntity(JobResult.class);
            logger.debug( "]] " + convertObjectToJsonString(jobResult) );
            assertEquals( jobResult.getJobId(), jobId );
            jobStatus = jobResult.getStatus();
        }
    }
    
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
        logger.info("Before op: " + sdf.format(new Date(System.currentTimeMillis())));
        responseObj = checkResponse(restRequest.post());
        logger.info("After op : " + sdf.format(new Date(System.currentTimeMillis())));
        CreateOrganizationalUnitRequest createOURequest = responseObj.getEntity(CreateOrganizationalUnitRequest.class);

        // rest/jobs/{jobId}
        waitForJobToComplete(deploymentUrl, createOURequest.getJobId(), createOURequest.getStatus(), requestFactory);
        
        // rest/organizaionalunits GET
        urlString = new URL(deploymentUrl,  deploymentUrl.getPath() + "rest/organizationalunits").toExternalForm();
        restRequest = createRequest(requestFactory, urlString);
        responseObj = checkResponse(restRequest.get());
        orgUnits = responseObj.getEntity(Collection.class);
        assertEquals( "Exepcted an OU to be added.", origUnitsSize + 1, orgUnits.size());
    }
    


}
