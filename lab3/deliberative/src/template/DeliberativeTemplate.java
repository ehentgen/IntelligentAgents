package template;

/* import table */
import logist.simulation.Vehicle;

import java.util.ArrayList;
import java.util.List;

import cern.colt.Arrays;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;


/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm {BFS, ASTAR, NAIVE}
	
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
		String algorithmName = agent.readProperty("algorithm", String.class, "NAIVE");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			System.out.println("Running ASTAR Algo");
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			System.out.println("Running BFS Algo");
			plan = bfsPlan(vehicle, tasks);
			break;
		case NAIVE:
			plan = naivePlan(vehicle, tasks);
			break;			
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}
	
	/**
	 * -> Q represents the states we still have to go through <br>
	 * -> C represents the states we already go through, and don't want to cycle through (again) <br>
	 * -> n is just a state <br>
	 * 
	 * More than a state-based BFS, this function should keep track of the best path, in order to compute the plan afterwards.<br>
	 * the best way of doing it is probably to keep this in the state itself as a List\Action\ (how did I come here?)
	 * 
	 * @param vehicle
	 * @param tasks
	 * @return
	 */
	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {
		List<DeliberativeState> Q = null;
		Q.add(new DeliberativeState(tasks, null, vehicle.getCurrentCity(), null)); // initial node
		
		List<DeliberativeState> loopCheck = null;
		List<DeliberativeState> S = null;
		DeliberativeState n;
		
		
		while (!Q.isEmpty()) {
			/* n <- first elem of Q && Q <- rest(Q) */
			n = Q.remove(0);
			
			if (n.isFinalState())
				System.out.println("Return n. To implement"); //TODO
			
			if (!loopCheck.contains(n)) { //?? only null?
				loopCheck.add(n);
				
				/* S <- successors(n) */
				S = (n.getSuccessors());
			}
			
			Q.addAll(S);
			
			/* if Q is empty, return Failure */
			if (Q.isEmpty())
				System.out.println("Q is empty ! Error in BFS!");
				System.exit(0);
		}
		
		
		return buildPlan();
	}
	
	private Plan buildPlan() {
		Plan p = new Plan(null);
		//TODO
		return p;
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
