package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;

	private HashMap<template.Action, Double> rewards = new HashMap<template.Action, Double>();
	private HashMap<State, Double> probabilities = new HashMap<State, Double>();
	private HashMap<State, template.Action> bestMoves = new HashMap<State, template.Action>();
	private HashMap<State, Double> bestValues = new HashMap<State, Double>();

	private List<State> allStates;
	private HashMap<Topology.City, List<State>> statesForCity = new HashMap<Topology.City, List<State>>();

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;

		// create the tables used for learning the strategy using the value
		// iteration algorithm
		allStates = createStates(topology);
		rewards = createRewardTable(td, agent);
		probabilities = createProbabilityTable(topology, td);

		learnStrategy();
	}

	private List<State> createStates(Topology topology) {
		List<State> allStates = new ArrayList<State>();
		List<City> allCities = topology.cities();

		for (City cityFrom : allCities) {
			List<State> states = new ArrayList<State>();

			for (City cityTo : allCities) {
				if (!cityFrom.equals(cityTo)) {
					State stateWithTask = new State(cityFrom, true, cityTo);
					State stateWithoutTask = new State(cityFrom, false, null);
					allStates.add(stateWithTask);
					allStates.add(stateWithoutTask);
					states.add(stateWithTask);
					states.add(stateWithoutTask);
					bestValues.put(stateWithTask, (double) -Double.MAX_VALUE);
					bestValues.put(stateWithoutTask, (double) -Double.MAX_VALUE);
				}
			}
			statesForCity.put(cityFrom, states);
		}
		return allStates;
	}

	private HashMap<template.Action, Double> createRewardTable(TaskDistribution taskDistribution, Agent agent) {
		HashMap<template.Action, Double> rewards = new HashMap<template.Action, Double>();

		for (State state : allStates) {
			List<template.Action> actions = state.getActions();

			for (template.Action action : actions) {
				double reward = 0;
				if (action.isPickUpTask()) {
					reward = taskDistribution.reward(action.cityFrom(), action.cityTo());
				}
				// distance in km
				double distance = action.cityFrom().distanceTo(action.cityTo());
				// get the first vehicle of the agent (?)
				double cost = agent.vehicles().get(0).costPerKm();
				rewards.put(action, reward - distance * cost);
			}
		}
		return rewards;
	}

	private HashMap<State, Double> createProbabilityTable(Topology topology, TaskDistribution taskDistribution) {
		HashMap<State, Double> probabilities = new HashMap<State, Double>();

		for (State state : allStates) {
			double probability = 0;
			if (state.hasAvailableTask()) {
				probability = taskDistribution.probability(state.currentCity(), state.destinationCity());
			} else {
				probability = 1 - isTaskInCityProbability(topology, taskDistribution, state.currentCity());
			}
			probabilities.put(state, probability);
		}
		return probabilities;
	}

	private double isTaskInCityProbability(Topology topology, TaskDistribution taskDistribution, City city) {

		double probability = 0;
		for (City cityTo : topology.cities()) {
			probability += taskDistribution.probability(city, cityTo);
		}
		return probability;
	}

	public void learnStrategy() {
		System.out.println("Learning the strategy...");
		boolean hasConverged = false;
		while (!hasConverged) {
			hasConverged = true;

			for (State state : allStates) {
				double maxQValue = 0;
				List<template.Action> actionsForState = state.getActions();

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
						bestMoves.put(state, action);
						bestValues.put(state, maxQValue);
						hasConverged = false;
					}
				}
			}
		}
		System.out.println("Strategy learned");
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}

		if (numActions >= 1) {
			System.out.println("The total profit after " + numActions + " actions is " + myAgent.getTotalProfit() + " (average profit: "
					+ (myAgent.getTotalProfit() / (double) numActions) + ")");
		}
		numActions++;

		return action;
	}
}
