package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    private final int COUNTDOWN = 10;
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
	    CentralizedPlan previousPlan = plan;
	    List<CentralizedPlan> neighbourPlans = chooseNeighbours(previousPlan);
	    plan = localChoice(neighbourPlans, previousPlan, probability);
	    minimumCostSoFar = plan.cost();

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
	    }
	    if (countDown == 0) {
		noImprovement = true;
		stochasticLocalSearchStopCause = "No improvement after "
			+ COUNTDOWN + " steps.";
	    }
	}

	return plan;
    }

    private CentralizedPlan localChoice(List<CentralizedPlan> plans,
	    CentralizedPlan previousPlan, double probability) {

	if (plans.isEmpty()) {
	    return previousPlan;
	}

	List<CentralizedPlan> minimumCostPlans = getMinimumCostPlans(plans);
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

	Map<Vehicle, Task> vehicleToFirstTask = new HashMap<Vehicle, Task>();
	Map<Task, Task> taskToTask = new HashMap<Task, Task>();
	Map<Task, Integer> time = new HashMap<Task, Integer>();
	Map<Task, Vehicle> taskToVehicle = new HashMap<Task, Vehicle>();

	// give the first task to the largest vehicle
	Task firstTask = tasksList.getFirst();
	vehicleToFirstTask.put(largestVehicle, firstTask);
	taskToVehicle.put(firstTask, largestVehicle);
	time.put(firstTask, 1);

	// do not give any task to the other vehicles
	for (int i = 0; i < numberOfVehicles; ++i) {
	    Vehicle vehicle = vehicles.get(i);
	    if (vehicle != largestVehicle) {
		vehicleToFirstTask.put(vehicle, null);
	    }
	}

	// give the remaining tasks to the largest vehicle
	int i = 2;
	while (!tasksList.isEmpty()) {
	    Task task = tasksList.removeFirst();

	    if (!tasksList.isEmpty()) {
		taskToTask.put(task, tasksList.getFirst());
	    } else {
		taskToTask.put(task, null);
	    }
	    taskToVehicle.put(task, largestVehicle);
	    time.put(task, i++);
	}

	CentralizedPlan plan = new CentralizedPlan(vehicleToFirstTask,
		taskToTask, time, taskToVehicle);

	return plan;
    }

    private List<CentralizedPlan> chooseNeighbours(CentralizedPlan previousPlan) {

	List<CentralizedPlan> neighbourPlans = new ArrayList<CentralizedPlan>();

	Vehicle thisVehicle = selectRandomVehicle(previousPlan);
	if (thisVehicle == null) {
	    System.err
		    .println("Error: no vehicle selected. No vehicle has any task assigned to it. Returning empty neighbour plans list.");
	    return neighbourPlans;
	}

	// applying the 'change first task between vehicles' operator
	for (Vehicle thatVehicle : vehicles) {
	    if (!thisVehicle.equals(thatVehicle)) {
		Task task = previousPlan.vehicleToFirstTask().get(thisVehicle);

		if (task != null
			&& getCurrentLoad(thatVehicle) + task.weight <= thatVehicle
				.capacity()) {
		    CentralizedPlan neighbourPlan = changeFirstTaskBetweenVehicles(
			    previousPlan, thisVehicle, thatVehicle);
		    neighbourPlans.add(neighbourPlan);
		}
	    }
	}

	// applying the 'change task order' operator
	int length = 0;
	Task task = previousPlan.vehicleToFirstTask().get(thisVehicle);

	do {
	    task = previousPlan.taskToTask().get(task);
	    length += 1;
	} while (task != null);

	if (length >= 2) {
	    for (int taskIndex1 = 1; taskIndex1 < length; ++taskIndex1) {
		for (int taskIndex2 = taskIndex1 + 1; taskIndex2 <= length; ++taskIndex2) {
		    CentralizedPlan neighbourPlan = changeTaskOrder(
			    previousPlan, thisVehicle, taskIndex1, taskIndex2);
		    neighbourPlans.add(neighbourPlan);
		}
	    }
	}

	return neighbourPlans;
    }

    private CentralizedPlan changeFirstTaskBetweenVehicles(
	    CentralizedPlan plan, Vehicle v1, Vehicle v2) {

	CentralizedPlan neighbourPlan = new CentralizedPlan(plan);
	Task task = plan.vehicleToFirstTask().get(v1);

	neighbourPlan.setNextTask(v1, neighbourPlan.taskToTask().get(task));
	neighbourPlan.setNextTask(task,
		neighbourPlan.vehicleToFirstTask().get(v2));
	neighbourPlan.setNextTask(v2, task);

	neighbourPlan.updateTime(v1);
	neighbourPlan.updateTime(v2);
	neighbourPlan.setVehicle(task, v2);

	return neighbourPlan;
    }

    private CentralizedPlan changeTaskOrder(CentralizedPlan plan,
	    Vehicle vehicle, int taskIndex_1, int taskIndex_2) {

	CentralizedPlan neighbourPlan = new CentralizedPlan(plan);

	// retrieving the first task to exchange
	Task previousTask_1 = null;
	Task task_1 = neighbourPlan.vehicleToFirstTask().get(vehicle);
	int count = 1;

	while (count < taskIndex_1) {
	    previousTask_1 = task_1;
	    task_1 = neighbourPlan.taskToTask().get(task_1);
	    ++count;
	}

	// retrieving the second task to exchange
	Task nextTask_1 = neighbourPlan.taskToTask().get(task_1);
	Task previousTask_2 = task_1;
	Task task_2 = neighbourPlan.taskToTask().get(previousTask_2);
	++count;

	while (count < taskIndex_2) {
	    previousTask_2 = task_2;
	    task_2 = neighbourPlan.taskToTask().get(task_2);
	    ++count;
	}

	Task nextTask_2 = neighbourPlan.taskToTask().get(task_2);

	// exchanging the two tasks
	if (nextTask_1 == task_2) {
	    // the task task_2 is delivered immediately after the task task_1
	    neighbourPlan.setNextTask(previousTask_1, task_2);
	    neighbourPlan.setNextTask(task_2, task_1);
	    neighbourPlan.setNextTask(task_1, previousTask_2);
	} else {
	    neighbourPlan.setNextTask(previousTask_1, task_2);
	    neighbourPlan.setNextTask(previousTask_2, task_1);
	    neighbourPlan.setNextTask(task_2, nextTask_1);
	    neighbourPlan.setNextTask(task_1, nextTask_2);
	}
	neighbourPlan.updateTime(vehicle);

	return neighbourPlan;
    }

    private Vehicle selectRandomVehicle(CentralizedPlan plan) {
	Random random = new Random();
	int numberOfVehicles = vehicles.size();
	int count = 0;
	Vehicle vehicle;

	do {
	    int r = random.nextInt(numberOfVehicles);
	    vehicle = vehicles.get(r);
	    ++count;
	} while (plan.vehicleToFirstTask().get(vehicle) == null
		&& count < numberOfVehicles);
	// break if no vehicle has a first task assigned to it

	return vehicle;
    }

    private double getCurrentLoad(Vehicle vehicle) {
	TaskSet carriedTasks = vehicle.getCurrentTasks();
	double load = 0;

	for (Task task : carriedTasks) {
	    load += task.weight;
	}

	return load;
    }

    private List<CentralizedPlan> getMinimumCostPlans(
	    List<CentralizedPlan> plans) {

	double minimumCost = Double.MAX_VALUE;
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

    private boolean checkConstraints() {
	boolean constraintsRespected = false;
	// TODO
	return constraintsRespected;
    }

    public String stochasticLocalSearchStopCause() {
	return stochasticLocalSearchStopCause;
    }

}
