package org.kie.tests.drools.wb.base.methods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kie.tests.drools.wb.base.util.TestConstants.PASSWORD;
import static org.kie.tests.drools.wb.base.util.TestConstants.USER;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.ObjectMapper;
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
import org.jboss.resteasy.client.ClientResponse;
import org.junit.Test;
import org.kie.remote.common.rest.KieRemoteHttpRequest;
import org.kie.remote.tests.base.AbstractKieRemoteRestMethods;
import org.kie.remote.tests.base.RestRequestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * These are various tests for the drools-wb-rest module
 */
public class KieDroolsWbRestIntegrationTestMethods extends AbstractKieRemoteRestMethods {

    private static Logger logger = LoggerFactory.getLogger(KieDroolsWbRestIntegrationTestMethods.class);

    private final int maxTries = 10;
   
    private final MediaType mediaType = MediaType.APPLICATION_JSON_TYPE;
   
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    
    private static ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS);
    }
        
    protected String serializeToJson(Object object) {
        String result = null;
        try {
            result =  mapper.writeValueAsString(object);
        } catch( Exception e ) {
            logger.error("Unable to serialize {} instance to JSON:\n{}", object.getClass().getSimpleName(), e);
            fail("Unable to deserialize JSON string, see log.");
        }
        return result;
    }
        
    private RequestCreator getRequestCreator(URL deploymentUrl) { 
        return new RequestCreator(deploymentUrl, 
                USER, PASSWORD, 
                MediaType.APPLICATION_JSON_TYPE);
    }
  
    private RestRequestHelper getRestRequestHelper(URL deploymentUrl) { 
        return RestRequestHelper.newInstance(deploymentUrl, 
                USER, PASSWORD, 
                500, 
                MediaType.APPLICATION_JSON_TYPE);
    }
  
    @Override
    public String getContentType() { 
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public <T> T deserializeXml( String xmlStr, Class<T> entityClass ) { 
        return noXmlContent(this, entityClass);
    }

    public <T> T deserializeJson( String jsonStr, Class<T> entityClass ) { 
        T result = null;
        try {
            result =  mapper.readValue(jsonStr, entityClass);
        } catch( Exception e ) {
            logger.error("Unable to deserialize {} instance from JSON:\n{}", entityClass.getSimpleName(), jsonStr, e);
            fail("Unable to deserialize JSON string, see log.");
        }
        return result;
    }
   
    private class RequestCreator {

        private final URL baseUrl;
        private final String userName;
        private final String password;
        private final MediaType contentType;

        public RequestCreator(URL baseUrl, String user, String password, MediaType mediaType) {
            StringBuilder urlString = new StringBuilder(baseUrl.toString());
            if( !urlString.toString().endsWith("/") ) {
                urlString.append("/");
            }
            urlString.append("rest/");
            try {
                this.baseUrl = new URL(urlString.toString());
            } catch(Exception e) { 
                e.printStackTrace();
                throw new IllegalStateException("Invalid url: " +  urlString, e);
            }
            this.userName = user;
            this.password = password;
            this.contentType = mediaType;
        }

        public KieRemoteHttpRequest createRequest( String relativeUrl ) {
            KieRemoteHttpRequest request = KieRemoteHttpRequest.newRequest(baseUrl).basicAuthorization(userName, password)
                    .relativeRequest(relativeUrl).accept(contentType.toString());
            return request;
        }
    }
    
    // Test methods ---------------------------------------------------------------------------------------------------------------
    
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
        RequestCreator requestCreator = getRequestCreator(deploymentUrl);
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("repositories");
        Collection<RepositoryResponse> repoResponses = get(httpRequest, Collection.class);
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
            httpRequest = requestCreator.createRequest("repositories");
            RepositoryRequest newRepo = new RepositoryRequest();
            String repoName = UUID.randomUUID().toString();
            newRepo.setName(repoName);
            newRepo.setDescription("repo for testing rest services");
            newRepo.setRequestType("new");
            newRepo.setPassword("");
            newRepo.setUserName("");
            addToRequestBody(httpRequest, newRepo);

            CreateOrCloneRepositoryRequest createJobRequest = postCheckTime(httpRequest, 202, CreateOrCloneRepositoryRequest.class);
            logger.debug("]] " + httpRequest.response().body() );
            assertNotNull( "create repo job request", createJobRequest);
            assertEquals( "job request status", JobStatus.ACCEPTED, createJobRequest.getStatus() );
            String jobId = createJobRequest.getJobId();

            // rest/jobs/{jobId} GET
            JobResult jobResult = waitForJobToComplete(deploymentUrl, jobId, createJobRequest.getStatus(), requestCreator);
            assertFalse( "Job did not fail: " + jobResult.getStatus(), JobStatus.SUCCESS.equals(jobResult.getStatus()) );
        }

        // rest/repositories POST
        httpRequest = requestCreator.createRequest("repositories");
        RepositoryRequest newRepo = new RepositoryRequest();
        String repoName = UUID.randomUUID().toString();
        newRepo.setName(repoName);
        newRepo.setDescription("repo for testing rest services");
        newRepo.setRequestType("new");
        
        addToRequestBody(httpRequest, newRepo);
        
        CreateOrCloneRepositoryRequest createJobRequest = postCheckTime(httpRequest, 202, CreateOrCloneRepositoryRequest.class);
        logger.debug("]] " + httpRequest.response().body() );
        assertNotNull( "create repo job request", createJobRequest);
        assertEquals( "job request status", JobStatus.ACCEPTED, createJobRequest.getStatus() );
        String jobId = createJobRequest.getJobId();
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, jobId, createJobRequest.getStatus(), requestCreator);
        
        // rest/repositories/{repoName}/projects POST
        httpRequest = requestCreator.createRequest("repositories/" + repoName + "/projects");
        Entity project = new Entity();
        project.setDescription("test project");
        String testProjectName = "test-project";
        project.setName(testProjectName);
        addToRequestBody(httpRequest, project);
        CreateProjectRequest createProjectRequest = postCheckTime(httpRequest, 202, CreateProjectRequest.class);
        logger.debug("]] " + httpRequest.response().body());
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, jobId, createProjectRequest.getStatus(), requestCreator);
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
        RequestCreator requestCreator = getRequestCreator(deploymentUrl);
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("repositories");
        Collection<Map<String, String>> repoResponses = get(httpRequest, Collection.class);
        assertTrue( repoResponses.size() > 0 );
        String repoName = repoResponses.iterator().next().get("name");
       
        String projectName = UUID.randomUUID().toString();
        {
        // rest/repositories/{repoName}/projects POST
        httpRequest = requestCreator.createRequest("repositories/" + repoName + "/projects");
        Entity project = new Entity();
        project.setDescription("test project");
        project.setName(projectName);
        addToRequestBody(httpRequest, project);
        CreateProjectRequest createProjectRequest = postCheckTime(httpRequest, 202, CreateProjectRequest.class);
        assertNotNull( "Empty response object", createProjectRequest );
        logger.debug("]] " + httpRequest.response().body() );
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, createProjectRequest.getJobId(), createProjectRequest.getStatus(), requestCreator);
        }

        {
        // rest/repositories/{repoName}/projects POST
        httpRequest = requestCreator.createRequest("repositories/" + repoName + "/projects/" + projectName + "/maven/compile");
        CompileProjectRequest compileRequest = postCheckTime(httpRequest, 202, CompileProjectRequest.class);
        assertNotNull( "Empty response object", compileRequest );
        logger.debug("]] " + httpRequest.response().body());
        
        // rest/jobs/{jobId} GET
        waitForJobToComplete(deploymentUrl, compileRequest.getJobId(), compileRequest.getStatus(), requestCreator);
        }
       
        // TODO implement GET
        // rest/repositories/{repoName}/projects GET
        /** get projects, compare/verify that new project is in list **/
        
        // TODO implement DELETE
        // rest/repositories/{repoName}/projects DELETE
        /** delete projects, verify that list of projects is now one less */
    }
    
    private JobResult waitForJobToComplete(URL deploymentUrl, String jobId, JobStatus jobStatus, RequestCreator requestCreator) throws Exception {
        assertEquals( "Initial status of request should be ACCEPTED", JobStatus.ACCEPTED, jobStatus );
        int wait = 0;
        JobResult jobResult = null;
        while( jobStatus.equals(JobStatus.ACCEPTED) && wait < maxTries ) {
            KieRemoteHttpRequest httpRequest = requestCreator.createRequest("jobs/" + jobId);
            jobResult = get(httpRequest, JobResult.class);
            logger.debug( "]] " + httpRequest.response().body() );
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
        RequestCreator requestCreator = getRequestCreator(deploymentUrl);
        KieRemoteHttpRequest httpRequest = requestCreator.createRequest("organizationalunits");
        Collection<OrganizationalUnit> orgUnits = get( httpRequest, Collection.class);
        int origUnitsSize = orgUnits.size();
        
        // rest/organizaionalunits POST
        httpRequest = requestCreator.createRequest("organizationalunits");
        OrganizationalUnit orgUnit = new OrganizationalUnit();
        orgUnit.setDescription("Test OU");
        orgUnit.setName(UUID.randomUUID().toString());
        orgUnit.setOwner(this.getClass().getSimpleName());
        addToRequestBody(httpRequest, orgUnit);
        CreateOrganizationalUnitRequest createOURequest = postCheckTime(httpRequest, 202, CreateOrganizationalUnitRequest.class);

        // rest/jobs/{jobId}
        waitForJobToComplete(deploymentUrl, createOURequest.getJobId(), createOURequest.getStatus(), requestCreator);
       
        // rest/organizaionalunits GET
        httpRequest = requestCreator.createRequest("organizationalunits");
        orgUnits = get(httpRequest, Collection.class);
        assertEquals( "Exepcted an OU to be added.", origUnitsSize + 1, orgUnits.size());
        
        // rest/repositories POST
        httpRequest = requestCreator.createRequest("repositories");
        RepositoryRequest newRepo = new RepositoryRequest();
        String repoName = UUID.randomUUID().toString();
        newRepo.setName(repoName);
        newRepo.setDescription("repo for testing rest services");
        newRepo.setRequestType("new");
        addToRequestBody(httpRequest, newRepo);
        
        CreateOrCloneRepositoryRequest createRepoRequest = postCheckTime(httpRequest, 202, CreateOrCloneRepositoryRequest.class);
        logger.debug("]] " + httpRequest.response().body());
        assertNotNull( "create repo job request", createRepoRequest);
        assertEquals( "job request status", JobStatus.ACCEPTED, createRepoRequest.getStatus() );
                
        // rest/jobs/{jobId}
        waitForJobToComplete(deploymentUrl, createRepoRequest.getJobId(), createRepoRequest.getStatus(), requestCreator);
       
        // rest/organizationalunits/{ou}/repositories/{repoName} POST
        httpRequest = requestCreator.createRequest("organizationalunits/" + orgUnit.getName() + "/repositories/" + repoName);
        
        AddRepositoryToOrganizationalUnitRequest addRepoToOuRequest = postCheckTime(httpRequest, 202, AddRepositoryToOrganizationalUnitRequest.class);
        logger.debug("]] " + httpRequest.response().body());
        assertNotNull( "add repo to ou job request", addRepoToOuRequest);
        assertEquals( "job request status", JobStatus.ACCEPTED, addRepoToOuRequest.getStatus() );
        
        // rest/jobs/{jobId}
        waitForJobToComplete(deploymentUrl, addRepoToOuRequest.getJobId(), addRepoToOuRequest.getStatus(), requestCreator);
       
        // rest/organizationalunits/{ou} GET
        httpRequest = requestCreator.createRequest("organizationalunits/" + orgUnit.getName() );
        
        OrganizationalUnit orgUnitRequest = get( httpRequest, OrganizationalUnit.class);
        logger.debug("]] " + httpRequest.response().body() );
        assertNotNull( "organizational unit request", orgUnitRequest);
        
        assertTrue( "repository has not been added to organizational unit", orgUnitRequest.getRepositories().contains(repoName));
        
        // rest/organizationalunits/{ou}/repositories/{repoName} DELETE
        httpRequest = requestCreator.createRequest("organizationalunits/" + orgUnit.getName() + "/repositories/" + repoName);
        
        RemoveRepositoryFromOrganizationalUnitRequest remRepoFromOuRquest = postCheckTime(httpRequest, 202, RemoveRepositoryFromOrganizationalUnitRequest.class);
        logger.debug("]] " + httpRequest.response().body() );
        assertNotNull( "add repo to ou job request", remRepoFromOuRquest);
        assertEquals( "job request status", JobStatus.ACCEPTED, remRepoFromOuRquest.getStatus() );
        
        // rest/jobs/{jobId}
        waitForJobToComplete(deploymentUrl, remRepoFromOuRquest.getJobId(), remRepoFromOuRquest.getStatus(), requestCreator);
        
        // rest/organizationalunits/{ou} GET
        httpRequest = requestCreator.createRequest("organizationalunits/" + orgUnit.getName() );
        
        orgUnitRequest = get( httpRequest, OrganizationalUnit.class);
        logger.debug("]] " + httpRequest.response().body() );
        assertNotNull( "organizational unit request", orgUnitRequest);
        
        assertFalse( "repository should have been deleted from organizational unit", orgUnitRequest.getRepositories().contains(repoName));
    }

}
