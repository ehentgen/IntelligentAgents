package template;

import logist.simulation.Vehicle;

import java.awt.geom.RectangularShape;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
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
	
	private City currentCity;
	
	private int currentCharge = 0;
	
	public DeliberativeState(TaskSet remaining, TaskSet executed, City current) {
		this.remainingTasks = remaining;
		this.executedTasks = executed;
		this.currentCity = current;
	}
	
	
	public List<DeliberativeState> buildSuccessors() {
		return null;
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
