package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

public class StochasticLocalSearch {

    private final List<Vehicle> vehicles;
    private final TaskSet tasks;
    private final double probability;
    private final long timeout;

    // maximum number of steps with no improvement before the stochastic local
    // search algorithm stops
    private final int COUNTDOWN = 1000; // TODO: optimal value?
    private String stochasticLocalSearchStopCause = "";

    public StochasticLocalSearch(List<Vehicle> vehicles, TaskSet tasks,
	    double probability, long timeout) {

	this.vehicles = vehicles;
	this.tasks = tasks;
	this.probability = probability;
	this.timeout = timeout;
    }

    public CentralizedPlan createPlan() {
	long time_start = System.currentTimeMillis();

	CentralizedPlan plan = selectInitialSolution(tasks);

	boolean timedOut = false;
	boolean noImprovement = false;
	int countDown = COUNTDOWN;
	double minimumCostSoFar = Double.MAX_VALUE;

	while (!timedOut && !noImprovement) {
	    // [perform a clone(), because otherwise, weird stuff happens]
	    CentralizedPlan previousPlan = plan.clone();

	    Set<CentralizedPlan> neighbourPlans = chooseNeighbours(previousPlan);
	    plan = localChoice(neighbourPlans, previousPlan, probability);

	    // check if the search will be stopped because of timeout
	    long time = System.currentTimeMillis();
	    long duration = time - time_start;
	    if (duration >= timeout) {
		timedOut = true;
		stochasticLocalSearchStopCause = "Timeout reached: " + timeout
			+ " ms.";
	    }

	    // check if the search will be stopped because there was no
	    // improvement for some number of steps
	    if (plan.cost() >= minimumCostSoFar) {
		--countDown;
	    } else {
		countDown = COUNTDOWN;
		minimumCostSoFar = plan.cost();
	    }
	    if (countDown == 0) {
		noImprovement = true;
		stochasticLocalSearchStopCause = "No improvement after "
			+ COUNTDOWN + " steps.";
	    }
	}
	return plan;
    }

    private CentralizedPlan localChoice(Set<CentralizedPlan> plans,
	    CentralizedPlan previousPlan, double probability) {

	List<CentralizedPlan> plansList = new ArrayList<CentralizedPlan>(plans);

	if (plans.isEmpty()) {
	    return previousPlan;
	}

	List<CentralizedPlan> minimumCostPlans = getMinimumCostPlans(plansList,
		previousPlan);
	CentralizedPlan minimumCostPlan;

	Random random = new Random();

	if (minimumCostPlans.size() == 1) {
	    minimumCostPlan = minimumCostPlans.get(0);
	} else {
	    int r = random.nextInt(minimumCostPlans.size());
	    minimumCostPlan = minimumCostPlans.get(r);
	}

	double p = random.nextDouble();
	if (p > probability) {
	    return minimumCostPlan;
	} else {
	    return previousPlan;
	}
    }

    // initial solution where each vehicle is assigned some set of task [not
    // completed]
    private CentralizedPlan selectInitialSolution_2(TaskSet tasks) {
	LinkedList<Task> tasksList = new LinkedList<Task>(tasks);

	int numberOfVehicles = vehicles.size();
	int i = 0;

	Map<Vehicle, TaskAction> vehicleToFirstTaskAction = new HashMap<Vehicle, TaskAction>();
	Map<TaskAction, TaskAction> taskActionToTaskAction = new HashMap<TaskAction, TaskAction>();

	for (Task task : tasksList) {
	    TaskAction taskAction_pickup = new TaskAction(task,
		    TaskAction.PICK_UP);
	    TaskAction taskAction_delivery = new TaskAction(task,
		    TaskAction.DELIVERY);

	    taskActionToTaskAction.put(taskAction_delivery,
		    vehicleToFirstTaskAction.get(vehicles.get(i)));
	    vehicleToFirstTaskAction.put(vehicles.get(i), taskAction_pickup);
	    taskActionToTaskAction.put(taskAction_pickup, taskAction_delivery);

	    i = (i + 1) % numberOfVehicles;
	}

	return new CentralizedPlan(vehicleToFirstTaskAction,
		taskActionToTaskAction);

    }

