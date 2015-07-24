package org.kie.perf.remote;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.kie.api.runtime.Globals;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.audit.AuditService;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Attachment;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.query.QueryFilter;

public interface RemoteController {

    public void setCredentials(String userId, String password);

    public RemoteController newPerProcessInstanceController(Long processInstanceId);

    // -------------------------------------------------- TaskService delegates

    public void activate(long taskId, String userId);

    public void claim(long taskId, String userId);

    public void claimNextAvailable(String userId, String language);

    public void complete(long taskId, String userId, Map<String, Object> data);

    public void delegate(long taskId, String userId, String targetUserId);

    public void exit(long taskId, String userId);

    public void fail(long taskId, String userId, Map<String, Object> faultData);

    public void forward(long taskId, String userId, String targetEntityId);

    public Task getTaskByWorkItemId(long workItemId);

    public Task getTaskById(long taskId);

    public List<TaskSummary> getTasksAssignedAsBusinessAdministrator(String userId);

    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId);

    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatus(String userId, List<Status> status);

    public List<TaskSummary> getTasksOwned(String userId);

    public List<TaskSummary> getTasksOwnedByStatus(String userId, List<Status> status);

    public List<TaskSummary> getTasksByStatusByProcessInstanceId(long processInstanceId, List<Status> status);

    public List<TaskSummary> getTasksAssignedAsBusinessAdministrator(String userId, QueryFilter queryFilter);

    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId, QueryFilter queryFilter);

    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatus(String userId, List<Status> status, QueryFilter queryFilter);

    public List<TaskSummary> getTasksOwned(String userId, QueryFilter queryFilter);

    public List<TaskSummary> getTasksOwnedByStatus(String userId, List<Status> status, QueryFilter queryFilter);

    public List<TaskSummary> getTasksByStatusByProcessInstanceId(long processInstanceId, List<Status> status, QueryFilter queryFilter);

    public List<Long> getTasksByProcessInstanceId(long processInstanceId);

    public long addTask(Task task, Map<String, Object> params);

    public void release(long taskId, String userId);

    public void resume(long taskId, String userId);

    public void skip(long taskId, String userId);

    public void start(long taskId, String userId);

    public void stop(long taskId, String userId);

    public void suspend(long taskId, String userId);

    public void nominate(long taskId, String userId, List<OrganizationalEntity> potentialOwners);

    public Content getContentById(long contentId);

    public Attachment getAttachmentById(long attachId);

    // ------------------------- KieRuntime
    public void setGlobal(String identifier, Object value);

    public Object getGlobal(String identifier);

    public Globals getGlobals();

    // ------------------------- ProcessRuntime
    public ProcessInstance startProcess(String processId);

    public ProcessInstance startProcess(String processId, Map<String, Object> parameters);

    public ProcessInstance createProcessInstance(String processId, Map<String, Object> parameters);

    public ProcessInstance startProcessInstance(long processInstanceId);

    public void signalEvent(String type, Object event);

    public void signalEvent(String type, Object event, long processInstanceId);

    public Collection<ProcessInstance> getProcessInstances();

    public ProcessInstance getProcessInstance(long processInstanceId);

    public ProcessInstance getProcessInstance(long processInstanceId, boolean readonly);

    public void abortProcessInstance(long processInstanceId);

    public WorkItemManager getWorkItemManager();

    public KieSession getKieSession();

    public TaskService getTaskService();

    public AuditService getAuditService();

}
