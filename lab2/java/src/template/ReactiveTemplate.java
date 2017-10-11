package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

public class ReactiveTemplate implements ReactiveBehavior {

	private double pPickup;
	private int numActions;
	private Agent myAgent;

	private HashMap<template.Action, Double> rewards = new HashMap<template.Action, Double>();
	private HashMap<State, Double> probabilities = new HashMap<State, Double>();
	private HashMap<State, template.Action> bestActions = new HashMap<State, template.Action>();
	private HashMap<State, Double> bestValues = new HashMap<State, Double>();

	private List<State> allStates;
	private HashMap<Topology.City, List<State>> statesForCity = new HashMap<Topology.City, List<State>>();

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		if (Math.abs(discount) > 1) {
			this.pPickup = 0.95;
			System.out.println("Discount factor must be in [0;1]. Default is 0.95");
		} else {
			this.pPickup = discount;
		}
		this.numActions = 0;
		this.myAgent = agent;

		// create the tables used for learning the strategy using the value
		// iteration algorithm
		allStates = createStates(topology);
		rewards = createRewardTable(td, agent);
		probabilities = createProbabilityTable(topology, td);

		System.out.println("--- " + name() + " ---");
		System.out.println("Learning the strategy...");
		learnStrategy();
		System.out.println("Strategy learned");
		System.out.println("Setup completed");
	}
	
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		// The agent applies its learned strategy here

		Action action;
		template.Action bestAction;
		City currentCity = vehicle.getCurrentCity();

		//If there is a task in the present city :
		if (availableTask != null && availableTask.pickupCity == currentCity) {
			State state = new State(currentCity, availableTask.deliveryCity);
			bestAction = bestActions.get(state); //Get the best action for this state

			//Execute the action :
			if (bestAction.isPickUpTask()) {action = new Pickup(availableTask);} 
			else {action = new Move(bestAction.cityTo());}
			
		} else {
			State state = new State(currentCity, null);
			bestAction = bestActions.get(state);
			action = new Move(bestAction.cityTo());
		}

		if (numActions >= 1) {
			System.out.println(name() + " -- The total profit after " + numActions + " actions is " + myAgent.getTotalProfit()
					+ " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
		}
		numActions++;
		
		return action;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		// The agent applies its learned strategy here

		Action action;
		template.Action bestAction;
		City currentCity = vehicle.getCurrentCity();

		// If there is a task in the present city :
		if (availableTask != null && availableTask.pickupCity == currentCity) {
			State state = new State(currentCity, availableTask.deliveryCity);
			bestAction = bestActions.get(state); // Get the best action for this
													// state

			// Execute the action :
			if (bestAction.isPickUpTask()) {
				action = new Pickup(availableTask);
			} else {
				action = new Move(bestAction.cityTo());
			}

		} else {
			State state = new State(currentCity, null);
			bestAction = bestActions.get(state);
			action = new Move(bestAction.cityTo());
		}

		if (numActions >= 1) {
			System.out.println(name() + " -- The total profit after " + numActions + " actions is " + myAgent.getTotalProfit()
					+ " (average profit: " + (myAgent.getTotalProfit() / (double) numActions) + ")");
		}
		numActions++;

		return action;
	}

	/**
	 * Create all the possible states. <br>
	 * -- Also fills bestValues (HashMap) with an initial low score (State ->
	 * score)<br>
	 * -- Also fills statesForCity : mapping between a city and its states
	 * 
	 * @param topology
	 * @return A list of all possible states
	 */
	private List<State> createStates(Topology topology) {
		List<State> allStates = new ArrayList<State>();
		List<City> allCities = topology.cities();

		for (City cityFrom : allCities) {
			List<State> states = new ArrayList<State>();


			// Firstly : Create a state for with a task for cityFrom -> any
			// city.
			for (City cityTo : allCities) {
				if (!cityFrom.equals(cityTo)) {
					State stateWithTask = new State(cityFrom, cityTo);
					allStates.add(stateWithTask);
					states.add(stateWithTask);
					bestValues.put(stateWithTask, (double) -Double.MAX_VALUE); // instantiate
																				// with
																				// very
																				// low
																				// score...
				}
			}

			// Secondly : Create another state without any task in cityFrom
				bestValues.put(stateWithTask, (double) -Double.MAX_VALUE); //instantiate with very low score...
				}
			}
			
			//Secondly : Create another state without any task in cityFrom 
			State stateWithoutTask = new State(cityFrom, null);
			allStates.add(stateWithoutTask);
			states.add(stateWithoutTask);
			bestValues.put(stateWithoutTask, (double) -Double.MAX_VALUE);
			statesForCity.put(cityFrom, states);
		}
		return allStates;
	}

	/**
	 * This method computes the cost or reward for all actions of all states
	 * 
	 * @param taskDistribution
	 * @param agent
	 * @return rewards ; the mapping between any possible action and its
	 *         'reward' (reward - cost balance)
	 */
	private HashMap<template.Action, Double> createRewardTable(TaskDistribution taskDistribution, Agent agent) {
		HashMap<template.Action, Double> rewards = new HashMap<template.Action, Double>();

		for (State state : allStates) {
			List<template.Action> actions = state.getActions();

			for (template.Action action : actions) {
				double expectedReward = 0;
				if (action.isPickUpTask()) {
					expectedReward = taskDistribution.reward(action.cityFrom(), action.cityTo());
				}
				// distance in km
				double distance = action.cityFrom().distanceTo(action.cityTo());
				// get the first vehicle of the agent
				double cost = agent.vehicles().get(0).costPerKm();
				rewards.put(action, expectedReward - distance * cost);
			}
		}
		return rewards;
	}

	/**
	 * We recover the probabilities given in the taskDistribution and map them
	 * with all our states.
	 * 
	 * @param topology
	 * @param taskDistribution
	 * @return probabilities : a mapping between any state and its probability
	 */
	private HashMap<State, Double> createProbabilityTable(Topology topology, TaskDistribution taskDistribution) {
		HashMap<State, Double> probabilities = new HashMap<State, Double>();

		for (State state : allStates) {
			double probability = 0;

			if (state.hasAvailableTask()) {
				probability = taskDistribution.probability(state.currentCity(), state.destinationCity());
			} else {
				probability = taskDistribution.probability(state.currentCity(), null);
			}
			probabilities.put(state, probability);
		}
		return probabilities;
	}

	/**
	 * This method implements the iteration value algorithm.
	 */
	public void learnStrategy() {
		boolean hasConverged = false;
		while (!hasConverged) {
			hasConverged = true;

			for (State state : allStates) {
				List<template.Action> actionsForState = state.getActions();
				double maxQValue = bestValues.get(state);

				for (template.Action action : actionsForState) {
					double acc = 0;
					/*
					 * If the current city of the state s' at time t+1 does not
					 * correspond to the destination city of the action
					 * performed in state s at time t, then the probability of
					 * reaching state s' at time t+1 given that the agent is at
					 * state s at time t and performs action a is zero.
					 * Therefore, only the states involving the destination city
					 * of the action are considered here.
					 */

					City cityTo = action.cityTo();
					for (State nextState : statesForCity.get(cityTo)) {
						acc += probabilities.get(nextState) * bestValues.get(nextState);
					}

					// select the maximum Q value over all actions for this
					// state
					double qValue = rewards.get(action) + this.pPickup * acc;
					if (qValue > maxQValue) {
						maxQValue = qValue;
						bestActions.put(state, action);
						bestValues.put(state, maxQValue);
						hasConverged = false;
					}
				}
			}
		}
	}

	/**
	 * Getter for the name of an agent
	 *
	 * @return the name of the agent
	 */
	private String name() {
		return myAgent.name();
	}
}