    // initial solution where all tasks are assigned to the largest vehicle
    private CentralizedPlan selectInitialSolution(TaskSet tasks) {
	// assign all tasks to the vehicle with the largest capacity
	Vehicle largestVehicle = vehicles.get(0);
	int numberOfVehicles = vehicles.size();

	for (int i = 1; i < numberOfVehicles; ++i) {
	    if (vehicles.get(i).capacity() > largestVehicle.capacity()) {
		largestVehicle = vehicles.get(i);
	    }
	}

	// if a task weight exceeds the largest vehicle capacity, then the
	// problem is unsolvable
	LinkedList<Task> tasksList = new LinkedList<Task>(tasks);
	int largestTaskWeight = 0;
	for (int i = 0; i < tasksList.size(); ++i) {
	    Task task = tasksList.get(i);
	    if (task.weight > largestTaskWeight) {
		largestTaskWeight = task.weight;
	    }
	}

	if (largestVehicle.capacity() < largestTaskWeight) {
	    System.err
		    .println("Error: problem is unsolvable. Largest task weight exceeds largest vehicle capacity.");
	    return null;
	}

	Map<Vehicle, TaskAction> vehicleToFirstTaskAction = new HashMap<Vehicle, TaskAction>();
	Map<TaskAction, TaskAction> taskActionToTaskAction = new HashMap<TaskAction, TaskAction>();
	// Map<Task, Integer> time = new HashMap<Task, Integer>();
	// Map<Task, Vehicle> taskToVehicle = new HashMap<Task, Vehicle>();

	// give the first task to the largest vehicle
	Task firstTask = tasksList.removeFirst();
	TaskAction firstTaskaction_pickup = new TaskAction(firstTask,
		TaskAction.PICK_UP);
	TaskAction firstTaskaction_delivery = new TaskAction(firstTask,
		TaskAction.DELIVERY);
	vehicleToFirstTaskAction.put(largestVehicle, firstTaskaction_pickup);
	taskActionToTaskAction.put(firstTaskaction_pickup,
		firstTaskaction_delivery);
	taskActionToTaskAction.put(firstTaskaction_delivery, new TaskAction(
		tasksList.getFirst(), TaskAction.PICK_UP));
	// taskToVehicle.put(firstTask, largestVehicle);
	// time.put(firstTask, 1);

	// do not give any task to the other vehicles
	for (int i = 0; i < numberOfVehicles; ++i) {
	    Vehicle vehicle = vehicles.get(i);
	    if (vehicle != largestVehicle) {
		vehicleToFirstTaskAction.put(vehicle, null);
	    }
	}

	// give the remaining tasks to the largest vehicle
	// by default, the vehicle will pick up and deliver tasks sequentially
	// int i = 2;
	while (!tasksList.isEmpty()) {
	    Task task = tasksList.removeFirst();

	    taskActionToTaskAction.put(
		    new TaskAction(task, TaskAction.PICK_UP), new TaskAction(
			    task, TaskAction.DELIVERY));

	    if (!tasksList.isEmpty()) {
		Task nextTask = tasksList.getFirst();
		taskActionToTaskAction.put(new TaskAction(task,
			TaskAction.DELIVERY), new TaskAction(nextTask,
			TaskAction.PICK_UP));
	    } else {
		taskActionToTaskAction.put(new TaskAction(task,
			TaskAction.DELIVERY), null);
	    }
	    // taskToVehicle.put(task, largestVehicle);
	    // time.put(task, i++);
	}

	CentralizedPlan plan = new CentralizedPlan(vehicleToFirstTaskAction,
		taskActionToTaskAction);

	return plan;
    }

