package template;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import logist.agent.Agent;
import logist.task.Task;
import logist.topology.Topology.City;

/**
 * Our algo (in order to get a plan) will travel through all those possibles
 * plans
 * 
 * @author Jean-Thomas
 *
 */

public class DeliberativeState {

    public static final int NOT_PICKED_UP = 0;
    public static final int PICKED_UP = 1;
    public static final int DELIVERED = 2;

    private final List<Task> tasks;
    private int[] tasksStatus;
    private int taskIndex;

    private City departure;
    private double cost;
    private double charge;

    private DeliberativeState previous;

    public DeliberativeState(int[] tasksStatus, List<Task> tasks,
	    int taskIndex, City departure, double charge, double cost,
	    DeliberativeState previous) {

	this.tasksStatus = tasksStatus;
	this.tasks = tasks;
	this.taskIndex = taskIndex;

	this.departure = departure;
	this.cost = cost;
	this.charge = charge;

	this.previous = previous;
    }

    public Set<DeliberativeState> getSuccessors(Agent agent) {
	Set<DeliberativeState> nextPossibleStates = new HashSet<DeliberativeState>();

	double costPerKm = agent.vehicles().get(0).costPerKm();
	double currentCost = cost;

	int numberOfTasks = tasksStatus.length;

	for (int i = 0; i < numberOfTasks; ++i) {
	    Task task = tasks.get(i);

	    if (tasksStatus[i] == NOT_PICKED_UP) {
		double updatedCharge = charge + task.weight;

		// in the next state, try picking up task, if possible
		if (updatedCharge < agent.vehicles().get(0).capacity()) {
		    // indicate the task will be picked up
		    int[] newTasksStatus = Arrays.copyOf(tasksStatus,
			    tasksStatus.length);
		    newTasksStatus[i] = PICKED_UP;

		    double updatedCost = currentCost
			    + departure.distanceTo(task.pickupCity) * costPerKm;

		    City destination = task.pickupCity;

		    DeliberativeState s = new DeliberativeState(newTasksStatus,
			    tasks, i, destination, updatedCharge, updatedCost,
			    this);
		    nextPossibleStates.add(s);
		}
	    } else if (tasksStatus[i] == PICKED_UP) {
		// indicate the task will be delivered
		int[] newTasksStatus = Arrays.copyOf(tasksStatus,
			tasksStatus.length);
		newTasksStatus[i] = DELIVERED;

		double updatedCharge = charge - task.weight;
		double updatedCost = currentCost
			+ departure.distanceTo(task.deliveryCity) * costPerKm;

		City destination = task.deliveryCity;

		DeliberativeState s = new DeliberativeState(newTasksStatus,
			tasks, i, destination, updatedCharge, updatedCost, this);
		nextPossibleStates.add(s);
	    }

	}
	return nextPossibleStates;
    }

    public double cost() {
	return cost;
    }

    public boolean isFinalState() {
	int numberOfTasks = tasksStatus.length;
	for (int i = 0; i < numberOfTasks; ++i) {
	    if (tasksStatus[i] != 2) {
		return false;
	    }
	}
	return true;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	long temp;
	temp = Double.doubleToLongBits(charge);
	result = prime * result + (int) (temp ^ (temp >>> 32));
	temp = Double.doubleToLongBits(cost);
	result = prime * result + (int) (temp ^ (temp >>> 32));
	result = prime * result
		+ ((departure == null) ? 0 : departure.hashCode());
	result = prime * result + Arrays.hashCode(tasksStatus);
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
	DeliberativeState other = (DeliberativeState) obj;
	if (Double.doubleToLongBits(charge) != Double
		.doubleToLongBits(other.charge))
	    return false;
	if (Double.doubleToLongBits(cost) != Double
		.doubleToLongBits(other.cost))
	    return false;
	if (departure == null) {
	    if (other.departure != null)
		return false;
	} else if (!departure.equals(other.departure))
	    return false;
	if (!Arrays.equals(tasksStatus, other.tasksStatus))
	    return false;
	return true;
    }

    public DeliberativeState previous() {
	return previous;
    }

    public City departure() {
	return departure;
    }

    public int taskStatus(int i) {
	return tasksStatus[i];
    }

    public int taskIndex() {
	return taskIndex;
    }

    public List<Task> tasks() {
	return tasks;
    }
}
