package org.kie.tests.drools.wb.base.methods;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kie.tests.drools.wb.base.util.TestConstants.MARY_PASSWORD;
import static org.kie.tests.drools.wb.base.util.TestConstants.MARY_USER;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.guvnor.rest.client.AddRepositoryToOrganizationalUnitRequest;
import org.guvnor.rest.client.CompileProjectRequest;
import org.guvnor.rest.client.CreateOrCloneRepositoryRequest;
import org.guvnor.rest.client.CreateOrganizationalUnitRequest;
import org.guvnor.rest.client.CreateProjectRequest;
import org.guvnor.rest.client.DeleteProjectRequest;
import org.guvnor.rest.client.Entity;
import org.guvnor.rest.client.JobResult;
import org.guvnor.rest.client.JobStatus;
import org.guvnor.rest.client.OrganizationalUnit;
import org.guvnor.rest.client.ProjectRequest;
import org.guvnor.rest.client.ProjectResponse;
import org.guvnor.rest.client.RemoveRepositoryFromOrganizationalUnitRequest;
import org.guvnor.rest.client.RepositoryRequest;
import org.guvnor.rest.client.RepositoryResponse;
import org.junit.Test;
import org.kie.remote.tests.base.RestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * These are various tests for the drools-wb-rest module
 */
public class KieDroolsWbRestIntegrationTestMethods {

    private static Logger logger = LoggerFactory.getLogger(KieDroolsWbRestIntegrationTestMethods.class);

    private final int maxTries = 10;
    private static final Random random = new Random();
 
    private URL deploymentUrl;
    private String contentType = MediaType.APPLICATION_JSON;
    private String user = MARY_USER; 
    private String password = MARY_PASSWORD; 
    
    private void setDeploymentUrl(URL deploymentUrl) { 
        this.deploymentUrl = deploymentUrl;
    }

    // Helper methods -------------------------------------------------------------------------------------------------------------
   
    
    private <T> T post(String relativeUrl, String user, String password, int status, Class<T>... returnType) {
        return RestUtil.post(deploymentUrl, "rest/" + relativeUrl, contentType,
                status, user, password,
                returnType);
    }
    
    private <T> T postTimeout(String relativeUrl, int status, double timeoutInSecs, Class<T>... returnTypes) {
        return RestUtil.postEntity(deploymentUrl, "rest/" + relativeUrl, contentType,
                status, user, password, timeoutInSecs,
                returnTypes);
    }
    
    private <T> T postTimeout(String relativeUrl, int status, double timeoutInSecs, Object entity, Class<T>... returnTypes) {
        return RestUtil.postEntity(deploymentUrl, "rest/" + relativeUrl, contentType,
                status, user, password, timeoutInSecs,
                entity, returnTypes);
    }
    
    private <T> T post(String relativeUrl, int status, Object entity, Class<T>... returnTypes) {
        return RestUtil.postEntity(deploymentUrl, "rest/" + relativeUrl, contentType,
                status, user, password,
                entity, returnTypes);
    }
    
    private <T> T get(String relativeUrl, int status, Class... returnTypes) {
        return RestUtil.get(deploymentUrl, "rest/" + relativeUrl, contentType,
                status, user, password,
                returnTypes);
    }
    
    private <T> T delete(String relativeUrl, int status, Class... returnTypes) {
        return RestUtil.delete(deploymentUrl, "rest/" + relativeUrl, contentType,
                status, user, password,
                returnTypes);
    }
    
    // Test methods ---------------------------------------------------------------------------------------------------------------
    
