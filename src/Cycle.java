import java.util.*;


public class Cycle implements Comparable<Cycle> {

	public List<Activity> activities;
	public List<Arc> arcs;
	public int transfers;
	public int length;
	public int delta;

	public Cycle(List<Activity> activities)
	{
		this.activities = activities;

		List<Arc> arcs = new ArrayList<Arc>();
		for (Activity a : activities)
		{
			if (a.type == Activity.Type.TRANSFER)
			{
				arcs.add(a.arc);
			}
		}
		this.arcs = arcs;
		this.transfers = arcs.size();

		{
			Set<Activity> set = new HashSet<Activity>(activities);

			if(set.size() < activities.size())
			{
				System.err.println("Cycle activity duplicate");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getDelta() {
		return delta;
	}

	public void setDelta(int delta) {
		this.delta = delta;
	}

	public List<Arc> getArcs()
	{
		return this.arcs;
	}

	@Override
	public int compareTo(Cycle o)
	{
		int compare = this.delta - o.delta;
		if (compare == 0)
		{
			return this.length - o.length;
		}
			
		return compare;
	}

	@Override
	public String toString() {
		return "Cycle [activities=" + activities + ", length=" + length + ", delta=" + delta + "]";
	}


}