    private Set<CentralizedPlan> chooseNeighbours(CentralizedPlan previousPlan) {

	Set<CentralizedPlan> neighbourPlans = new HashSet<CentralizedPlan>();

	// select the vehicle on which the transformations will be applied
	Vehicle thisVehicle = selectRandomVehicle(previousPlan);
	if (thisVehicle == null) {
	    System.err
		    .println("Error: no vehicle selected. No vehicle has any task assigned to it. Returning empty neighbour plans list.");
	    return neighbourPlans;
	}

	// applying the 'change first task between vehicles' operator: give the
	// first task in thisVehicle to thatVehicle
	for (Vehicle thatVehicle : vehicles) {
	    if (!thisVehicle.equals(thatVehicle)) {

		// if it exists, the first taskAction will inevitably be a
		// pickup action
		TaskAction taskAction = previousPlan.vehicleToFirstTaskAction()
			.get(thisVehicle);

		if (taskAction != null) {
		    CentralizedPlan neighbourPlan = changeFirstTaskBetweenVehicles(
			    previousPlan, thisVehicle, thatVehicle);
		    // [maybe]
		    // only consider this plan if the constraints are respected
		    // (only the plan for thatVehicle - the 'destination
		    // vehicle' - needs to be checked)
		    if (constraintsRespected(neighbourPlan)) {
			neighbourPlans.add(neighbourPlan);
		    }
		}
	    }
	}

	// applying the 'change task order' operator
	int length = 0;
	TaskAction taskAction = previousPlan.vehicleToFirstTaskAction().get(
		thisVehicle);

	List<TaskAction> taskActionList = new ArrayList<TaskAction>();

	do {
	    taskActionList.add(taskAction);
	    taskAction = previousPlan.taskActionToTaskAction().get(taskAction);
	    length += 1;
	} while (taskAction != null);

	if (length > 2) {
	    for (int taskActionIndex_1 = 0; taskActionIndex_1 < length - 1; ++taskActionIndex_1) {
		TaskAction taskAction_1 = taskActionList.get(taskActionIndex_1);

		for (int taskActionIndex_2 = taskActionIndex_1 + 1; taskActionIndex_2 < length; ++taskActionIndex_2) {
		    TaskAction taskAction_2 = taskActionList
			    .get(taskActionIndex_2);

		    CentralizedPlan neighbourPlan = changeTaskOrder(
			    previousPlan, thisVehicle, taskAction_1,
			    taskAction_2);
		    // [maybe]
		    // only consider this plan if the constraints are respected
		    // (only the plan for thisVehicle needs to be checked)
		    if (constraintsRespected(neighbourPlan)) {
			neighbourPlans.add(neighbourPlan);
		    }
		}
	    }
	}
	return neighbourPlans;
    }

    // [note: checking whether this is a valid transaction is handled
    // afterwards]
    private CentralizedPlan changeFirstTaskBetweenVehicles(
	    CentralizedPlan plan, Vehicle vehicle_1, Vehicle vehicle_2) {

	// [perform a clone(), because otherwise, weird stuff happens]
	CentralizedPlan neighbourPlan = plan.clone();

	// TODO: check if better to compare TaskAction or Task

	// the pickup and delivery for a task must be transferred together to
	// the other vehicle
	TaskAction taskAction_pickup = plan.vehicleToFirstTaskAction().get(
		vehicle_1);

	TaskAction previousTaskAction_delivery = null;
	TaskAction taskAction_delivery = null;
	TaskAction p = plan.taskActionToTaskAction().get(taskAction_pickup);
	while (p != null) {
	    if (p.task().equals(taskAction_pickup.task())) {
		taskAction_delivery = p;
	    }

	    p = plan.taskActionToTaskAction().get(p);
	}

	TaskAction p2 = plan.taskActionToTaskAction().get(taskAction_pickup);
	while (p2 != null) {
	    if (taskAction_delivery.equals(neighbourPlan
		    .taskActionToTaskAction().get(p2))) {
		previousTaskAction_delivery = p2;
	    }
	    p2 = plan.taskActionToTaskAction().get(p2);
	}

	// redefine the mapping of the taskActions for vehicle_1
	if (taskAction_delivery.equals(neighbourPlan.taskActionToTaskAction()
		.get(taskAction_pickup))) {
	    neighbourPlan.setNextTask(vehicle_1, neighbourPlan
		    .taskActionToTaskAction().get(taskAction_delivery));
	} else {
	    neighbourPlan.setNextTask(vehicle_1, neighbourPlan
		    .taskActionToTaskAction().get(taskAction_pickup));

	    neighbourPlan.setNextTask(
		    previousTaskAction_delivery,
		    neighbourPlan.taskActionToTaskAction().get(
			    taskAction_delivery));
	}

	// redefine the mapping of the taskActions for vehicle_2
	// (the order of the re-mapping is important!)
	neighbourPlan.setNextTask(taskAction_delivery, neighbourPlan
		.vehicleToFirstTaskAction().get(vehicle_2));
	neighbourPlan.setNextTask(vehicle_2, taskAction_pickup);
	neighbourPlan.setNextTask(taskAction_pickup, taskAction_delivery);

	// neighbourPlan.updateTime(v1);
	// neighbourPlan.updateTime(v2);
	// neighbourPlan.setVehicle(task, v2);

	return neighbourPlan;
    }

