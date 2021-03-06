package templateJT;

/* import table */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate3 implements DeliberativeBehavior {

    enum Algorithm {
	BFS, ASTAR, NAIVE
    }

    /* Environment */
    Topology topology;
    TaskDistribution td;

    /* the properties of the agent */
    Agent agent;
    int capacity;

    /* the planning class */
    Algorithm algorithm;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {
	this.topology = topology;
	this.td = td;
	this.agent = agent;

	// initialize the planner
	int capacity = agent.vehicles().get(0).capacity();
	String algorithmName = agent.readProperty("algorithm", String.class,
		"NAIVE");

	// Throws IllegalArgumentException if algorithm is unknown
	algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

	// ...
    }

    @Override
    public Plan plan(Vehicle vehicle, TaskSet tasks) {
	Plan plan;
	long start = 0;
	long end = 0;
	
	long startTime = System.currentTimeMillis();

	// Compute the plan with the selected algorithm.
	switch (algorithm) {
	case ASTAR:
	    System.out.println("Running ASTAR Algorithm");
	    plan = astarPlan(vehicle, tasks);
	    break;
	case BFS:
	    System.out.println("Running BFS Algorithm");
	    plan = bfsPlan(vehicle, tasks);
	    break;
	case NAIVE:	
	    plan = naivePlan(vehicle, tasks);
	    break;
	default:
	    throw new AssertionError("Should not happen.");
	}
	
	long endTime = System.currentTimeMillis();
	System.out.println(plan.toString());	
	System.out.println("Time to compute: " + (endTime - startTime) / 1000.0 + " sec");
	
	return plan;
    }

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
    private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {
	// initialize the starting state of the BFS algorithm
	LinkedList<DeliberativeState3> Q = new LinkedList<DeliberativeState3>();

	int[] tasksStatus = new int[tasks.size()];
	Arrays.fill(tasksStatus, 0);
	DeliberativeState3 initialState = new DeliberativeState3(tasksStatus,
		new ArrayList<Task>(tasks), -1, vehicle.getCurrentCity(), 0, 0,
		null); // initial node
	Q.add(initialState);

	Set<DeliberativeState3> loopCheck = new HashSet<DeliberativeState3>();
	Set<DeliberativeState3> S = null;
	DeliberativeState3 currentState;

	// stores the final cost in the agent's plan resulting from the BFS
	// algorithm (which is also the minimum cost)
	Double minimumCost = Double.MAX_VALUE;
	// stores the final state in the agent's plan resulting from the BFS
	// algorithm
	DeliberativeState3 finalState = null;

	int number_of_iterations = 0;
	int number_of_states = 0;

	while (!Q.isEmpty()) {
	    currentState = Q.pop();

	    // a plan where all tasks are delivered has been found
	    if (currentState.isFinalState()) {
		// check whether this is the best plan so far
		if (currentState.cost() < minimumCost) {
		    minimumCost = currentState.cost();
		    finalState = currentState;
		}
	    }

	    if (!loopCheck.contains(currentState)) {
		loopCheck.add(currentState);
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
	System.out.println("cost:" + minimumCost);

	City initialCity = vehicle.getCurrentCity();
	Plan plan = new Plan(initialCity);
	plan = buildPlan(finalState, plan, new ArrayList<Task>(tasks));

	return plan;
    }

    private Plan astarPlan(final Vehicle vehicle, final TaskSet tasks) {
	// DeliberativeState comparator: orders DeliberativeStates by increasing
	// costs
	Comparator<DeliberativeState3> comparator = new Comparator<DeliberativeState3>() {

	    @Override
	    public int compare(DeliberativeState3 o1, DeliberativeState3 o2) {
		// return Double.compare(score(o1), score(o2));
		return Double.compare(o1.cost(), o2.cost());
	    }

	    private double score(DeliberativeState3 state) {
		double accumulatedCost = state.cost();
		double futureHeuristicCost = Double.MAX_VALUE;
		double heuristicCost = 0;

		double costPerKm = vehicle.costPerKm();

		int numberOfTasks = tasks.size();
		for (int i = 0; i < numberOfTasks; ++i) {
		    Task task = state.tasks().get(i);

		    if (state.taskStatus(i) == DeliberativeState3.NOT_PICKED_UP) {
			heuristicCost = (state.departure().distanceTo(
				task.pickupCity) + task.pathLength())
				* costPerKm;
		    } else if (state.taskStatus(i) == DeliberativeState3.PICKED_UP) {
			heuristicCost += (state.departure()
				.distanceTo(task.deliveryCity)) * costPerKm;
		    }

		    if (heuristicCost < futureHeuristicCost) {
			futureHeuristicCost = heuristicCost;
		    }
		}

		return accumulatedCost + futureHeuristicCost;
	    }
	};

	// initialize the starting state of the ASTAR algorithm
	PriorityQueue<DeliberativeState3> Q = new PriorityQueue<DeliberativeState3>(
		comparator);

	int[] tasksStatus = new int[tasks.size()];
	Arrays.fill(tasksStatus, 0);
	DeliberativeState3 initialState = new DeliberativeState3(tasksStatus,
		new ArrayList<Task>(tasks), -1, vehicle.getCurrentCity(), 0, 0,
		null); // initial node
	Q.add(initialState);

	Set<DeliberativeState3> loopCheck = new HashSet<DeliberativeState3>();
	Set<DeliberativeState3> S = null;
	DeliberativeState3 currentState;

	// stores the final cost in the agent's plan resulting from the BFS
	// algorithm (which is also the minimum cost)
	Double minimumCost = Double.MAX_VALUE;
	// stores the final state in the agent's plan resulting from the BFS
	// algorithm
	DeliberativeState3 finalState = null;

	int number_of_iterations = 0;
	int number_of_states = 0;

	while (!Q.isEmpty()) {
	    // retrieve the state with the least cost in the PriorityQueue
	    // currentState = Q.poll();
	    currentState = Q.poll();

	    // a plan where all tasks are delivered has been found
	    if (currentState.isFinalState()) {
		// check whether this is the best plan so far
		if (currentState.cost() < minimumCost) {
		    minimumCost = currentState.cost();
		    finalState = currentState;
		    break;
		}
	    }

	    if (!loopCheck.contains(currentState)) {
		loopCheck.add(currentState);
		S = currentState.getSuccessors(agent);
		// sorting and merging of the successors of the current state by
		// increasing cost is handled by the PriorityQueue
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
	System.out.println("cost:" + minimumCost);

	City initialCity = vehicle.getCurrentCity();
	Plan plan = new Plan(initialCity);
	plan = buildPlan(finalState, plan, new ArrayList<Task>(tasks));
	System.out.println("Plan: " + plan.toString());

	return plan;
    }

    private Plan buildPlan(DeliberativeState3 state, Plan plan, List<Task> tasks) {
	DeliberativeState3 previousState = state.previous();

	// the initial node has not been reached yet
	if (previousState != null) {
	    buildPlan(previousState, plan, tasks);

	    City previousCity = previousState.departure();
	    City currentCity = state.departure();

	    int i = state.taskIndex();
	    int taskStatus = state.taskStatus(i);
	    Task task = tasks.get(i);

	    for (City city : previousCity.pathTo(currentCity))
		plan.appendMove(city);

	    if (taskStatus == DeliberativeState3.PICKED_UP) {
		plan.appendPickup(task);
	    } else if (taskStatus == DeliberativeState3.DELIVERED) {
		plan.appendDelivery(task);
	    }
	}

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
	    // This cannot happen for this simple agent, but typically
	    // you will need to consider the carriedTasks when the next
	    // plan is computed.
	}
    }
}
