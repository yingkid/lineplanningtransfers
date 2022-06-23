import java.util.*;

public class Line {
	public int id;
	public List<Edge> edges; 			//not in line order
	public List<Boolean> direction;		//TRUE: left to right, FALSE: right to left
	public List<Arc> forwardArcs;
	public List<Arc> backwardArcs;
	public List<Stop> stops;			//not in line order
	public double length;
	public double capacity = Settings.LINECAPACITY;
	public double costs;
	public int minFreq;
	public int maxFreq;
	
	public void finish()
	{
		List<Stop> stops = new ArrayList<>();
		Stop leftStop = edges.get(0).leftStop;
		Stop rightStop = edges.get(0).rightStop;
		stops.add(leftStop);
		stops.add(rightStop);
		for (int i = 1; i < edges.size(); i++)
		{
			Stop lastStop = stops.get(stops.size() - 1);
			if (lastStop == edges.get(i).leftStop)
			{
				Stop next = edges.get(i).rightStop;
				stops.add(next);
			}
			else if (lastStop == edges.get(i).rightStop)
			{
				Stop next = edges.get(i).leftStop;
				stops.add(next);
			}
		}
		
		this.stops = stops;
	}
	

	
	
	public Line(Line other, List<Edge> edges, List<Stop> stops)
	{
		this.id = other.id;
		this.length = other.length;
		this.capacity = other.capacity;
		this.costs = other.costs;
		this.minFreq = other.minFreq;
		this.maxFreq = other.maxFreq;
		
		this.edges = edges;
		this.stops = stops;
	}
	
	
	public Line(int id)
	{
		this.id = id;
		this.edges = new ArrayList<Edge>();
		this.direction = new ArrayList<Boolean>();
	}
	
	public void addStops(List<Stop> stops)
	{
		this.stops = stops;
	}
	
	public void addEdge(Instance instance)
	{
		for (int i = 0, j = 1; j < stops.size(); i++, j++)
		{
			Stop s1 = stops.get(i);
			Stop s2 = stops.get(j);
			System.out.println(s1 + " " + s2);
			Edge e = instance.findEdge(s1, s2);
			this.edges.add(e);
			if (e.leftStop == s1 && e.rightStop == s2) 
			{
				this.direction.add(true);
			}
			else if (e.leftStop == s2 && e.rightStop == s1)
			{
				this.direction.add(false);
			}
			else
			{
				System.err.println("Error addEdge");
			}
		}
	}
	
	public void addEdge(Edge e)
	{
		this.edges.add(e);
	}
	
	public void setFrequencies()
	{
		int minFreq = Integer.MAX_VALUE;
		int maxFreq = Integer.MIN_VALUE;
		for (Edge e : edges)
		{
			if (e.minFreq < minFreq)
			{
				minFreq = e.minFreq;
			}
			if (e.maxFreq > maxFreq)
			{
				maxFreq = e.maxFreq;
			}
		}
		this.minFreq = minFreq;
		this.maxFreq = maxFreq;
		System.out.println(this + " minFreq=" + this.minFreq + " maxFreq=" + this.maxFreq);
	}
	
	@Override
	public String toString() {
		return String.format("L%-3d(#stops=%2d)", id, stops.size());
	}
	
	public String shortString()
	{
		return String.format("L%-2d", this.id);
	}
	
	public String stopsString()
	{
		List<String> str = new ArrayList<String>();
		for (Stop s : stops)
		{
			str.add(s.id + "");
		}
		return String.join(",", str);
	}
	
	public String stopsNameString()
	{
		List<String> str = new ArrayList<String>();
		for (Stop s : stops)
		{
			str.add(s.shortName + "");
		}
		return String.join(",", str);
	}
	
	
}
