package templateJT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import logist.agent.Agent;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

/**
 * Our algo (in order to get a plan) will travel through all those possibles plans
 * @author E&JT
 *
 */
public class DeliberativeState {

	public static final int NOT_PICKED_UP = 0;
	public static final int PICKED_UP = 1;
	public static final int DELIVERED = 2;
	
	//private TaskSet remainingTasks;
	private DeliberativeState previous;
	private int[] tasksStatus;
	private List<Task> tasks;
	private int taskIndex;
	
	private City departure;
	
	private double charge = 0;
	double cost = 0;

	public DeliberativeState(int[] tasksStatus, List<Task> tasks, int taskIndex, City departure, double charge, double cost,
			DeliberativeState previous) {
		
		this.tasksStatus = tasksStatus;
		this.tasks = tasks;
		this.taskIndex = taskIndex;

		this.departure = departure;
		this.charge = charge;
		this.cost = cost;

		this.previous = previous;
	}
	
	public Set<DeliberativeState> getSuccessors(Agent agent) {
		Set<DeliberativeState> nextPossibleStates = new HashSet<DeliberativeState>();

		for (Task task : tasks) {
			int taskID = task.id;
			
			if (tasksStatus[taskID] == NOT_PICKED_UP) {
				if (charge + task.weight < agent.vehicles().get(0).capacity()) {
					int[] newTasksStatus = Arrays.copyOf(tasksStatus, tasksStatus.length);
					newTasksStatus[taskID] = PICKED_UP;
					
					City destination = task.pickupCity;
					double updatedCost = cost
							+ departure.distanceTo(task.pickupCity) * agent.vehicles().get(0).costPerKm();

					DeliberativeState s = new DeliberativeState(
							newTasksStatus, tasks, taskID, destination, charge + task.weight, updatedCost, this);
					nextPossibleStates.add(s);
				}
			}
			else if (tasksStatus[taskID] == PICKED_UP) {
				int[] newTasksStatus = Arrays.copyOf(tasksStatus, tasksStatus.length);
				newTasksStatus[taskID] = DELIVERED;
				
				City destination = task.deliveryCity;
				double updatedCost = cost
						+ departure.distanceTo(task.deliveryCity) * agent.vehicles().get(0).costPerKm();
				
				DeliberativeState s = new DeliberativeState(
						newTasksStatus, tasks, taskID, destination, charge + task.weight, updatedCost, this);
				nextPossibleStates.add(s);
			}
		}
		return nextPossibleStates;
	}
	
	
	/**
	 * This method could be useful to compare final states and choose the best one
	 * @return
	 */
	public double getGain() {
		return -1; //loadedTasks.rewardSum();
	}
	
	public double getCost() {
		return cost;
	}
	
	public boolean isFinalState() {
		for (int i = 0; i < tasksStatus.length; ++i) {
			if (tasksStatus[i] != 2) {
				return false;
			}
		}
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(cost);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		if (Double.doubleToLongBits(cost) != Double.doubleToLongBits(other.cost))
			return false;
		if (!Arrays.equals(tasksStatus, other.tasksStatus))
			return false;
		return true;
	}


	

}
