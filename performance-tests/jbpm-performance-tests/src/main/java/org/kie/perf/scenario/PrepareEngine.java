package org.kie.perf.scenario;

import java.io.StringReader;

import org.jbpm.services.task.impl.factories.TaskFactory;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Task;
import org.kie.perf.jbpm.constant.UserStorage;

public class PrepareEngine {

    public static void createNewTasks(boolean start, int count, TaskService taskService) {
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('" + UserStorage.PerfUser.getUserId()
                + "')], businessAdministrators = [ new User('Administrator') ], }),";
        str += "names = [ new I18NText( 'en-UK', 'perf-sample-task')] })";
        int tasksToBePrepared = count;
        int totalTasks = tasksToBePrepared;
        while (tasksToBePrepared > 0) {
            Task task = TaskFactory.evalTask(new StringReader(str));
            taskService.addTask(task, null);
            if (start) {
                taskService.start(totalTasks - tasksToBePrepared + 1, UserStorage.PerfUser.getUserId());
            }
            tasksToBePrepared--;
        }
    }

}
