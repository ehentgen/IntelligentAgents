package templateJT;

import logist.topology.Topology.City;

public class EdgeCity {

	public City from;
	public City to;
	public double distance;


	public EdgeCity(City from, City to, double distance) {
		this.from = from;
		this.to = to;
		this.distance = distance;
	}

	@Override
	public int hashCode() {
		return (int) (from.hashCode() + to.hashCode() + distance);
	}

	@Override
	public boolean equals(Object arg0) {

		EdgeCity that = (EdgeCity) arg0;

		return (that.to == to && that.from == from) || (that.to == from && that.from == to);

	}
}
