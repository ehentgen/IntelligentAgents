package template;

import java.util.Map;
import java.util.Set;

import logist.simulation.Vehicle;
import logist.task.Task;

public class CentralizedPlan {

    private Map<Vehicle, Task> vehicleToFirstTask;
    private Map<Task, Task> taskToTask;

    // private Map<Task, Integer> time;
    // private Map<Task, Vehicle> taskToVehicle;

    public CentralizedPlan(Map<Vehicle, Task> vehicleToFirstTask,
	    Map<Task, Task> taskToTask) {
	this.vehicleToFirstTask = vehicleToFirstTask;
	this.taskToTask = taskToTask;
	// this.time = time;
	// this.taskToVehicle = taskToVehicle;
    }

    public CentralizedPlan(CentralizedPlan plan) {
	this(plan.vehicleToFirstTask, plan.taskToTask);
    }

    public Map<Vehicle, Task> vehicleToFirstTask() {
	return vehicleToFirstTask;
    }

    public Map<Task, Task> taskToTask() {
	return taskToTask;
    }

    // public Map<Task, Integer> time() {
    // return time;
    // }

    // public Map<Task, Vehicle> vehicle() {
    // return taskToVehicle;
    // }

    public double cost() {
	double cost = 0;
	Set<Vehicle> vehicles = vehicleToFirstTask.keySet();

	for (Vehicle vehicle : vehicles) {
	    double costPerKm = vehicle.costPerKm();

	    if (vehicleToFirstTask.get(vehicle) != null) {
		cost += vehicle.getCurrentCity().distanceTo(
			vehicleToFirstTask.get(vehicle).pickupCity)
			* costPerKm;

		Task task = taskToTask.get(vehicleToFirstTask.get(vehicle));

		while (task != null) {
		    cost += task.pickupCity.distanceTo(task.deliveryCity)
			    * costPerKm;

		    Task nextTask = taskToTask.get(task);
		    if (nextTask != null) {
			cost += task.deliveryCity
				.distanceTo(nextTask.pickupCity) * costPerKm;
		    }

		    task = nextTask;
		}
	    }
	}

	return cost;
    }

    public void setNextTask(Vehicle vehicle, Task task) {
	vehicleToFirstTask.put(vehicle, task);
    }

    public void setNextTask(Task task1, Task task2) {
	taskToTask.put(task1, task2);
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

}
