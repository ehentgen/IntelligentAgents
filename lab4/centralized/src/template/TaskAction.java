package template;

import logist.task.Task;

public class TaskAction {

    public static final int PICK_UP = 0;
    public static final int DELIVERY = 1;

    private final Task task;
    private final int status;

    public TaskAction(Task task, int status) {
	this.task = task;
	this.status = status;
    }

    public TaskAction(TaskAction taskAction) {
	this(taskAction.task, taskAction.status);
    }

    public Task task() {
	return task;
    }

    public int status() {
	return status;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + status;
	result = prime * result + ((task == null) ? 0 : task.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	TaskAction other = (TaskAction) obj;
	if (status != other.status)
	    return false;
	if (task == null) {
	    if (other.task != null)
		return false;
	} else if (!task.equals(other.task))
	    return false;
	return true;
    }

}
