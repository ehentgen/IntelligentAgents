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
public class NaiveTemplate implements AuctionBehavior {

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

	int adversaryId = 1 - agent.id();

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

	if (SLS.getLargestVehicle().capacity() < task.weight)
	    return null;

	// TODO: maybe do not recompute whole SLS every time?
	List<Task> tasksList_tmp = new ArrayList<Task>(tasksList);
	tasksList_tmp.add(task);
	costNewPlan = SLS.createPlan(tasksList_tmp).cost();

	// SLS may have have found a better new plan
	double marginalCost = Math.max(costNewPlan - costCurrentPlan, 0);

	double minimumBid = 100;
	if (task.id <= 0) {
	    minimumBid = 0;
	}

	double risky_percentage = 0.02;

	double bid = marginalCost + minimumBid + risky_percentage
		* tasksList.size();

	System.out.println("task: " + task);
	System.out.println("bid: " + Math.round(bid));
	System.out.println();

	return (long) Math.round(bid);
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
	long time_start = System.currentTimeMillis();

	CentralizedPlan centralizedPlan = SLS.createPlan(new ArrayList<Task>(
		tasks));

	List<Plan> plans = new ArrayList<Plan>();

	for (Vehicle vehicle : vehicles) {
	    Plan plan = buildPlan(centralizedPlan, vehicle);
	    System.out.println(plan);
	    plans.add(plan);
	}

	while (plans.size() < vehicles.size())
	    plans.add(Plan.EMPTY);

	long time_end = System.currentTimeMillis();
	long duration = time_end - time_start;
	System.out.println("The plan was generated in " + duration
		+ " milliseconds.");
	System.out.println("Plan cost: " + centralizedPlan.cost());
	System.out.println("Reward:"
		+ (tasks.rewardSum() - centralizedPlan.cost()));
	System.out.println("#tasks:" + tasks.size());

	return plans;
    }

    public static Plan buildPlan(CentralizedPlan centralizedPlan,
	    Vehicle vehicle) {
	City currentCity = vehicle.getCurrentCity();
	Plan plan = new Plan(currentCity);

	TaskAction taskAction = centralizedPlan.vehicleToFirstTaskAction().get(
		vehicle);

	while (taskAction != null) {
	    Task task = taskAction.task();

	    if (taskAction.status() == TaskAction.PICK_UP) {
		for (City city : currentCity.pathTo(task.pickupCity)) {
		    plan.appendMove(city);
		}
		plan.appendPickup(task);
		currentCity = task.pickupCity;
	    } else if (taskAction.status() == TaskAction.DELIVERY) {
		for (City city : currentCity.pathTo(task.deliveryCity)) {
		    plan.appendMove(city);
		}
		plan.appendDelivery(task);
		currentCity = task.deliveryCity;
	    }

	    taskAction = centralizedPlan.taskActionToTaskAction().get(
		    taskAction);
	}
	return plan;
    }

}
