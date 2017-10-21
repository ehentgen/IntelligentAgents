package templateJT;

import logist.simulation.Vehicle;

import java.util.ArrayList;
import java.util.List;

import logist.agent.Agent;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.plan.Action.Delivery;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

/**
 * Our algo (in order to get a plan) will travel through all those possibles plans
 * @author Jean-Thomas & Emily
 *
 */
public class DeliberativeState {
	
	private TaskSet remainingTasks;
	private TaskSet loadedTasks;
	private List<Action> actionsPerformed;
	
	private City currentCity;
	
	private double currentCharge = 0;
	double cost = 0;
	
	//public DeliberativeState(TaskSet remaining, TaskSet executed, City current, List<Action> wayToHere) {
	public DeliberativeState(TaskSet remaining, TaskSet loaded, City current, List<Action> wayToHere, double cost, double charge) {
		this.remainingTasks = remaining;
		this.loadedTasks = loaded;
		this.cost = cost;
		this.currentCharge = charge;
		this.currentCity = current;
		this.actionsPerformed = wayToHere; //This will be super-useful in order to find how to reach the best state
		
		if (wayToHere == null) {
			this.actionsPerformed = new ArrayList<Action>();
		} else {
			this.actionsPerformed = wayToHere;
		}
	}
	
	public List<DeliberativeState> getSuccessors(Agent agent) {
		List<DeliberativeState> nextPossibleStates = new ArrayList<DeliberativeState>();
		double costPerKm = agent.vehicles().get(0).costPerKm();
		//TaskSet useless = TaskSet.copyOf(remainingTasks);
		
		for (Task task : remainingTasks) {
			List<Action> newActionsPerformed = new ArrayList<Action>(actionsPerformed);

			double newCharge = currentCharge + task.weight;
			
			if (newCharge < agent.vehicles().get(0).capacity()) {
				//remove the task we will pickup
				TaskSet newRemaining = TaskSet.copyOf(remainingTasks);
				newRemaining.remove(task);
				
				TaskSet newLoaded = TaskSet.copyOf(loadedTasks);
				newLoaded.add(task);
				
				double updatedCost = cost + currentCity.distanceTo(task.pickupCity)  * costPerKm;
				
				for (City city : currentCity.pathTo(task.pickupCity))
					newActionsPerformed.add(new Move(city));
				
				newActionsPerformed.add(new Pickup(task));
				
				DeliberativeState s = new DeliberativeState(newRemaining, newLoaded, task.pickupCity, newActionsPerformed, updatedCost, newCharge);
				nextPossibleStates.add(s);
			}
			
		}
		
		for (Task task : loadedTasks) {
			List<Action> newActionsPerformed = new ArrayList<Action>(actionsPerformed);

			//remove the task we will execute
			TaskSet newRemaining = TaskSet.copyOf(remainingTasks);
			TaskSet newLoaded = TaskSet.copyOf(loadedTasks);
			newLoaded.remove(task);
			
			double newCharge = currentCharge - task.weight;
			double updatedCost = cost + 
					(currentCity.distanceTo(task.deliveryCity)) * costPerKm;
			
			updatedCost = updatedCost - task.reward;
			
			/*for (City city : currentCity.pathTo(task.pickupCity))
				newActionsPerformed.add(new Move(city));
			
			newActionsPerformed.add(new Pickup(task));*/
			
			for (City city : currentCity.pathTo(task.deliveryCity)) {
				Move a = new Move(city);
				newActionsPerformed.add(a);
			}
			newActionsPerformed.add(new Delivery(task));
			
			
			DeliberativeState s = new DeliberativeState(newRemaining, newLoaded, task.deliveryCity, newActionsPerformed, updatedCost, newCharge);
			nextPossibleStates.add(s);
		}
		return nextPossibleStates;
	}
	
	
	/**
	 * This method could be useful to compare final states and choose the best one
	 * @return
	 */
	public double getGain() {
		return loadedTasks.rewardSum();
	}
	
	public double getCost() {
		return cost;
	}
	
	// => There is only one possible final state.
	public boolean isFinalState() {
		return (remainingTasks.isEmpty() && loadedTasks.isEmpty());
	}


	public List<Action> actionsPerformed() {
		return actionsPerformed;
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(cost);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((currentCity == null) ? 0 : currentCity.hashCode());
		result = prime * result + ((loadedTasks == null) ? 0 : loadedTasks.hashCode());
		result = prime * result + ((remainingTasks == null) ? 0 : remainingTasks.hashCode());
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
		if (currentCity == null) {
			if (other.currentCity != null)
				return false;
		} else if (!currentCity.equals(other.currentCity))
			return false;
		if (loadedTasks == null) {
			if (other.loadedTasks != null)
				return false;
		} else if (!loadedTasks.equals(other.loadedTasks))
			return false;
		if (remainingTasks == null) {
			if (other.remainingTasks != null)
				return false;
		} else if (!remainingTasks.equals(other.remainingTasks))
			return false;
		return true;
	}



}
