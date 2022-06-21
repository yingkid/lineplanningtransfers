
public class Edge {

	public final int id;
	public final Stop leftStop;
	public final Stop rightStop;
	public final double length;
	public final int minTraveltime;
	public final int maxTraveltime;
	public int load;
	public int minFreq;
	public int maxFreq;
	
	public Edge(int id, Stop leftStop, Stop rightStop, double length, int minTraveltime, int maxTraveltime) {
		this.id = id;
		this.leftStop = leftStop;
		this.rightStop = rightStop;
		this.length = length;
		this.minTraveltime = minTraveltime;
		this.maxTraveltime = maxTraveltime;
	}

	@Override
	public String toString() {
		return String.format("Edge [id=%2d S%-2d-S%-2d length=%3d minTravelTime=%3d maxTravelTime=%3d]", id, leftStop.id, rightStop.id, (int) length, minTraveltime, maxTraveltime);
	}
	
	

}
