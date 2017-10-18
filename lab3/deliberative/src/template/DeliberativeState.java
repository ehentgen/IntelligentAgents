package template;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import logist.agent.Agent;
import logist.plan.Action;
import logist.plan.Action.Delivery;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
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

    private Set<Task> notPickedUpTasks;
    private Set<Task> pickedUpTasks;

    private List<Action> actionsPerformed;

    private City departure;

    private double cost;
    private double charge;

    private double reward;

    public DeliberativeState(Set<Task> notPickedUpTasks,
	    Set<Task> pickedUpTasks, City departure, double charge,
	    double cost, double reward, List<Action> wayToHere) {

	this.notPickedUpTasks = notPickedUpTasks;
	this.pickedUpTasks = pickedUpTasks;

	this.departure = departure;
	this.cost = cost;
	this.charge = charge;

	this.reward = reward;

	// This will be super-useful in order to find how to reach the best
	// state
	if (wayToHere == null) {
	    this.actionsPerformed = new ArrayList<Action>();
	} else {
	    this.actionsPerformed = wayToHere;
	}
    }

    public Set<DeliberativeState> getSuccessors(Agent agent) {
	Set<DeliberativeState> nextPossibleStates = new HashSet<DeliberativeState>();

	double costPerKm = agent.vehicles().get(0).costPerKm();
	double currentCost = cost;

	for (Task task : notPickedUpTasks) {
	    List<Action> newActionsPerformed = new ArrayList<Action>(
		    actionsPerformed);

	    double updatedCharge = charge + task.weight;

	    // in the next state, try picking up task, if possible
	    if (updatedCharge < agent.vehicles().get(0).capacity()) {
		// remove the task we will pick up
		Set<Task> newNotPickedUpTasks = new HashSet<Task>(
			notPickedUpTasks);
		newNotPickedUpTasks.remove(task);

		// and add it to the picked up tasks list
		Set<Task> newPickedUpTasks = new HashSet<Task>(pickedUpTasks);
		newPickedUpTasks.add(task);

		double updatedCost = currentCost
			+ departure.distanceTo(task.pickupCity) * costPerKm;

		City destination = task.pickupCity;

		List<City> path = departure.pathTo(destination);
		for (City city : path) {
		    newActionsPerformed.add(new Move(city));
		}
		newActionsPerformed.add(new Pickup(task));

		DeliberativeState s = new DeliberativeState(
			newNotPickedUpTasks, newPickedUpTasks, destination,
			updatedCharge, updatedCost, reward, newActionsPerformed);
		nextPossibleStates.add(s);
	    }

	}

	for (Task task : pickedUpTasks) {
	    List<Action> newActionsPerformed = new ArrayList<Action>(
		    actionsPerformed);

	    // remove the picked up but not yet delivered task
	    Set<Task> newPickedUpTasks = new HashSet<Task>(pickedUpTasks);
	    newPickedUpTasks.remove(task);

	    double updatedCharge = charge - task.weight;
	    double updatedCost = currentCost
		    + departure.distanceTo(task.deliveryCity) * costPerKm;

	    double updatedReward = reward + task.reward;

	    City destination = task.deliveryCity;

	    List<City> path = departure.pathTo(destination);
	    for (City city : path) {
		newActionsPerformed.add(new Move(city));
	    }
	    newActionsPerformed.add(new Delivery(task));

	    DeliberativeState s = new DeliberativeState(new HashSet<Task>(
		    notPickedUpTasks), newPickedUpTasks, destination,
		    updatedCharge, updatedCost, updatedReward,
		    newActionsPerformed);
	    nextPossibleStates.add(s);

	}

	return nextPossibleStates;
    }

    public double cost() {
	return cost;
    }

    /**
     * This method could be useful to compare final states and choose the best
     * one
     * 
     * @return
     */
    public double getGain() {
	return reward;
    }

    // => There is only one possible final state.
    public boolean isFinalState() {
	return (notPickedUpTasks.isEmpty() && pickedUpTasks.isEmpty());
    }

    public List<Action> actionsPerformed() {
	return actionsPerformed;
    }

}
