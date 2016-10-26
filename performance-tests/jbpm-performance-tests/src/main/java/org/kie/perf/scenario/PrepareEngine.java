package org.kie.perf.scenario;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.services.task.impl.factories.TaskFactory;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Task;
import org.kie.perf.jbpm.JBPMController;
import org.kie.perf.jbpm.constant.ProcessStorage;
import org.kie.perf.jbpm.constant.UserStorage;

public class PrepareEngine {

    public static List<Long> createNewTasks(boolean start, int count, TaskService taskService) {
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('" + UserStorage.PerfUser.getUserId()
                + "')], businessAdministrators = [ new User('Administrator') ], }),";
        str += "names = [ new I18NText( 'en-UK', 'perf-sample-task')] })";
        List<Long> taskIds = new ArrayList<Long>();
        while (count > 0) {
            Task task = TaskFactory.evalTask(new StringReader(str));
            long taskId = taskService.addTask(task, null);
            taskIds.add(taskId);
            if (start) {
                taskService.start(taskId, UserStorage.PerfUser.getUserId());
            }
            count--;
        }
        return taskIds;
    }

}
