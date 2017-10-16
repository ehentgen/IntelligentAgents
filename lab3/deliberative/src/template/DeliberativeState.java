package template;

import logist.simulation.Vehicle;

import java.awt.geom.RectangularShape;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * Our algo (in order to get a plan) will travel through all those possibles plans
 * @author Jean-Thomas
 *
 */
public class DeliberativeState {
	
	private TaskSet remainingTasks;
	private TaskSet executedTasks;
	private List<Action> actionsPerformed;
	
	private City currentCity;
	
	private int currentCharge = 0;
	
	public DeliberativeState(TaskSet remaining, TaskSet executed, City current, List<Action> wayToHere) {
		this.remainingTasks = remaining;
		this.executedTasks = executed;
		this.currentCity = current;
		this.actionsPerformed = wayToHere; //This will be super-useful in order to find how to reach the best state
	}
	
	
	public List<DeliberativeState> getSuccessors() {
		List<DeliberativeState> nextPossibleStates = null;
		
		for (Task task : remainingTasks) {
			
			//remove the task we will execute
			TaskSet newRemaining = TaskSet.copyOf(remainingTasks);
			newRemaining.remove(task);
			
			//and add it to the executed list
			TaskSet newExecuted = TaskSet.copyOf(executedTasks);
			newRemaining.add(task);
			
			City newCity = task.deliveryCity;

			List<City> cities = task.path();			
			for (City city : cities) {
				Move a = new Move(city);
				actionsPerformed.add(a);
			}
			
			DeliberativeState s = new DeliberativeState(newRemaining, newExecuted, newCity, actionsPerformed);
			nextPossibleStates.add(s); //avertissement ?
		}
		return nextPossibleStates;
	}
	
	
	/**
	 * This method could be useful to compare final states and choose the best one
	 * @return
	 */
	public double getGain() {
		return executedTasks.rewardSum();
	}
	
	// => There is only one possible final state.
	public boolean isFinalState() {
		return (remainingTasks.isEmpty());
	}

}
