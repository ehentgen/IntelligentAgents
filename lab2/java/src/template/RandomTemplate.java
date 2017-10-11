package template;

import java.util.Random;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class RandomTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		this.random = new Random();
		if (Math.abs(discount) > 1) {
			this.pPickup = 0.95;
			System.out.println("Discount factor must be in [0;1]. Default is 0.95");
		} else {
			this.pPickup = discount;
		}
		this.numActions = 0;
		this.myAgent = agent;

		// no learning strategy
		System.out.println("--- " + name() + " ---");
		System.out.println("Setup completed");
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		// The random agent picks up a task if it is available and according to
		// a certain probability, and otherwise randomly moves to a neighboring
		// city
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}

		if (numActions >= 1) {
			System.out.println(name() + " -- The total profit after " + numActions + " actions is " + myAgent.getTotalProfit()
					+ " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
		}
		numActions++;

		return action;
	}

	private String name() {
		return "Random agent";
	}

}