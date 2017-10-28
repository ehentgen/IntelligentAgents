package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedTemplate implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
	    Agent agent) {

	// this code is used to get the timeouts
	LogistSettings ls = null;
	try {
	    ls = Parsers.parseSettings("config\\settings_default.xml");
	} catch (Exception exc) {
	    System.out
		    .println("There was a problem loading the configuration file.");
	}

	// the setup method cannot last more than timeout_setup milliseconds
	timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
	// the plan method cannot execute more than timeout_plan milliseconds
	timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

	this.topology = topology;
	this.distribution = distribution;
	this.agent = agent;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
	long time_start = System.currentTimeMillis();

	// System.out.println("Agent " + agent.id() + " has tasks " + tasks);
	// Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

	List<Plan> plans = new ArrayList<Plan>();
	// plans.add(planVehicle1);

	double probability = 0.3;
	StochasticLocalSearch stochasticLocalSearch = new StochasticLocalSearch(
		vehicles, tasks, probability, timeout_plan);

	CentralizedPlan centralizedPlan = stochasticLocalSearch.createPlan();

	for (Vehicle vehicle : vehicles) {
	    plans.add(buildPlan(centralizedPlan, vehicle));
	}
	while (plans.size() < vehicles.size()) {
	    plans.add(Plan.EMPTY);
	}

	long time_end = System.currentTimeMillis();
	long duration = time_end - time_start;
	System.out.println("The plan was generated in " + duration
		+ " milliseconds.");
	System.out.println("Stochastic Local Search stopped: "
		+ stochasticLocalSearch.stochasticLocalSearchStopCause());
	System.out.println("Plan cost: " + centralizedPlan.cost());

	return plans;
    }

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
	City current = vehicle.getCurrentCity();
	Plan plan = new Plan(current);

	for (Task task : tasks) {
	    // move: current city => pickup location
	    for (City city : current.pathTo(task.pickupCity)) {
		plan.appendMove(city);
	    }

	    plan.appendPickup(task);

	    // move: pickup location => delivery location
	    for (City city : task.path()) {
		plan.appendMove(city);
	    }

	    plan.appendDelivery(task);

	    // set current city
	    current = task.deliveryCity;
	}
	return plan;
    }

    private Plan buildPlan(CentralizedPlan centralizedPlan, Vehicle vehicle) {
	City currentCity = vehicle.getCurrentCity();
	Plan plan = new Plan(currentCity);

	Task task = centralizedPlan.vehicleToFirstTask().get(vehicle);
	while (task != null) {
	    for (City city : currentCity.pathTo(task.pickupCity)) {
		plan.appendMove(city);
	    }
	    plan.appendPickup(task);
	    for (City city : task.pickupCity.pathTo(task.deliveryCity)) {
		plan.appendMove(city);
	    }
	    plan.appendDelivery(task);
	    currentCity = task.deliveryCity;
	    task = centralizedPlan.taskToTask().get(task);
	}
	return plan;
    }

}
