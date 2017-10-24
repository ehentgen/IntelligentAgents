package template;

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
 * 
 * @author Jean-Thomas Furrer
 * @author Emily Hentgen
 *
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

    enum Algorithm {
	BFS, ASTAR, NAIVE
    }

    /* Environment */
    Topology topology;
    TaskDistribution td;

    /* the properties of the agent */
    Agent agent;
    int capacity;
    TaskSet carriedTasks;

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

	// Compute the plan with the selected algorithm.
	switch (algorithm) {
	case ASTAR:
	    // ...
	    System.out.println("Running ASTAR Algorithm");
	    start = System.currentTimeMillis();
	    plan = astarPlan(vehicle, tasks);
	    end = System.currentTimeMillis();
	    System.out.println("Execution time: " + (end - start) + "ms");
	    break;
	case BFS:
	    // ...
	    System.out.println("Running BFS Algorithm");
	    start = System.currentTimeMillis();
	    plan = bfsPlan(vehicle, tasks);
	    end = System.currentTimeMillis();
	    System.out.println("Execution time: " + (end - start) + "ms");
	    break;
	case NAIVE:
	    plan = naivePlan(vehicle, tasks);
	    break;
	default:
	    throw new AssertionError("Should not happen.");
	}
	return plan;
    }

    private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {

	// the previous plan has been cancelled and a new one needs to be
	// computed: add the tasks the vehicle may still be carrying to the set
	// of tasks to deliver
	if (carriedTasks != null) {
	    tasks.addAll(carriedTasks);
	}

	int[] tasksStatus = new int[tasks.size()];
	// by default, no task has been picked up yet
	Arrays.fill(tasksStatus, 0);
	// if the previous plan has been cancelled, the vehicle may already be
	// carrying some tasks
	if (carriedTasks != null) {
	    int numberOfTasks = tasks.size();
	    Task[] tasksList = tasks.toArray(new Task[tasks.size()]);
	    for (int i = 0; i < numberOfTasks; ++i) {
		if (carriedTasks.contains(tasksList[i])) {
		    tasksStatus[i] = DeliberativeState.PICKED_UP;
		}
	    }
	}

	// initialize the starting state of the BFS algorithm
	LinkedList<DeliberativeState> Q = new LinkedList<DeliberativeState>();

	DeliberativeState initialState = new DeliberativeState(tasksStatus,
		new ArrayList<Task>(tasks), -1, vehicle.getCurrentCity(), 0, 0,
		null); // initial node
	Q.add(initialState);

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
	System.out.println("cost:" + minimumCost);

	City initialCity = vehicle.getCurrentCity();
	Plan plan = new Plan(initialCity);
	plan = buildPlan(finalState, plan, new ArrayList<Task>(tasks));
	System.out.println("Plan: " + plan.toString());

	return plan;
    }

    private Plan astarPlan(final Vehicle vehicle, final TaskSet tasks) {
	// DeliberativeState comparator: orders DeliberativeStates by increasing
	// costs
	Comparator<DeliberativeState> comparator = new Comparator<DeliberativeState>() {

	    @Override
	    public int compare(DeliberativeState o1, DeliberativeState o2) {
		return Double.compare(o1.cost(), o2.cost());
	    }
	};

	// the previous plan has been cancelled and a new one needs to be
	// computed: add the tasks the vehicle may still be carrying to the set
	// of tasks to deliver
	if (carriedTasks != null) {
	    tasks.addAll(carriedTasks);
	}

	int[] tasksStatus = new int[tasks.size()];
	// by default, no task has been picked up yet
	Arrays.fill(tasksStatus, 0);
	// if the previous plan has been cancelled, the vehicle may already be
	// carrying some tasks
	if (carriedTasks != null) {
	    int numberOfTasks = tasks.size();
	    Task[] tasksList = tasks.toArray(new Task[tasks.size()]);
	    for (int i = 0; i < numberOfTasks; ++i) {
		if (carriedTasks.contains(tasksList[i])) {
		    tasksStatus[i] = DeliberativeState.PICKED_UP;
		}
	    }
	}

	// initialize the starting state of the ASTAR algorithm
	PriorityQueue<DeliberativeState> Q = new PriorityQueue<DeliberativeState>(
		comparator);

	DeliberativeState initialState = new DeliberativeState(tasksStatus,
		new ArrayList<Task>(tasks), -1, vehicle.getCurrentCity(), 0, 0,
		null); // initial node
	Q.add(initialState);

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
	System.out.println("cost:" + minimumCost);

	City initialCity = vehicle.getCurrentCity();
	Plan plan = new Plan(initialCity);
	plan = buildPlan(finalState, plan, new ArrayList<Task>(tasks));
	System.out.println("Plan: " + plan.toString());

	return plan;
    }

    private Plan buildPlan(DeliberativeState state, Plan plan, List<Task> tasks) {
	DeliberativeState previousState = state.previous();

	// the backtracking process has not reached the initial node yet
	if (previousState != null) {
	    buildPlan(previousState, plan, tasks);

	    City previousCity = previousState.departure();
	    City currentCity = state.departure();

	    int i = state.taskIndex();
	    int taskStatus = state.taskStatus(i);
	    Task task = tasks.get(i);

	    for (City city : previousCity.pathTo(currentCity))
		plan.appendMove(city);

	    if (taskStatus == DeliberativeState.PICKED_UP) {
		plan.appendPickup(task);
	    } else if (taskStatus == DeliberativeState.DELIVERED) {
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
	    this.carriedTasks = carriedTasks;
	}
    }
}