    // [note: checking whether this is a valid transaction is handled
    // afterwards]
    // [note: by design, taskAction_1 will come before taskAction_2]
    private CentralizedPlan changeTaskOrder(CentralizedPlan plan,
	    Vehicle vehicle, TaskAction taskAction_1, TaskAction taskAction_2) {

	// [perform a clone(), because otherwise, weird stuff happens]
	CentralizedPlan neighbourPlan = plan.clone();

	TaskAction previousTaskAction_1 = null;
	TaskAction previousTaskAction_2 = null;

	TaskAction taskAction = neighbourPlan.vehicleToFirstTaskAction().get(
		vehicle);

	// retrieve previousTaskAction_1 and previousTaskAction_2
	while (taskAction != null) {
	    if (taskAction_1.equals(neighbourPlan.taskActionToTaskAction().get(
		    taskAction))) {
		previousTaskAction_1 = new TaskAction(taskAction);
	    }
	    if (taskAction_2.equals(neighbourPlan.taskActionToTaskAction().get(
		    taskAction))) {
		previousTaskAction_2 = new TaskAction(taskAction);
	    }
	    taskAction = neighbourPlan.taskActionToTaskAction().get(taskAction);
	}

	TaskAction nextTaskAction_1 = null;
	if (neighbourPlan.taskActionToTaskAction().get(taskAction_1) != null) {
	    nextTaskAction_1 = new TaskAction(neighbourPlan
		    .taskActionToTaskAction().get(taskAction_1));
	}

	TaskAction nextTaskAction_2 = null;
	if (neighbourPlan.taskActionToTaskAction().get(taskAction_2) != null) {
	    nextTaskAction_2 = new TaskAction(neighbourPlan
		    .taskActionToTaskAction().get(taskAction_2));
	}

	// exchange the two tasks
	if (taskAction_1.equals(plan.vehicleToFirstTaskAction().get(vehicle))) {
	    if (taskAction_2.equals(nextTaskAction_1)) {
		neighbourPlan.setNextTask(vehicle, taskAction_2);
		neighbourPlan.setNextTask(taskAction_2, taskAction_1);
		neighbourPlan.setNextTask(taskAction_1, nextTaskAction_2);
	    } else {
		neighbourPlan.setNextTask(vehicle, taskAction_2);
		neighbourPlan.setNextTask(previousTaskAction_2, taskAction_1);
		neighbourPlan.setNextTask(taskAction_2, nextTaskAction_1);
		neighbourPlan.setNextTask(taskAction_1, nextTaskAction_2);
	    }
	} else {
	    if (taskAction_2.equals(nextTaskAction_1)) {
		neighbourPlan.setNextTask(previousTaskAction_1, taskAction_2);
		neighbourPlan.setNextTask(taskAction_2, taskAction_1);
		neighbourPlan.setNextTask(taskAction_1, nextTaskAction_2);
	    } else {
		neighbourPlan.setNextTask(previousTaskAction_1, taskAction_2);
		neighbourPlan.setNextTask(previousTaskAction_2, taskAction_1);
		neighbourPlan.setNextTask(taskAction_2, nextTaskAction_1);
		neighbourPlan.setNextTask(taskAction_1, nextTaskAction_2);
	    }
	}

	return neighbourPlan;
    }

    /*
     * private CentralizedPlan changeTaskOrder(CentralizedPlan plan, Vehicle
     * vehicle, int taskIndex_1, int taskIndex_2) {
     * 
     * CentralizedPlan neighbourPlan = new CentralizedPlan(plan);
     * 
     * // retrieving the first taskAction to exchange TaskAction
     * previousTaskAction_1 = null; TaskAction taskAction_1 =
     * neighbourPlan.vehicleToFirstTaskAction().get( vehicle); int count = 1;
     * 
     * while (count < taskIndex_1) { previousTaskAction_1 = taskAction_1;
     * taskAction_1 = neighbourPlan.taskActionToTaskAction().get( taskAction_1);
     * ++count; }
     * 
     * // retrieving the second taskAction to exchange TaskAction
     * nextTaskAction_1 = neighbourPlan.taskActionToTaskAction()
     * .get(taskAction_1); TaskAction previousTaskAction_2 = taskAction_1;
     * TaskAction taskAction_2 = neighbourPlan.taskActionToTaskAction().get(
     * previousTaskAction_2); ++count;
     * 
     * while (count < taskIndex_2) { previousTaskAction_2 = taskAction_2;
     * taskAction_2 = neighbourPlan.taskActionToTaskAction().get( taskAction_2);
     * ++count; }
     * 
     * TaskAction nextTaskAction_2 = neighbourPlan.taskActionToTaskAction()
     * .get(taskAction_2);
     * 
     * // exchanging the two tasks if (nextTaskAction_1 == taskAction_2) {
     * neighbourPlan.setNextTask(previousTaskAction_1, taskAction_2);
     * neighbourPlan.setNextTask(taskAction_2, taskAction_1);
     * neighbourPlan.setNextTask(taskAction_1, previousTaskAction_2); } else {
     * neighbourPlan.setNextTask(previousTaskAction_1, taskAction_2);
     * neighbourPlan.setNextTask(previousTaskAction_2, taskAction_1);
     * neighbourPlan.setNextTask(taskAction_2, nextTaskAction_1);
     * neighbourPlan.setNextTask(taskAction_1, nextTaskAction_2); } //
     * neighbourPlan.updateTime(vehicle);
     * 
     * System.out.println(neighbourPlan); return neighbourPlan; }
     */

