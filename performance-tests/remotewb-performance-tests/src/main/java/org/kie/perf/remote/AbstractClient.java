package org.kie.perf.remote;

import org.kie.api.runtime.Globals;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractClient implements RemoteController {

    protected RuntimeEngine runtimeEngine;

    protected TaskService taskService;

    protected KieSession kieSession;

    protected AuditService auditService;

    protected static KieWBTestConfig config = KieWBTestConfig.getInstance();

    // -------------------------------------------------- TaskService

    @Override
    public void activate(long taskId, String userId) {
        taskService.activate(taskId, userId);
    }

    @Override
    public void claim(long taskId, String userId) {
        taskService.claim(taskId, userId);
    }

    @Override
    public void claimNextAvailable(String userId, String language) {
        taskService.claimNextAvailable(userId, language);
    }

    @Override
    public void complete(long taskId, String userId, Map<String, Object> data) {
        taskService.complete(taskId, userId, data);
    }

    @Override
    public void delegate(long taskId, String userId, String targetUserId) {
        taskService.delegate(taskId, userId, targetUserId);
    }

    @Override
    public void exit(long taskId, String userId) {
        taskService.exit(taskId, userId);
    }

    @Override
    public void fail(long taskId, String userId, Map<String, Object> faultData) {
        taskService.fail(taskId, userId, faultData);
    }

    @Override
    public void forward(long taskId, String userId, String targetEntityId) {
        taskService.forward(taskId, userId, targetEntityId);
    }

    @Override
    public Task getTaskByWorkItemId(long workItemId) {
        return taskService.getTaskByWorkItemId(workItemId);
    }

    @Override
    public Task getTaskById(long taskId) {
        return taskService.getTaskById(taskId);
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsBusinessAdministrator(String userId) {
        return getTasksAssignedAsBusinessAdministrator(userId, null);
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId) {
        return getTasksAssignedAsPotentialOwner(userId, null);
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatus(String userId, List<Status> status) {
        return getTasksAssignedAsPotentialOwnerByStatus(userId, status, null);
    }

    @Override
    public List<TaskSummary> getTasksOwned(String userId) {
        return getTasksOwned(userId, null);
    }

    @Override
    public List<TaskSummary> getTasksOwnedByStatus(String userId, List<Status> status) {
        return getTasksOwnedByStatus(userId, status, null);
    }

    @Override
    public List<TaskSummary> getTasksByStatusByProcessInstanceId(long processInstanceId, List<Status> status) {
        return getTasksByStatusByProcessInstanceId(processInstanceId, status, null);
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsBusinessAdministrator(String userId, QueryFilter queryFilter) {
        String lang = (queryFilter == null) ? "" : queryFilter.getLanguage();
        return taskService.getTasksAssignedAsBusinessAdministrator(userId, lang);
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwner(String userId, QueryFilter queryFilter) {
        String lang = (queryFilter == null) ? "" : queryFilter.getLanguage();
        return taskService.getTasksAssignedAsPotentialOwner(userId, lang);
    }

    @Override
    public List<TaskSummary> getTasksAssignedAsPotentialOwnerByStatus(String userId, List<Status> status, QueryFilter queryFilter) {
        String lang = (queryFilter == null) ? "" : queryFilter.getLanguage();
        return taskService.getTasksAssignedAsPotentialOwnerByStatus(userId, status, lang);
    }

    @Override
    public List<TaskSummary> getTasksOwned(String userId, QueryFilter queryFilter) {
        String lang = (queryFilter == null) ? "" : queryFilter.getLanguage();
        return taskService.getTasksOwned(userId, lang);
    }

    @Override
    public List<TaskSummary> getTasksOwnedByStatus(String userId, List<Status> status, QueryFilter queryFilter) {
        String lang = (queryFilter == null) ? "" : queryFilter.getLanguage();
        return taskService.getTasksOwnedByStatus(userId, status, lang);
    }

    @Override
    public List<TaskSummary> getTasksByStatusByProcessInstanceId(long processInstanceId, List<Status> status, QueryFilter queryFilter) {
        String lang = (queryFilter == null) ? "" : queryFilter.getLanguage();
        return taskService.getTasksByStatusByProcessInstanceId(processInstanceId, status, lang);
    }

    @Override
    public List<Long> getTasksByProcessInstanceId(long processInstanceId) {
        return taskService.getTasksByProcessInstanceId(processInstanceId);
    }

    @Override
    public long addTask(Task task, Map<String, Object> params) {
        return taskService.addTask(task, params);
    }

    @Override
    public void release(long taskId, String userId) {
        taskService.release(taskId, userId);
    }

    @Override
    public void resume(long taskId, String userId) {
        taskService.resume(taskId, userId);
    }

    @Override
    public void skip(long taskId, String userId) {
        taskService.skip(taskId, userId);
    }

    @Override
    public void start(long taskId, String userId) {
        taskService.start(taskId, userId);
    }

    @Override
    public void stop(long taskId, String userId) {
        taskService.stop(taskId, userId);
    }

    @Override
    public void suspend(long taskId, String userId) {
        taskService.suspend(taskId, userId);
    }

    @Override
    public void nominate(long taskId, String userId, List<OrganizationalEntity> potentialOwners) {
        taskService.nominate(taskId, userId, potentialOwners);
    }

    @Override
    public Content getContentById(long contentId) {
        return taskService.getContentById(contentId);
    }

    @Override
    public Attachment getAttachmentById(long attachId) {
        return taskService.getAttachmentById(attachId);
    }

    // ------------------------- KieRuntime

    @Override
    public void setGlobal(String identifier, Object value) {
        kieSession.setGlobal(identifier, value);
    }

    @Override
    public Object getGlobal(String identifier) {
        return kieSession.getGlobal(identifier);
    }

    @Override
    public Globals getGlobals() {
        return kieSession.getGlobals();
    }

    // ------------------------- ProcessRuntime

    @Override
    public ProcessInstance startProcess(String processId) {
        return kieSession.startProcess(processId);
    }

    @Override
    public ProcessInstance startProcess(String processId, Map<String, Object> parameters) {
        return kieSession.startProcess(processId, parameters);
    }

    @Override
    public ProcessInstance createProcessInstance(String processId, Map<String, Object> parameters) {
        return kieSession.createProcessInstance(processId, parameters);
    }

    @Override
    public ProcessInstance startProcessInstance(long processInstanceId) {
        return kieSession.startProcessInstance(processInstanceId);
    }

    @Override
    public void signalEvent(String type, Object event) {
        kieSession.signalEvent(type, event);
    }

    @Override
    public void signalEvent(String type, Object event, long processInstanceId) {
        kieSession.signalEvent(type, event, processInstanceId);
    }

    @Override
    public Collection<ProcessInstance> getProcessInstances() {
        return kieSession.getProcessInstances();
    }

    @Override
    public ProcessInstance getProcessInstance(long processInstanceId) {
        return kieSession.getProcessInstance(processInstanceId);
    }

    @Override
    public ProcessInstance getProcessInstance(long processInstanceId, boolean readonly) {
        return kieSession.getProcessInstance(processInstanceId, readonly);
    }

    @Override
    public void abortProcessInstance(long processInstanceId) {
        kieSession.abortProcessInstance(processInstanceId);
    }

    @Override
    public WorkItemManager getWorkItemManager() {
        return kieSession.getWorkItemManager();
    }

    @Override
    public AuditService getAuditService() {
        return auditService;
    }

    @Override
    public KieSession getKieSession() {
        return this.kieSession;
    }

    @Override
    public TaskService getTaskService() {
        return taskService;
    }

}
