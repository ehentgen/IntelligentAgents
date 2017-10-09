package template;

import java.util.HashMap;
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

public class RoutineDummyTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;

	private HashMap<City, City> itinerary;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;

		// no learning strategy, but creates a fixed default itinerary between
		// cities
		System.out.println("--- " + name() + " ---");
		itinerary = createItinerary(topology);
		System.out.println("Setup completed");
	}

	private HashMap<City, City> createItinerary(Topology topology) {
		HashMap<City, City> itinerary = new HashMap<Topology.City, Topology.City>();

		for (City city : topology.cities()) {
			itinerary.put(city, city.randomNeighbor(random));
		}

		return itinerary;
	}

	private City nextCity() {
		return itinerary.get(myAgent.vehicles().get(0).getCurrentCity());
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		// The dummy agent always picks up a task if it is available, and
		// otherwise moves between cities according to the same (randomly)
		// predefined path
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			action = new Move(nextCity());
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
		return "Routine agent";
	}

}
