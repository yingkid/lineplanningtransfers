import java.util.*;

public class Generator
{
	private Random rand = Settings.random;
	private char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toUpperCase().toCharArray();
	private Instance instance;
	private List<Stop> stops;
	private List<Line> lines;
	private List<OD> pairs;
	
	public int[][] distances;
	public String name;
	
	public Generator(String name, List<int[]> linesWithStops)
	{
		int nStops = 0;
		for (int[] i : linesWithStops)
		{
			int max = Arrays.stream(i).max().getAsInt() + 1;
			if (max> nStops)
			{
				nStops = max;
			}		
		}
		this.name = name;
		this.instance = new Instance(this);
		this.stops = generateStops(nStops);
		this.lines = new ArrayList<Line>();
		this.distances = generateDistances();
		
		for (int[] line : linesWithStops)
		{
			for (int i = 0, j = 1; j < line.length; i++, j++)
			{
				Stop s1 = stops.get(line[i]);
				Stop s2 = stops.get(line[j]);
				Edge e = instance.findEdge(s1, s2);
				if (e == null)
				{
					Edge newE = instance.createEdge(s1, s2, distances[i][j]);
				}
			}
		}
		
		
		for (int i = 0; i < linesWithStops.size(); i++)
		{
			
			Line l = new Line(i);
			List<Stop> stops = new ArrayList<Stop>();
			int[] array = linesWithStops.get(i);
			for (int j = 0; j < array.length; j++)
			{
				stops.add(this.stops.get(array[j]));
			}
			l.addStops(stops);
			l.addEdge(instance);
			lines.add(l);
			l.minFreq = 1;
			l.maxFreq = 4;
			l.costs = 1000;
		}
		this.pairs = createODpairs();

		this.instance.setLines(lines);
		this.instance.setStops(stops);
		this.instance.setPairs(pairs);
		this.instance.createVerticesAndArcs();
		
	}
	
	public List<Stop> generateStops(int nStops)
	{
		List<Stop> stops = new ArrayList<Stop>();
		for (int i = 0; i < nStops; i++)
		{
			String stopName = String.valueOf(alphabet[i]);
			Stop s = new Stop(i, stopName, stopName, 0.0, 0.0);
			stops.add(s);
		}
		return stops;
	}
	
	public int[][] generateDistances()
	{
		
		int size = stops.size();
		int[][] distances = new int[size][size];
		for (int i = 0; i < size; i++)
		{
			for (int j = 0; j < size; j++)
			{
				distances[i][j] = 1 + rand.nextInt(30);
			}
		}
		return distances;
	}
	
	public List<OD> createODpairs()
	{
		List<OD> ods = new ArrayList<OD>();
		//ods.add(new OD(stops.get(0), stops.get(2), 15));
		//ods.add(new OD(stops.get(4), stops.get(0), 15));
		int nrStops = stops.size();
		for (int i = 0; i < nrStops; i++)
		{
			for (int j = i; j < nrStops; j++)
			{
				Stop s1 = stops.get(i);
				Stop s2 = stops.get(j);
				if (s1 == s2) continue;
				int demand = 10 + 10*rand.nextInt(10);
				OD od1 = new OD(s1, s2, demand);
				OD od2 = new OD(s2, s1, demand);
				ods.add(od1);
				ods.add(od2);
			}

		}
		return ods;
	}
	
	public Edge createEdge(Stop s1, Stop s2)
	{
		int size = this.instance.getEdges().size();
		int distance = distances[stops.indexOf(s1)][stops.indexOf(s2)];
		Edge e = new Edge(size, s1, s2, distance, distance, distance);
		this.instance.getEdges().add(e);
		return e;
	}
	
	public Instance getInstance() {
		return instance;
	}
	
}