    /**
     * Tests the following REST urls: 
     * 
     * ../rest/repostitories GET
     * ../rest/repostitories POST
     * ../rest/jobs/{id} GET
     * ../rest/repositories/{repo}/projects POST>
     * ../rest/repositories/{repo}/projects GET
     * ../rest/repositories/{repo}/projects DELETE
     * 
     * @param deploymentUrl URL of deployment
     * @throws Exception When things go wrong.. 
     */
    public void manipulatingRepositories(URL deploymentUrl) throws Exception {
        setDeploymentUrl(deploymentUrl);
        
        // rest/repositories GET
        Collection<RepositoryResponse> repoResponses = get("repositories", 200, Collection.class, RepositoryResponse.class);

        assertTrue( repoResponses.size() > 0 );
        String ufPlaygroundUrl = null;
        Iterator<RepositoryResponse> iter = repoResponses.iterator();
        while( iter.hasNext() ) { 
            RepositoryResponse repoResp = iter.next();
            if( "uf-playground".equals(repoResp.getName()) ) { 
                ufPlaygroundUrl = repoResp.getGitURL();
                break;
            }
        }
        assertEquals( "UF-Playground Git URL", "git://uf-playground", ufPlaygroundUrl );
        
        { 
            // rest/repositories POST
            RepositoryRequest newRepo = new RepositoryRequest();
            String repoName = UUID.randomUUID().toString();
            newRepo.setName(repoName);
            newRepo.setDescription("repo for testing rest services");
            newRepo.setRequestType("create");
            newRepo.setPassword("");
            newRepo.setUserName("");

            post("repositories", Status.BAD_REQUEST.getStatusCode(), newRepo);
        }

        String repoName = UUID.randomUUID().toString();
       
        {
            // rest/repositories POST
            RepositoryRequest newRepo = new RepositoryRequest();
            newRepo.setName(repoName);
            newRepo.setDescription("repo for testing rest services");
            newRepo.setRequestType("new");

            CreateOrCloneRepositoryRequest createJobRequest = postTimeout("repositories", 202, 1, newRepo, CreateOrCloneRepositoryRequest.class);
            assertNotNull( "create repo job request", createJobRequest);
            JobStatus requestStatus = createJobRequest.getStatus();
            assertTrue( "job request status: " + requestStatus, JobStatus.ACCEPTED.equals(requestStatus) || JobStatus.APPROVED.equals(requestStatus) );

            // rest/jobs/{jobId} GET
            waitForJobToComplete(deploymentUrl, createJobRequest.getJobId(), createJobRequest.getStatus());
        }
       
        { 
            // rest/repositories/{repoName}/projects POST
            // - backwards compatibility
            Entity project = new Entity();
            project.setDescription("random project");
            String testProjectName = UUID.randomUUID().toString();
            project.setName(testProjectName);

            CreateProjectRequest createProjectRequest = postTimeout("repositories/" + repoName + "/projects", 202, 0.5, project, CreateProjectRequest.class);

            // rest/jobs/{jobId} GET
            waitForJobToComplete(deploymentUrl, createProjectRequest.getJobId(), createProjectRequest.getStatus());
        }
       
        ProjectRequest newProject = new ProjectRequest();
        {
            // rest/repositories/{repoName}/projects POST
            String testProjectName = UUID.randomUUID().toString();
            newProject.setDescription("test get/del project");
            newProject.setName(testProjectName);
            String testProjectGroupid = UUID.randomUUID().toString();
            newProject.setGroupId(testProjectGroupid);
            String testVersion = "" + random.nextInt(100) + ".0";
            newProject.setVersion(testVersion);
            CreateProjectRequest createProjectRequest = postTimeout("repositories/" + repoName + "/projects", 202, 0.5, newProject, CreateProjectRequest.class);

            // rest/jobs/{jobId} GET
            waitForJobToComplete(deploymentUrl, createProjectRequest.getJobId(), createProjectRequest.getStatus());
        }
        
        // rest/repositories/{repoName}/projects GET
        Collection<ProjectResponse> projectResponses = get("repositories/" + repoName + "/projects", 200, Collection.class, ProjectResponse.class);
        
        assertNotNull( "Null project request list", projectResponses );
        assertFalse( "Empty project request list", projectResponses.isEmpty() );
        ProjectRequest foundProjReq = null;
        for( ProjectRequest projReq : projectResponses ) { 
           if( newProject.getName().equals(projReq.getName()) ) { 
              foundProjReq = projReq;
              break;
           }
        }
        assertNotNull( "Could not find project", foundProjReq ); 
        assertEquals( "Project group id", newProject.getGroupId(), foundProjReq.getGroupId() );
        assertEquals( "Project version", newProject.getVersion(), foundProjReq.getVersion() );
       
        {
            // rest/repositories/{repoName}/projects DELETE
            DeleteProjectRequest deleteProjectRequest = delete(
                    "repositories/" + repoName + "/projects/" + newProject.getName(), 
                    202, 
                    DeleteProjectRequest.class);

            // rest/jobs/{jobId} GET
            waitForJobToComplete(deploymentUrl, deleteProjectRequest.getJobId(), deleteProjectRequest.getStatus());
        }

        // rest/repositories/{repoName}/projects GET
        projectResponses = get( "repositories/" + repoName + "/projects", 200, Collection.class, ProjectResponse.class);
        
        
        assertNotNull( "Null project request list", projectResponses );
        assertFalse( "Empty project request list", projectResponses.isEmpty() );
        
        foundProjReq = null;
        for( ProjectRequest projReq : projectResponses ) { 
           if( newProject.getName().equals(projReq.getName()) ) { 
              foundProjReq = projReq;
              break;
           }
        }
        assertNull( "Project was not deleted!", foundProjReq ); 
    }
   
