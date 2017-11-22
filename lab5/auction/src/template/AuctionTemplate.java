package template;

//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
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
public class AuctionTemplate implements AuctionBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private Random random;
    private Vehicle vehicle;
    private City currentCity;

    private long timeout_setup;
    private long timeout_bid;
    private long timeout_plan;

    private StochasticLocalSearch SLS;
    private List<Task> tasksList;

    private double costCurrentPlan;
    private double costNewPlan;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
	    Agent agent) {

	this.topology = topology;
	this.distribution = distribution;
	this.agent = agent;
	this.vehicle = agent.vehicles().get(0);
	this.currentCity = vehicle.homeCity();

	long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
	this.random = new Random(seed);

	// this code is used to get the timeouts
	LogistSettings ls = null;
	try {
	    ls = Parsers.parseSettings("config\\settings_auction.xml");
	} catch (Exception exc) {
	    System.out
		    .println("There was a problem loading the configuration file.");
	}

	// the setup method cannot last more than timeout_setup milliseconds
	timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
	// the bid method cannot execute more than timeout_bid milliseconds
	timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
	// the plan method cannot execute more than timeout_plan milliseconds
	timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

	System.out.println("Timeouts:");
	System.out.println("setup (ms): " + timeout_setup);
	System.out.println("bid   (ms): " + timeout_bid);
	System.out.println("plan  (ms): " + timeout_plan);

	SLS = new StochasticLocalSearch(agent.vehicles(), timeout_plan);
	tasksList = new ArrayList<Task>();
	costCurrentPlan = 0;
	costNewPlan = 0;
    }

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
	if (winner == agent.id()) {
	    currentCity = previous.deliveryCity;
	    tasksList.add(previous);
	    costCurrentPlan = costNewPlan;
	}

	System.out.println("--- Previous ---");
	System.out.println("task: " + previous);
	System.out.println("winner: " + winner);
	System.out.print("bids: ");
	for (long bid : bids) {
	    System.out.print(bid + " ");
	}
	System.out.println();
	System.out.println("----------------");
    }

    @Override
    public Long askPrice(Task task) {

	// do not bid if task bigger than largest vehicle capacity (or bid in
	// order to fool adversary? but problem if win task...)

	// Vehicle largestVehicle = getLargestVehicle()
	// ...
	if (vehicle.capacity() < task.weight)
	    return null;

	// compute your stuff here: cost of current plan, cost of next plan,
	// bid...

	// TODO: maybe do not recompute whole SLS every time?
	List<Task> tasksList_tmp = new ArrayList<Task>(tasksList);
	tasksList_tmp.add(task);
	costNewPlan = SLS.createPlan(tasksList_tmp).cost();

	double marginalCost = costNewPlan - costCurrentPlan;

	long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
	long distanceSum = distanceTask
		+ currentCity.distanceUnitsTo(task.pickupCity);
	// double marginalCost = Measures.unitsToKM(distanceSum *
	// vehicle.costPerKm());

	// 1) bid below marginalCost only if winning the task disadvantages the
	// adversary (keep track of [estimated] adversary profit)
	double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
	double bid = ratio * marginalCost;

	System.out.println("task: " + task);
	System.out.println("bid: " + Math.round(bid));
	System.out.println();

	return (long) Math.round(bid);
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
	long time_start = System.currentTimeMillis();

	// StochasticLocalSearch stochasticLocalSearch = new
	// StochasticLocalSearch( vehicles, tasks, timeout_plan);

	CentralizedPlan centralizedPlan = SLS.createPlan(new ArrayList<Task>(
		tasks));

	// System.out.println("Agent " + agent.id() + " has tasks " + tasks);
	Plan planVehicle1 = naivePlan(vehicle, tasks);
	List<Plan> plans = new ArrayList<Plan>();

	/*
	 * for (Vehicle vehicle : vehicles) { Plan plan = buildPlan(auctionPlan,
	 * vehicle); System.out.println(plan); plans.add(plan); }
	 */

	plans.add(planVehicle1);
	while (plans.size() < vehicles.size())
	    plans.add(Plan.EMPTY);

	long time_end = System.currentTimeMillis();
	long duration = time_end - time_start;
	System.out.println("The plan was generated in " + duration
		+ " milliseconds.");
	System.out.println("Plan cost: " + centralizedPlan.cost());

	return plans;
    }

    public static Plan buildPlan(CentralizedPlan centralizedPlan,
	    Vehicle vehicle) {
	City currentCity = vehicle.getCurrentCity();
	Plan plan = new Plan(currentCity);

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
}
