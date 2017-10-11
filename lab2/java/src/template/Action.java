package template;

import logist.topology.Topology.City;

public class Action {

	private boolean isPickUpTask;
	private City cityFrom;
	private City cityTo;

	/**
	 * 
	 * @param pickUpTask_
	 *            : whether a task is accepted during this action
	 * @param cityFrom_
	 * @param cityTo_
	 */
	public Action(boolean pickUpTask_, City cityFrom_, City cityTo_) {
		this.isPickUpTask = pickUpTask_;
		this.cityFrom = cityFrom_;
		this.cityTo = cityTo_;
	}

	/*
	 * ========================== ==== Getters & others ====
	 * ==========================
	 */
	public City cityFrom() {
		return cityFrom;
	}

	public City cityTo() {
		return cityTo;
	}

	public boolean isPickUpTask() {
		return isPickUpTask;
	}

	// Eclipse generated hashCode() method
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cityFrom == null) ? 0 : cityFrom.hashCode());
		result = prime * result + ((cityTo == null) ? 0 : cityTo.hashCode());
		result = prime * result + (isPickUpTask ? 1231 : 1237);
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
		Action other = (Action) obj;
		if (cityFrom == null) {
			if (other.cityFrom != null)
				return false;
		} else if (!cityFrom.equals(other.cityFrom))
			return false;
		if (cityTo == null) {
			if (other.cityTo != null)
				return false;
		} else if (!cityTo.equals(other.cityTo))
			return false;
		if (isPickUpTask != other.isPickUpTask)
			return false;
		return true;
	}

}