    private Vehicle selectRandomVehicle(CentralizedPlan plan) {
	Random random = new Random();
	int numberOfVehicles = vehicles.size();
	int count = 0;
	Vehicle vehicle;

	do {
	    int r = random.nextInt(numberOfVehicles);
	    vehicle = vehicles.get(r);
	    ++count;
	} while (plan.vehicleToFirstTaskAction().get(vehicle) == null
		&& count < numberOfVehicles);
	// 'count' is used to break if no vehicle has a first task assigned to
	// it

	return vehicle;
    }

    private List<CentralizedPlan> getMinimumCostPlans(
	    List<CentralizedPlan> plans, CentralizedPlan previousPlan) {

	// TODO: check is difference between choosing Double.MAX_VALUE and
	// previousPlan.cost()

	// double minimumCost = Double.MAX_VALUE;
	double minimumCost = previousPlan.cost();
	List<CentralizedPlan> minimumCostPlans = new ArrayList<CentralizedPlan>();

	for (CentralizedPlan plan : plans) {
	    if (plan.cost() < minimumCost) {
		minimumCost = plan.cost();
		minimumCostPlans.clear();
		minimumCostPlans.add(plan);
	    } else if (plan.cost() == minimumCost) {
		minimumCostPlans.add(plan);
	    }
	}
	return minimumCostPlans;
    }

    public boolean constraintsRespected(CentralizedPlan plan) {
	for (Vehicle vehicle : vehicles) {
	    if (!constraintsRespected(plan, vehicle)) {
		return false;
	    }
	}
	return true;
    }

    public boolean constraintsRespected(CentralizedPlan plan, Vehicle vehicle) {
	return capacityRespected(plan, vehicle)
		&& pickupAndDeliveryOrderRespected(plan, vehicle);
    }

    public boolean capacityRespected(CentralizedPlan plan, Vehicle vehicle) {
	int capacity = vehicle.capacity();
	double load = 0;
	TaskAction taskAction = plan.vehicleToFirstTaskAction().get(vehicle);

	while (taskAction != null) {
	    if (taskAction.status() == TaskAction.PICK_UP) {
		load += taskAction.task().weight;
	    } else if (taskAction.status() == TaskAction.DELIVERY) {
		load -= taskAction.task().weight;
	    }

	    // this plan does not respect the capacity constraint
	    if (load > capacity) {
		return false;
	    }
	    taskAction = plan.taskActionToTaskAction().get(taskAction);
	}
	return true;
    }

    public boolean pickupAndDeliveryOrderRespected(CentralizedPlan plan,
	    Vehicle vehicle) {

	Set<Task> pickedUpTasks = new HashSet<Task>();
	TaskAction taskAction = plan.vehicleToFirstTaskAction().get(vehicle);

	while (taskAction != null) {
	    if (taskAction.status() == TaskAction.PICK_UP) {
		// remember which tasks have been picked up
		pickedUpTasks.add(taskAction.task());
	    } else if (taskAction.status() == TaskAction.DELIVERY
		    && !pickedUpTasks.contains(taskAction.task())) {
		// check whether the task has been picked up beforehand:
		// if not, then the pick up and delivery order constraint is not
		// respected for this plan
		return false;
	    }
	    taskAction = plan.taskActionToTaskAction().get(taskAction);
	}
	return true;
    }

    public String stochasticLocalSearchStopCause() {
	return stochasticLocalSearchStopCause;
    }

}