    /**
     * Tests the following REST urls: 
     * 
     * ../rest/repositories GET
     * ../rest/repositories/{repo}/projects POST
     * ../rest/jobs/{id} GET
     *
     * @param deploymentUrl
     * @throws Exception
     */
    @Test
    public void mavenOperations(URL deploymentUrl) throws Exception { 
        setDeploymentUrl(deploymentUrl);
        
        // rest/repositories GET
        Collection<RepositoryResponse> repoResponses = get("repositories", 200, Collection.class, RepositoryResponse.class);
        assertFalse( "Empty repository responses list", repoResponses.isEmpty() );
        String repoName = repoResponses.iterator().next().getName();
       
        String projectName = UUID.randomUUID().toString();
        {
            // rest/repositories/{repoName}/projects POST
            ProjectRequest project = new ProjectRequest();
            project.setDescription("test project");
            String groupId = UUID.randomUUID().toString();
            String version = random.nextInt(1000) + ".0";
            project.setName(projectName);
            project.setGroupId(groupId);
            project.setVersion(version);
            
            CreateProjectRequest createProjectRequest = postTimeout("repositories/" + repoName + "/projects", 202, 0.5, project, CreateProjectRequest.class);
            assertNotNull( "Empty response object", createProjectRequest );

            // rest/jobs/{jobId} GET
            waitForJobToComplete(deploymentUrl, createProjectRequest.getJobId(), createProjectRequest.getStatus());
        }

        {
            // rest/repositories/{repoName}/projects POST
            CompileProjectRequest compileRequest = postTimeout("repositories/" + repoName + "/projects/" + projectName + "/maven/compile", 202, 0.5, CompileProjectRequest.class);
            assertNotNull( "Empty response object", compileRequest );

            // rest/jobs/{jobId} GET
            waitForJobToComplete(deploymentUrl, compileRequest.getJobId(), compileRequest.getStatus());
        }
    }
    
    private JobResult waitForJobToComplete(URL deploymentUrl, String jobId, JobStatus jobStatus) throws Exception {
        return waitForJobToHaveStatus(deploymentUrl, jobId, jobStatus, JobStatus.SUCCESS);
    }
    
