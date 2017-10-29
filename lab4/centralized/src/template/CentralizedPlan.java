package template;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

public class CentralizedPlan implements Cloneable {

    private Map<Vehicle, TaskAction> vehicleToFirstTaskAction;
    private Map<TaskAction, TaskAction> taskActionToTaskAction;

    // private Map<Task, Integer> time;
    // private Map<Task, Vehicle> taskToVehicle;

    public CentralizedPlan(Map<Vehicle, TaskAction> vehicleToFirstTaskAction,
	    Map<TaskAction, TaskAction> taskActionToTaskAction) {
	this.vehicleToFirstTaskAction = vehicleToFirstTaskAction;
	this.taskActionToTaskAction = taskActionToTaskAction;
	// this.time = time;
	// this.taskToVehicle = taskToVehicle;
    }

    public CentralizedPlan(CentralizedPlan plan) {
	this(plan.vehicleToFirstTaskAction, plan.taskActionToTaskAction);
    }

    public Map<Vehicle, TaskAction> vehicleToFirstTaskAction() {
	return vehicleToFirstTaskAction;
    }

    public Map<TaskAction, TaskAction> taskActionToTaskAction() {
	return taskActionToTaskAction;
    }

    // public Map<Task, Integer> time() {
    // return time;
    // }

    // public Map<Task, Vehicle> vehicle() {
    // return taskToVehicle;
    // }

    public double cost() {
	double cost = 0;
	Set<Vehicle> vehicles = vehicleToFirstTaskAction.keySet();

	for (Vehicle vehicle : vehicles) {
	    City currentCity = vehicle.getCurrentCity();
	    double costPerKm = vehicle.costPerKm();

	    if (vehicleToFirstTaskAction.get(vehicle) != null) {
		cost += currentCity.distanceTo(vehicleToFirstTaskAction.get(
			vehicle).task().pickupCity)
			* costPerKm;

		TaskAction taskAction = taskActionToTaskAction
			.get(vehicleToFirstTaskAction.get(vehicle));

		while (taskAction != null) {
		    Task task = taskAction.task();
		    if (taskAction.status() == TaskAction.PICK_UP) {
			cost += currentCity.distanceTo(task.pickupCity)
				* costPerKm;
			currentCity = task.pickupCity;
		    } else if (taskAction.status() == TaskAction.DELIVERY) {
			cost += currentCity.distanceTo(task.deliveryCity)
				* costPerKm;
			currentCity = task.deliveryCity;
		    }

		    taskAction = taskActionToTaskAction.get(taskAction);

		    /*
		     * TaskAction nextTaskAction = taskActionToTaskAction
		     * .get(taskAction);
		     * 
		     * if (nextTaskAction != null) { cost +=
		     * task.deliveryCity.distanceTo(nextTaskAction
		     * .task().pickupCity) * costPerKm; }
		     * 
		     * taskAction = nextTaskAction;
		     */
		}
	    }
	}

	return cost;
    }

    public void setNextTask(Vehicle vehicle, TaskAction taskAction) {
	vehicleToFirstTaskAction.put(vehicle, taskAction);
    }

    public void setNextTask(TaskAction taskAction_1, TaskAction taskAction_2) {
	if (taskAction_1.equals(taskAction_2)) {
	    System.err.println("Error on setNextTask");
	}
	taskActionToTaskAction.put(taskAction_1, taskAction_2);

    }

    /*
     * public void updateTime(Vehicle vehicle) { Task firstTask =
     * vehicleToFirstTask.get(vehicle); if (firstTask != null) {
     * time.put(firstTask, 1); }
     * 
     * Task nextTask = taskToTask.get(firstTask); int i = 2; while (nextTask !=
     * null) { time.put(taskToTask.get(nextTask), i++); nextTask =
     * taskToTask.get(nextTask); } }
     */

    // public void setVehicle(Task task, Vehicle vehicle) {
    // taskToVehicle.put(task, vehicle);
    // }

    public CentralizedPlan clone() {
	CentralizedPlan plan;
	try {
	    plan = (CentralizedPlan) super.clone();

	    Map<Vehicle, TaskAction> copy = new HashMap<Vehicle, TaskAction>();
	    for (Map.Entry<Vehicle, TaskAction> entry : this.vehicleToFirstTaskAction
		    .entrySet()) {
		copy.put(entry.getKey(), entry.getValue());
	    }
	    plan.vehicleToFirstTaskAction = copy;
	    Map<TaskAction, TaskAction> copy2 = new HashMap<TaskAction, TaskAction>();
	    for (Map.Entry<TaskAction, TaskAction> entry : this.taskActionToTaskAction
		    .entrySet()) {
		copy2.put(entry.getKey(), entry.getValue());
	    }
	    plan.taskActionToTaskAction = copy2;
	} catch (CloneNotSupportedException e) {
	    return null; // will never happen
	}
	return plan;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime
		* result
		+ ((taskActionToTaskAction == null) ? 0
			: taskActionToTaskAction.hashCode());
	result = prime
		* result
		+ ((vehicleToFirstTaskAction == null) ? 0
			: vehicleToFirstTaskAction.hashCode());
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
	CentralizedPlan other = (CentralizedPlan) obj;
	if (taskActionToTaskAction == null) {
	    if (other.taskActionToTaskAction != null)
		return false;
	} else if (!taskActionToTaskAction.equals(other.taskActionToTaskAction))
	    return false;
	if (vehicleToFirstTaskAction == null) {
	    if (other.vehicleToFirstTaskAction != null)
		return false;
	} else if (!vehicleToFirstTaskAction
		.equals(other.vehicleToFirstTaskAction))
	    return false;
	return true;
    }
}
