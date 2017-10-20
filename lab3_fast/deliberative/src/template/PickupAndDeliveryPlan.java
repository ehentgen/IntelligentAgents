package template;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import logist.agent.Agent;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

abstract class PickupAndDeliveryPlan {

    /**
     * -> Q represents the states we still have to go through <br>
     * -> C represents the states we already went through, and don't want to
     * cycle through (again) <br>
     * -> n is just a state <br>
     * 
     * More than a state-based BFS, this function should keep track of the best
     * path, in order to compute the plan afterwards.<br>
     * the best way of doing it is probably to keep this in the state itself as
     * a List\Action\ (how did I come here?)
     * 
     * @param vehicle
     * @param tasks
     * @return
     * @return
     */
    public static Plan BFSPlan(Agent agent, Vehicle vehicle, TaskSet tasks) {

	// initialize the starting state of the BFS algorithm
	List<DeliberativeState> Q = new ArrayList<DeliberativeState>();
	Set<Task> notPickedUpTasks = new HashSet<Task>(tasks);
	Set<Task> pickedUpTasks = new HashSet<Task>();

	// DeliberativeState initialState = new DeliberativeState(
	// notPickedUpTasks, pickedUpTasks, vehicle.getCurrentCity(), 0,
	// 0, 0, null); // initial node
	// Q.add(initialState);

	Set<DeliberativeState> loopCheck = new HashSet<DeliberativeState>();
	Set<DeliberativeState> S = null;
	DeliberativeState currentState;

	// stores the final cost in the agent's plan resulting from the BFS
	// algorithm (which is also the minimum cost)
	Double minimumCost = Double.MAX_VALUE;
	// stores the final state in the agent's plan resulting from the BFS
	// algorithm
	DeliberativeState finalState = null;

	int number_of_iterations = 0;
	double number_of_states = 0;

	while (!Q.isEmpty()) {
	    /* n <- first elem of Q && Q <- rest(Q) */
	    currentState = Q.remove(0);

	    // a plan where all tasks are delivered has been found
	    if (currentState.isFinalState()) {
		// check whether this is the best plan so far
		if (currentState.cost() < minimumCost) {
		    minimumCost = currentState.cost();
		    finalState = currentState;
		    System.out.println("min cost:" + minimumCost);
		}
	    }

	    if (!loopCheck.contains(currentState)) { // ?? only null?
		loopCheck.add(currentState);
		/* S <- successors(n) */
		S = currentState.getSuccessors(agent);
		Q.addAll(S);
	    }

	    if (Q.size() > number_of_states) {
		number_of_states = Q.size();
	    }
	    ++number_of_iterations;
	}

	System.out.println("nb states: " + number_of_states);
	System.out.println("nb iterations: " + number_of_iterations);

	return null;// buildPlan(agent, finalState);
    }

    public static Plan astarPlan(Agent agent, Vehicle vehicle, TaskSet tasks) {
	// DeliberativeState comparator: orders DeliberativeStates by increasing
	// costs
	Comparator<DeliberativeState> comparator = new Comparator<DeliberativeState>() {

	    @Override
	    public int compare(DeliberativeState o1, DeliberativeState o2) {
		return Double.compare(o1.cost(), o2.cost());
	    }
	};

	// initialize the starting state of the BFS algorithm
	PriorityQueue<DeliberativeState> Q = new PriorityQueue<DeliberativeState>(
		comparator);

	Set<Task> notPickedUpTasks = new HashSet<Task>(tasks);
	Set<Task> pickedUpTasks = new HashSet<Task>();

	// DeliberativeState initialState = new DeliberativeState(
	// notPickedUpTasks, pickedUpTasks, vehicle.getCurrentCity(), 0,
	// 0, 0, null); // initial node
	// Q.add(initialState);

	Set<DeliberativeState> loopCheck = new HashSet<DeliberativeState>();
	Set<DeliberativeState> S = null;
	DeliberativeState currentState;

	// stores the final cost in the agent's plan resulting from the BFS
	// algorithm (which is also the minimum cost)
	Double minimumCost = Double.MAX_VALUE;
	// stores the final state in the agent's plan resulting from the BFS
	// algorithm
	DeliberativeState finalState = null;

	int number_of_iterations = 0;
	double number_of_states = 0;

	while (!Q.isEmpty()) {

	    // retrieve the state with the least cost in the PriorityQueue
	    currentState = Q.poll();

	    // a plan where all tasks are delivered has been found
	    if (currentState.isFinalState()) {
		// check whether this is the best plan so far
		if (currentState.cost() < minimumCost) {
		    minimumCost = currentState.cost();
		    finalState = currentState;
		    System.out.println("min cost:" + minimumCost);
		}
	    }

	    if (!loopCheck.contains(currentState)) {
		loopCheck.add(currentState);

		S = currentState.getSuccessors(agent);

		// sorting and merging of the successors of the current
		// state by increasing cost is handled by the PriorityQueue
		// implementation
		Q.addAll(S);
	    }

	    if (Q.size() > number_of_states) {
		number_of_states = Q.size();
	    }

	    ++number_of_iterations;
	}

	System.out.println("nb states: " + number_of_states);
	System.out.println("nb iterations: " + number_of_iterations);

	return null; // buildPlan(agent, finalState);
    }

    /*
     * 
     * public static Plan buildPlan(Agent agent, DeliberativeState state) { City
     * initialCity = agent.vehicles().get(0).getCurrentCity(); Plan plan = new
     * Plan(initialCity); List<Action> actions = state.actionsPerformed(); for
     * (Action action : actions) { plan.append(action); }
     * System.out.println(plan.toString()); return plan; }
     */

    public static Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
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

    public static Plan buildPlan(String plan, Agent agent, Vehicle vehicle,
	    TaskSet tasks) {
	// add the tasks the vehicle might be carrying if the initial plan has
	// been cancelled and a new one is being recomputed
	tasks.addAll(agent.vehicles().get(0).getCurrentTasks());

	return BFSPlan(agent, vehicle, tasks);
    }

}