    private JobResult waitForJobToHaveStatus(URL deploymentUrl, String jobId, JobStatus jobStatus, JobStatus expectedStatus ) throws Exception {
        assertTrue( "Initial status of request should be ACCEPTED or APROVED: " + jobStatus, 
                jobStatus.equals(JobStatus.ACCEPTED) || jobStatus.equals(JobStatus.APPROVED) );
        int wait = 0;
        JobResult jobResult = null;
        while( ! jobStatus.equals(JobStatus.SUCCESS) && wait < maxTries ) {
            jobResult = get("jobs/" + jobId, 200, JobResult.class);
            assertEquals( jobResult.getJobId(), jobId );
            jobStatus = jobResult.getStatus();
            if( jobStatus.equals(expectedStatus) ) { 
                break;
            } else if( jobStatus.equals(JobStatus.FAIL) ) { 
                fail( "Request failed." );
            }
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
        setDeploymentUrl(deploymentUrl);
        
        // rest/organizaionalunits GET
        Collection<OrganizationalUnit> orgUnits = get("organizationalunits", 200, Collection.class, OrganizationalUnit.class);
        int origUnitsSize = orgUnits.size();
        
        // rest/organizaionalunits POST
        OrganizationalUnit orgUnit = new OrganizationalUnit();
        {
            orgUnit.setDescription("Test OU");
            orgUnit.setName(UUID.randomUUID().toString());
            orgUnit.setOwner(this.getClass().getSimpleName());
            String [] repoArr = { "uf-playground" };
            orgUnit.setRepositories(Arrays.asList(repoArr));

            CreateOrganizationalUnitRequest createOURequest = postTimeout("organizationalunits", 202, 0.5, orgUnit, CreateOrganizationalUnitRequest.class);

            // rest/jobs/{jobId}
            waitForJobToComplete(deploymentUrl, createOURequest.getJobId(), createOURequest.getStatus());
        }
        
        // rest/organizaionalunits GET
        orgUnits = get("organizationalunits", 200, Collection.class, OrganizationalUnit.class);
        assertEquals( "Exepcted an OU to be added.", origUnitsSize + 1, orgUnits.size());
      
        OrganizationalUnit foundOu = null;
        for( OrganizationalUnit ou : orgUnits ) { 
            if( orgUnit.getName().equals(ou.getName()) ) { 
                foundOu = ou;
                break;
            }
        }
        assertNotNull("Could not find creatd OU", foundOu);
        assertEquals("OU owner", orgUnit.getOwner(), foundOu.getOwner() );
        assertArrayEquals("OU owner", orgUnit.getRepositories().toArray(), foundOu.getRepositories().toArray());
        
        // rest/organizaionalunits POST (BZ-1175477: duplicate POST (same OU name) should fail
        orgUnit = new OrganizationalUnit();
        {
            orgUnit.setDescription("Duplicate Test OU");
            orgUnit.setName(foundOu.getName());
            orgUnit.setOwner(this.getClass().getSimpleName());
            String [] repoArr = { "uf-playground" };
            orgUnit.setRepositories(Arrays.asList(repoArr));

            CreateOrganizationalUnitRequest createOURequest = postTimeout("organizationalunits", 202, 0.5, orgUnit, CreateOrganizationalUnitRequest.class);

            // rest/jobs/{jobId}
            waitForJobToHaveStatus(deploymentUrl, createOURequest.getJobId(), createOURequest.getStatus(), JobStatus.DENIED);
        } 
        
        // rest/repositories POST
        RepositoryRequest newRepo = new RepositoryRequest();
        {
            String repoName = UUID.randomUUID().toString();
            newRepo.setName(repoName);
            newRepo.setDescription("repo for testing rest services");
            newRepo.setRequestType("new");

            CreateOrCloneRepositoryRequest createRepoRequest = postTimeout("repositories", 202, 0.5, newRepo, CreateOrCloneRepositoryRequest.class);
            assertNotNull( "create repo job request", createRepoRequest);
            JobStatus requestStatus = createRepoRequest.getStatus();
            assertTrue( "job request status: " + requestStatus, JobStatus.ACCEPTED.equals(requestStatus) || JobStatus.APPROVED.equals(requestStatus) );

            // rest/jobs/{jobId}
            waitForJobToComplete(deploymentUrl, createRepoRequest.getJobId(), createRepoRequest.getStatus());
        }
      
        {
            // rest/organizationalunits/{ou}/repositories/{repoName} POST
            AddRepositoryToOrganizationalUnitRequest addRepoToOuRequest = postTimeout(
                    "organizationalunits/" + orgUnit.getName() + "/repositories/" + newRepo.getName(),
                    202, 0.5,
                    AddRepositoryToOrganizationalUnitRequest.class);
            assertNotNull( "add repo to ou job request", addRepoToOuRequest);
            JobStatus requestStatus = addRepoToOuRequest.getStatus();
            assertTrue( "job request status: " + requestStatus, JobStatus.ACCEPTED.equals(requestStatus) || JobStatus.APPROVED.equals(requestStatus) );

            // rest/jobs/{jobId}
            waitForJobToComplete(deploymentUrl, addRepoToOuRequest.getJobId(), addRepoToOuRequest.getStatus());
        }
       
        // rest/organizationalunits/{ou} GET
        OrganizationalUnit orgUnitRequest = get( "organizationalunits/" + orgUnit.getName(), 200, OrganizationalUnit.class);
        assertNotNull( "organizational unit request", orgUnitRequest);
        
        assertTrue( "repository has not been added to organizational unit", orgUnitRequest.getRepositories().contains(newRepo.getName()));
        
        {
            // rest/organizationalunits/{ou}/repositories/{repoName} DELETE
            RemoveRepositoryFromOrganizationalUnitRequest remRepoFromOuRquest = delete(
                    "organizationalunits/" + orgUnit.getName() + "/repositories/" + newRepo.getName(),
                    202,
                    RemoveRepositoryFromOrganizationalUnitRequest.class);
            assertNotNull( "add repo to ou job request", remRepoFromOuRquest);
            JobStatus requestStatus = remRepoFromOuRquest.getStatus();
            assertTrue( "job request status: " + requestStatus, JobStatus.ACCEPTED.equals(requestStatus) || JobStatus.APPROVED.equals(requestStatus) );

            // rest/jobs/{jobId}
            waitForJobToComplete(deploymentUrl, remRepoFromOuRquest.getJobId(), remRepoFromOuRquest.getStatus());
        }
        
        // rest/organizationalunits/{ou} GET
        orgUnitRequest = get( "organizationalunits/" + orgUnit.getName(), 200, OrganizationalUnit.class);
        assertNotNull( "organizational unit request", orgUnitRequest);
        
        assertFalse( "repository should have been deleted from organizational unit", orgUnitRequest.getRepositories().contains(newRepo.getName()));
    }

}
