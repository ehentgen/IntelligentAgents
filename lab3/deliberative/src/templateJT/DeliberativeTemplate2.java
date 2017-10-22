package templateJT;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Scanner;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import templateJT.*;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate2 implements DeliberativeBehavior {

	enum Algorithm {
		BFS, ASTAR
	}

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;
	
	int maxStates = 0;

	/* the planning class */
	Algorithm algorithm;

	TaskSet carriedTaskAfterCancellation;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;

		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class,
				"ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

		carriedTaskAfterCancellation = null;
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		long startTime = System.currentTimeMillis();
		switch (algorithm) {
		case ASTAR:
			plan = aStarPlan(vehicle, tasks);
			break;
		case BFS:
			plan = bfsPlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		
		System.out.println("Max # of states : " + maxStates);
		
		long endTime = System.currentTimeMillis();
		System.out.println("Time to compute: " + (endTime - startTime) / 1000.0 + " sec");
		System.out.println(plan.toString());
		return plan;
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {

		if (!carriedTasks.isEmpty()) {
			carriedTaskAfterCancellation = carriedTasks;
		} else {
			carriedTaskAfterCancellation = null;
		}
	}

	private Plan aStarPlan(Vehicle vehicle, TaskSet tasks) {

		int capacity = vehicle.capacity();
		City firstCity = vehicle.getCurrentCity();

		// create an empty task set
		TaskSet emptySet = TaskSet.copyOf(tasks);
		emptySet.removeAll(tasks);

		StateNode currentState = new StateNode(
				firstCity,
				(carriedTaskAfterCancellation != null ? carriedTaskAfterCancellation
						: emptySet), tasks, null);
		currentState.setG(0);
		currentState.setH(0);

		PriorityQueue<StateNode> c = new PriorityQueue<StateNode>(
				new Comparator<StateNode>() {

					@Override
					public int compare(StateNode o1, StateNode o2) {

						if (o1.getF() > o2.getF())
							return 1;
						else if (o1.getF() < o2.getF())
							return -1;
						else
							return 0;
					}
				});

		// allow us to know quickly if a state deserve to be put inside the
		// priority queue
		HashMap<StateNode, Double> f = new HashMap<StateNode, Double>();

		c.add(currentState);
		f.put(currentState, currentState.getF());

		System.out.println("Deliberative with a*");
		do {

			currentState = c.poll();

			if (currentState == null) {
				throw (new java.lang.IllegalArgumentException(
						"Unreachable city(ies)"));
			}

			if (currentState.isFinalState()) {
				break;
			}

			boolean hasDeliver = false;
			// create the delivery action
			for (Task t : currentState.getCarriedTasks()) {

				if (t.deliveryCity.equals(currentState.getCurrentCity())) {

					hasDeliver = true;

					ActionEdge a = new ActionEdge(currentState, null, false, t);
					TaskSet newTaskSet = TaskSet.copyOf(currentState
							.getCarriedTasks());
					newTaskSet.remove(t);
					StateNode nextState = new StateNode(
							currentState.getCurrentCity(), newTaskSet,
							currentState.getRemainingTasks(), a);
					nextState.setG(currentState.getG());
					nextState.setH(heuristic(nextState));

					if (!f.containsKey(nextState)
							|| f.get(nextState) <= nextState.getF()) {
						f.put(nextState, nextState.getF());
						c.add(nextState);
					}
					break;
				}

			}

			// if we can deliver it doesn't make sense to look for others
			// possible action at this state, it will only make our queue
			// bigger for nothing
			if (hasDeliver) {
				continue;
			}

			// create the move action
			for (City n : currentState.getCurrentCity().neighbors()) {

				ActionEdge a = new ActionEdge(currentState, n, false, null);
				StateNode nextState = new StateNode(n,
						currentState.getCarriedTasks(),
						currentState.getRemainingTasks(), a);
				nextState.setG(currentState.getG()
						+ currentState.getCurrentCity().distanceTo(n));
				nextState.setH(heuristic(nextState));

				if (!f.containsKey(nextState)
						|| f.get(nextState) <= nextState.getF()) {
					f.put(nextState, nextState.getF());
					c.add(nextState);
				}
			}

			// create the pickup action
			for (Task t : currentState.getRemainingTasks()) {

				if (t.pickupCity.equals(currentState.getCurrentCity())) {

					// check overweight
					if (t.weight <= capacity - currentState.getWeight()) {
						ActionEdge a = new ActionEdge(currentState, null, true,
								t);
						TaskSet newRemainingTaskSet = TaskSet
								.copyOf(currentState.getRemainingTasks());
						newRemainingTaskSet.remove(t);
						TaskSet newCarriedTaskSet = TaskSet.copyOf(currentState
								.getCarriedTasks());
						newCarriedTaskSet.add(t);
						StateNode nextState = new StateNode(
								currentState.getCurrentCity(),
								newCarriedTaskSet, newRemainingTaskSet, a);

						nextState.setG(currentState.getG());
						nextState.setH(heuristic(nextState));

						if (!f.containsKey(nextState)
								|| f.get(nextState) <= nextState.getF()) {
							f.put(nextState, nextState.getF());
							c.add(nextState);
						}
					}
				}
			}

		} while (!c.isEmpty());

		return constructPlan(currentState, firstCity);
	}

	// heuristic using the minimum spanning tree
	// Here minimum over the distance between cities
	// with a pickup or delivery action(s)
	private double heuristic(StateNode s) {
		
		HashSet<City> neededCities = new HashSet<Topology.City>();
		
		for (Task t : s.getCarriedTasks()) {
			neededCities.add(t.deliveryCity);
		}
		
		for (Task t : s.getRemainingTasks()) {
			neededCities.add(t.deliveryCity);
			neededCities.add(t.pickupCity);
		}
		neededCities.add(s.getCurrentCity());
		// if no cities needed we will reach a final state
		if (neededCities.isEmpty())
			return 0.0;

		PriorityQueue<EdgeCity> p = new PriorityQueue<EdgeCity>(
				new Comparator<EdgeCity>() {

					@Override
					public int compare(EdgeCity o1, EdgeCity o2) {

						if (o1.distance < o2.distance)
							return -1;
						else if (o1.distance > o2.distance)
							return 1;
						else
							return 0;
					}
				});

		HashSet<EdgeCity> e = new HashSet<EdgeCity>();

		City cur = s.getCurrentCity();
		neededCities.remove(cur);

		HashSet<EdgeCity> result = new HashSet<EdgeCity>();

		while (!neededCities.isEmpty()) {

			for (City c : neededCities) {
				EdgeCity newEdge = new EdgeCity(c, cur, c.distanceTo(cur));
				if (!e.contains(newEdge)) {
					e.add(newEdge);
					p.add(newEdge);
				}
			}
			if (!p.isEmpty()) {
				boolean done = false;
				EdgeCity curEdge = null;
				City newCur = cur;
				do {
					curEdge = p.poll();
					if (neededCities.contains(curEdge.from)) {
						newCur = curEdge.from;
						done = true;
					}
					else if (neededCities.contains(curEdge.to)) {
						newCur = curEdge.to;
						done = true;
					}
				} while(!done && !p.isEmpty());
				
				cur = newCur;
				
				if (curEdge == null) {
					System.out.println("mmmh");
				}
				
				result.add(curEdge);
				neededCities.remove(cur);
			}
		}

		double sum = 0.0;
		for (EdgeCity c : result)
			sum += c.distance;

		return sum;
		
	}

	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {

		int capacity = vehicle.capacity();
		City firstCity = vehicle.getCurrentCity();

		// create an empty task set
		TaskSet emptySet = TaskSet.copyOf(tasks);
		emptySet.removeAll(tasks);

		StateNode currentState = new StateNode(
				firstCity,
				(carriedTaskAfterCancellation != null ? carriedTaskAfterCancellation
						: emptySet), tasks, null);

		HashSet<StateNode> visited = new HashSet<StateNode>();
		LinkedList<StateNode> toVisit = new LinkedList<StateNode>();
		toVisit.add(currentState);

		do {
			currentState = toVisit.poll();
			if (maxStates < toVisit.size()) maxStates = toVisit.size();

			if (currentState == null) {
				throw (new java.lang.IllegalArgumentException(
						"Unreachable city(ies)"));
			}

			if (currentState.isFinalState()) {
				break;
			}

			visited.add(currentState);

			double numb = currentState.getCurrentCity().neighbors().size();
			//System.out.println("Number of neignbours : " + numb);
			
			// create the move action
			for (City n : currentState.getCurrentCity().neighbors()) {

				ActionEdge a = new ActionEdge(currentState, n, false, null);
				StateNode nextState = new StateNode(n,
						currentState.getCarriedTasks(),
						currentState.getRemainingTasks(), a);
				nextState.setG(currentState.getG()
						+ currentState.getCurrentCity().distanceTo(n));

				if (!visited.contains(nextState)) {
					toVisit.add(nextState);
				}
			}

			// create the delivery action
			for (Task t : currentState.getCarriedTasks()) {

				if (t.deliveryCity.equals(currentState.getCurrentCity())) {

					ActionEdge a = new ActionEdge(currentState, null, false, t);
					TaskSet newTaskSet = TaskSet.copyOf(currentState
							.getCarriedTasks());
					newTaskSet.remove(t);
					StateNode nextState = new StateNode(
							currentState.getCurrentCity(), newTaskSet,
							currentState.getRemainingTasks(), a);
					nextState.setG(currentState.getG());

					if (!visited.contains(nextState)) {
						toVisit.add(nextState);
					}
					break;
				}

			}

			// create the pickup action
			for (Task t : currentState.getRemainingTasks()) {
				if (t.pickupCity.equals(currentState.getCurrentCity())
						&& capacity >= currentState.getWeight() + t.weight) {
					ActionEdge a = new ActionEdge(currentState, null, true, t);
					TaskSet newRemainingTaskSet = TaskSet.copyOf(currentState
							.getRemainingTasks());
					newRemainingTaskSet.remove(t);
					TaskSet newCarriedTaskSet = TaskSet.copyOf(currentState
							.getCarriedTasks());
					newCarriedTaskSet.add(t);
					StateNode nextState = new StateNode(
							currentState.getCurrentCity(), newCarriedTaskSet,
							newRemainingTaskSet, a);
					nextState.setG(currentState.getG());

					if (!visited.contains(nextState)) {
						toVisit.add(nextState);
					}

				}

			}

		} while (!toVisit.isEmpty());

		return constructPlan(currentState, firstCity);
	}

	private Plan constructPlan(StateNode s, City fc) {

		StateNode finalState = s;
		LinkedList<ActionEdge> stackOfActions = new LinkedList<ActionEdge>();

		System.out.println("Distance traveled: " + s.getG());

		// first loop, we stack the action in reverse order
		do {

			stackOfActions.add(s.getAction());
			s = s.getAction().getLastState();

		} while (s.getAction() != null);

		Plan plan = new Plan(fc);
		ActionEdge currentAction;

		// second loop we extract from the stack
		// the action in the correct order
		do {

			currentAction = stackOfActions.pollLast();

			if (currentAction.getMoveTo() != null) {
				plan.appendMove(currentAction.getMoveTo());
			} else {
				if (currentAction.isPickup()) {
					plan.appendPickup(currentAction.getTask());
				} else {
					plan.appendDelivery(currentAction.getTask());
				}
			}

		} while (!stackOfActions.isEmpty());

		return plan;
	}

}
