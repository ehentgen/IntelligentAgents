package template;

import java.util.ArrayList;
import java.util.List;

import logist.topology.Topology.City;

public class State {

	private City currentCity;
	private City destinationCity;

	private List<Action> actions;

	public State(City currentCity_, City destinationCity_) {
		this.currentCity = currentCity_;
		this.destinationCity = destinationCity_;

		actions = new ArrayList<Action>();
		createActions();
	}

	/**
	 * This method creates all the possible actions for a given state and store them. <br><br>
	 * There are 3 cases : (implemented in this order)<br>
	 * -- Task not available <br>
	 * -- Task available, accepted <br>
	 * -- Task available, refused <br>
	 */
	private void createActions() {
		List<City> neighbourCities = currentCity.neighbors();

		if (!hasAvailableTask()) {
			// there is no available task in this city
			for (City neighbourCity : neighbourCities) {
				actions.add(new Action(false, currentCity, neighbourCity));
			}
		} else {
			// the agent picks up the available task
			actions.add(new Action(true, currentCity, destinationCity));
			// or the agent does not pick up the available task and moves to a
			// neighboring city
			for (City neighbourCity : neighbourCities) {
				actions.add(new Action(false, currentCity, neighbourCity));
			}
		}
	}

	
	/*
	 * ==========================
	 * ==== Getters & others ====
	 * ==========================
	 */
	public List<Action> getActions() {
		return actions;
	}

	public City currentCity() {
		return currentCity;
	}

	public City destinationCity() {
		return destinationCity;
	}

	public boolean hasAvailableTask() {
		return (destinationCity != null);
	}

	// Eclipse generated hashCode() method
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actions == null) ? 0 : actions.hashCode());
		result = prime * result + ((currentCity == null) ? 0 : currentCity.hashCode());
		result = prime * result + ((destinationCity == null) ? 0 : destinationCity.hashCode());
		return result;
	}

	// Eclipse generated equals() method
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		State other = (State) obj;
		if (actions == null) {
			if (other.actions != null)
				return false;
		} else if (!actions.equals(other.actions))
			return false;
		if (currentCity == null) {
			if (other.currentCity != null)
				return false;
		} else if (!currentCity.equals(other.currentCity))
			return false;
		if (destinationCity == null) {
			if (other.destinationCity != null)
				return false;
		} else if (!destinationCity.equals(other.destinationCity))
			return false;
		return true;
	}
}