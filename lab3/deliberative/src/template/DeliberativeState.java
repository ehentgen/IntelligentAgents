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
    private Set<Task> deliveredTasks;

    private List<Action> actionsPerformed;

    private City departure;

    private double cost;
    private double charge;

    public DeliberativeState(Set<Task> notPickedUpTasks,
	    Set<Task> pickedUpTasks, Set<Task> deliveredTasks, City departure,
	    double charge, double cost, List<Action> wayToHere) {

	this.notPickedUpTasks = notPickedUpTasks;

	this.pickedUpTasks = pickedUpTasks;
	this.deliveredTasks = deliveredTasks;

	this.departure = departure;
	this.cost = cost;
	this.charge = charge;

	// This will be super-useful in order to find how to reach the best
	// state
	if (wayToHere == null) {
	    this.actionsPerformed = new ArrayList<Action>();
	} else {
	    this.actionsPerformed = wayToHere;
	}
    }

    public List<DeliberativeState> getSuccessors(Agent agent) {
	List<DeliberativeState> nextPossibleStates = new ArrayList<DeliberativeState>();

	double costPerKm = agent.vehicles().get(0).costPerKm();
	double currentCost = cost;

	for (Task task : notPickedUpTasks) {
	    double updatedCharge = charge + task.weight;

	    // in the next state, try picking up task, if possible
	    if (updatedCharge < agent.vehicles().get(0).capacity()) {
		// remove the task we will pick up
		Set<Task> newNotPickedUpTasks = new HashSet<Task>(
			notPickedUpTasks);
		newNotPickedUpTasks.remove(task);

		// and add it to the picked up tasks list
		Set<Task> newPickedUpTasks = new HashSet<Task>(deliveredTasks);
		newPickedUpTasks.add(task);

		double updatedCost = currentCost
			+ departure.distanceTo(task.pickupCity) * costPerKm;

		City destination = task.pickupCity;

		DeliberativeState s = new DeliberativeState(
			newNotPickedUpTasks, newPickedUpTasks, deliveredTasks,
			destination, updatedCharge, updatedCost,
			actionsPerformed);
		nextPossibleStates.add(s);

		List<City> path = departure.pathTo(destination);
		for (City city : path) {
		    actionsPerformed.add(new Move(city));
		}
		actionsPerformed.add(new Pickup(task));
	    }

	}

	for (Task task : pickedUpTasks) {
	    // remove the picked up but not yet delivered task
	    Set<Task> newPickedUpTasks = new HashSet<Task>(pickedUpTasks);
	    newPickedUpTasks.remove(task);

	    // and add it to the delivered tasks set
	    Set<Task> newDeliveredTasks = new HashSet<Task>(deliveredTasks);
	    newDeliveredTasks.add(task);

	    double updatedCharge = charge - task.weight;
	    double updatedCost = currentCost
		    + departure.distanceTo(task.deliveryCity) * costPerKm;

	    City destination = task.deliveryCity;

	    DeliberativeState s = new DeliberativeState(notPickedUpTasks,
		    newPickedUpTasks, newDeliveredTasks, destination,
		    updatedCharge, updatedCost, actionsPerformed);
	    nextPossibleStates.add(s);

	    List<City> path = departure.pathTo(destination);
	    for (City city : path) {
		actionsPerformed.add(new Move(city));
	    }
	    actionsPerformed.add(new Delivery(task));
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
	double rewardSum = 0;
	for (Task task : deliveredTasks) {
	    rewardSum += task.reward;
	}
	return rewardSum;
    }

    // => There is only one possible final state.
    public boolean isFinalState() {
	return (notPickedUpTasks.isEmpty() && pickedUpTasks.isEmpty());
    }

    public List<Action> actionsPerformed() {
	return actionsPerformed;
    }

}
