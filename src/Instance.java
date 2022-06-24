import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class Instance {
	private LocalDateTime date = LocalDateTime.now();
	public String dateTime = date.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
	public String name = "";
	public String path = "";

	private List<Stop> stops;
	private List<Edge> edges;
	private List<OD> pairs;
	private List<Line> lines;

	private List<Vertex> vertices;
	private List<Arc> arcs;
	public List<Cycle> excludedCycles = new ArrayList<Cycle>();


	public Instance (String name, String path)
	{
		System.out.println("Instance path: " + path);
		this.name = name;
		this.path = path;

		this.stops = readStops(path + "Stop.giv");
		this.edges = readEdges(path + "Edge.giv");
		this.lines = readLines(path + "Pool.giv", path + "Pool-Cost.giv");
		readLoad(path + "Load.giv");
		this.pairs = readODpairs(path + "OD.giv");
		setFrequencies();
		this.vertices = createVertices();
		this.arcs = createArcs();
		
		System.out.println("vertices " + vertices.size());
		System.out.println("arcs " + arcs.size());
	}
	
	public Instance (String name, String path, List<int[]> lines)
	{
		System.out.println("Instance path: " + path);
		this.name = name;
		this.path = path;

		this.stops = readStops(path + "Stop.giv");
		this.edges = readEdges(path + "Edge.giv");
		this.lines = generateLinePool(lines);
		readLoad(path + "Load.giv");
		this.pairs = readODpairs(path + "OD.giv");
		setFrequencies();
		this.vertices = createVertices();
		this.arcs = createArcs();

		System.out.println("vertices " + vertices.size());
		System.out.println("arcs " + arcs.size());
	}

	public Instance(Generator g)
	{
		this.path = g.name;
		this.edges = new ArrayList<Edge>();
	}


	public void setStops(List<Stop> stops) {
		this.stops = stops;
	}

	public void setEdges(List<Edge> edges) {
		this.edges = edges;
	}

	public void setLines(List<Line> lines) {
		this.lines = lines;
	}


	public void setPairs(List<OD> pairs) {
		this.pairs = pairs;
	}

	public void createVerticesAndArcs()
	{
		this.vertices = createVertices();
		this.arcs = createArcs();
	}

	public List<Vertex> getVertices() {
		return vertices;
	}

	public List<Arc> getArcs() {
		return arcs;
	}

	private void setFrequencies()
	{
		for (Line l : lines)
		{
			l.setFrequencies();
		}
	}

	private List<Vertex> createVertices()
	{
		//checks for transfer stations
		List<Vertex> vertices = new ArrayList<Vertex>();
		
		//create in and out vertices for each stop
		for (Stop s : stops)
		{
			vertices.add(s.in);
			vertices.add(s.out);
		}
		
		//create station-line vertices
		for (Line l : lines)
		{
			for (Stop s : l.stops)
			{
				if (!s.lines.containsKey(l))
				{
					Vertex lineVertex = new Vertex(s, Vertex.Type.LINE, l);
					vertices.add(lineVertex);
					s.lines.put(l, lineVertex);
				}
			}
		}
		
		//create platform vertex
		for (Stop s : stops)
		{
			//counting edges of stop
			Set<Edge> edges = new HashSet<Edge>();
			for (Line l : s.lines.keySet())
			{

				for (Edge e : l.edges)
				{
					if (e.leftStop == s || e.rightStop == s)
					{
						edges.add(e);
					}
				}
			}
			//if a station has more than two edges, then it is a transfer station

			if (edges.size() > 2) 
			{
				Vertex platform = new Vertex(s, Vertex.Type.PLAT);
				vertices.add(platform);
				s.addPlatformVertex(platform);
			}
			else if (edges.size() == 2)
			{
				outerloop:
					for (Edge e : edges)
					{
						for (Line l : s.lines.keySet())
						{
							int sizeEdges = l.edges.size();
							int idx = l.edges.indexOf(e);
							if (idx == 0 || idx == sizeEdges - 1)
							{
								Vertex platform = new Vertex(s, Vertex.Type.PLAT);
								vertices.add(platform);
								s.addPlatformVertex(platform);
								break outerloop;
							}
						}
					}
			}
		}
		

		
		return Collections.unmodifiableList(vertices);
	}
	private List<Arc> createArcs()
	{
		List<Arc> arcs = new ArrayList<Arc>();

		//Arcs representing station-line to station-line
		for (Line l : lines)
		{
			l.forwardArcs = new ArrayList<Arc>();
			l.backwardArcs = new ArrayList<Arc>();
			//Check arc are connecting

			for (int i = 0, j = 1; j < l.stops.size(); i++, j++)
			{
				Stop s1 = l.stops.get(i);
				Stop s2 = l.stops.get(j);
				Edge e = this.findEdge(s1, s2);

				Vertex left = this.findVertex(s1, l);
				Vertex right = this.findVertex(s2, l);
				Arc leftToRight = new Arc(left, right, Arc.Type.TRAVEL, e.maxTraveltime, l);
				Arc rightToLeft = new Arc(right, left, Arc.Type.TRAVEL, e.maxTraveltime, l);
				arcs.add(leftToRight);
				arcs.add(rightToLeft);
				//arcs.add(leftToRight);
				//arcs.add(rightToLeft);

				l.forwardArcs.add(leftToRight);
				l.backwardArcs.add(rightToLeft);


			}
			Collections.reverse(l.backwardArcs);
		}
		for (Stop s : stops)
		{
			for (Line l : s.lines.keySet())
			{
				//Source and sink arcs for each station
				Vertex stationLine = s.lines.get(l);
				Arc sourceArc = new Arc(s.in, stationLine, Arc.Type.IN, 0);
				Arc sinkArc = new Arc(stationLine, s.out, Arc.Type.OUT, 0);
				arcs.add(sourceArc);
				arcs.add(sinkArc);

				//Platform vertex for each station
				if (s.platform != null)
				{
					Arc stationLineToPlatform = new Arc(stationLine, s.platform, Arc.Type.TOPLAT, 0);
					arcs.add(stationLineToPlatform);

					for (int i = l.minFreq; i <= l.maxFreq; i++)
					{
						if (i == 0) continue;
						Arc platformToStationLine =  new Arc(s.platform, stationLine, Arc.Type.FRPLAT, Math.round(60/i) * Settings.LONGTRANSFERCOSTSFACTOR, i);
						arcs.add(platformToStationLine);
					}
				}

				//Short transfer for each station
				if (Settings.SHORTTRANSFERS)
				{
					if (s.platform != null) //if a real transfer station
					{
						for (Line other : s.lines.keySet())
						{
							Vertex otherStationLine = s.lines.get(other);
							if (l != other)
							{
								Arc stationLineToOtherStationLine = new Arc(stationLine, otherStationLine, Arc.Type.TRANSF, Settings.FASTTRANSFERTIME);
								arcs.add(stationLineToOtherStationLine);
							}
						}
					}
				}


			}

		}

		return arcs;
	}

	private List<Edge> readEdges(String path)
	{
		System.out.println("readEdges: " + path);
		List<Edge> edges = new ArrayList<Edge>();
		List<String> input = readFile(path);
		for (String str : input)
		{
			String[] sArray = str.split(";");
			Stop left = findStop(Integer.parseInt(sArray[1].trim()));
			Stop right = findStop(Integer.parseInt(sArray[2].trim()));
			Edge e = new Edge(
					Integer.parseInt(sArray[0]),
					left,
					right,
					Double.parseDouble(sArray[3].trim()),
					Integer.parseInt(sArray[4].trim()),
					Integer.parseInt(sArray[5].trim()));
			edges.add(e);
		}
		return Collections.unmodifiableList(edges);
	}

	private void readLoad(String path)
	{
		System.out.println("readLoad: " + path);
		List<String> input = readFile(path);
		for (String str : input)
		{
			String[] sArray = str.split(";");
			Edge edge = findEdge(Integer.parseInt(sArray[0].trim()));
			edge.load = Integer.parseInt(sArray[1].trim());
			edge.minFreq = Integer.parseInt(sArray[2].trim());
			edge.maxFreq = Integer.parseInt(sArray[3].trim());
		}
	}

	public List<Stop> readStops(String path)
	{
		System.out.println("readStops: " + path);
		List<Stop> stops = new ArrayList<Stop>();
		List<String> input = readFile(path);
		for (String str : input)
		{
			String[] sArray = str.split(";");
			Stop s = new Stop(
					Integer.parseInt(sArray[0]),
					sArray[1],
					sArray[2],
					Double.parseDouble(sArray[3]),
					Double.parseDouble(sArray[4]));
			stops.add(s);
		}
		return Collections.unmodifiableList(stops);
	}

	public List<OD> readODpairs(String path)
	{
		System.out.println("readODpairs: " + path);
		List<OD> pairs = new ArrayList<OD>();
		List<String> input = readFile(path);
		for (String str : input)
		{
			String[] sArray = str.split(";");
			Stop o = findStop(Integer.parseInt(sArray[0].trim()));
			Stop d = findStop(Integer.parseInt(sArray[1].trim()));
			OD pair = new OD(o, d, Integer.parseInt(sArray[2].trim()));
			pairs.add(pair);
		}
		return Collections.unmodifiableList(pairs);
	}

	public List<Line> readLines(String path, String pathCosts)
	{
		System.out.println("readLines: " + path);
		List<Line> lines = new ArrayList<Line>();
		List<String> input;

		try
		{
			input = readFile(path);
			int id = Integer.parseInt(input.get(0).split(";")[0]); //Pool.giv starts with 1
			Line line = new Line(id);
			for (String str : input)
			{
				String[] sArray = str.split(";");
				int lineId = Integer.parseInt(sArray[0].trim());

				if (id != lineId)
				{
					id = lineId;
					lines.add(line);
					line.finish();
					line = new Line(lineId);
				}
				Edge e = findEdge(Integer.parseInt(sArray[2].trim()));
				line.addEdge(e);
			}
			lines.add(line);
			line.finish();
		}
		catch (Exception e)
		{
			System.err.println("no lines read");
		}
		//Read line costs
		try
		{
			input = readFile(pathCosts);

			for (String str : input)
			{
				String[] sArray = str.split(";");
				int lineId = Integer.parseInt(sArray[0].trim());
				Line line = findLine(lines, lineId);
				if (line == null) continue;
				line.costs = Double.parseDouble(sArray[2].trim());				
			}
		}
		catch (Exception e)
		{
			System.err.println("no pool costs");
			for (Line l : lines)
			{
				l.costs = 1000;
			}
		}

		return Collections.unmodifiableList(lines);

	}

	private List<String> readFile(String path) 
	{
		try 
		{
			List<String> input = Files.readAllLines(Paths.get(path));
			input.removeIf(s -> s.startsWith("#"));
			return input;
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return null;
		}
	}

	private Line findLine(List<Line> lines, int id)
	{
		for (Line l : lines)
		{
			if (l.id == id) return l;
		}
		System.err.println("cannot find line " + id);
		return null;
	}

	private Stop findStop(int id)
	{
		for (Stop s : stops)
		{
			if (s.id == id) return s;
		}
		System.err.println("cannot find " + id);
		return null;
	}
	
	private Stop findStopByShortString(int id)
	{
		for (Stop s : stops)
		{
			if (s.shortName.equals(Integer.toString(id))) return s;
		}
		System.err.println("cannot find " + id);
		return null;
	}

	private Edge findEdge(int id)
	{
		for (Edge e : edges)
		{
			if (e.id == id) return e;
		}
		System.err.println("cannot find " + id);
		return null;
	}

	private Vertex findVertex(Stop stop, Line line)
	{
		System.out.println("findVertex" + stop + "" + line);
		for (Vertex v : vertices)
		{
			if (v.line == line && v.stop == stop)
			{
				return v;
			}
		}
		return null;
	}

	public void print()
	{
		System.out.println(Settings.values());
		System.out.println("stops (" + stops.size() + ")=");
		for (int i = 0; i < stops.size(); i++)
		{
			System.out.printf("%2d. %-20s\n", i, stops.get(i));
		}
		System.out.println("edges (" + edges.size() + ")=");
		for (Edge e : edges)
		{
			System.out.println("\t" + e);
		}
		System.out.println("pairs (" + pairs.size() + ")=");
		int totalDemand = 0;
		List<String> shortNames = new ArrayList<String>();
		for (Stop s : stops)
		{
			shortNames.add(s.shortName);
			List<String> str = new ArrayList<String>();
			for (Stop s2 : stops)
			{
				int demand = getDemand(s, s2);
				str.add(String.format("%3d",demand));
				totalDemand += demand;
			}
			String join = String.join(", ", str);
			System.out.printf("%3s. demand: %-20s\n", s.shortName, join);
		}
		System.out.println("destination: " + String.join(", ", shortNames));
		System.out.println("total passengers= " + totalDemand);

		System.out.println("lines (" + lines.size() + ")=");
		for (int i = 0; i < lines.size(); i++)
		{
			Line l = lines.get(i);
			System.out.printf("%3d. %-15s %-6s costs=%3.2f \t%-20s\n", i, l, l.minFreq + "-" + l.maxFreq, l.costs, l.stopsNameString());
		}
		System.out.println("vertices (" + vertices.size() + ")=");
		for (int i = 0; i < vertices.size(); i++)
		{
			Vertex v = vertices.get(i);
			System.out.printf("%4d. %-40s\n", i, v + " " + v.type);
			for (Arc a : v.getArcsOut())
			{
				System.out.printf("\tOUT %-40s\n", a);

			}
			for (Arc a : v.getArcsIn())
			{
				System.out.printf("\tIN  %-40s\n", a);
			}
		}
		System.out.println("arcs (" + arcs.size() + ")=" );
		for (int i = 0; i < arcs.size(); i++)
		{
			System.out.printf("%4d. %-40s\n", i, arcs.get(i));
		}
	}

	public void printToFile()
	{
		try 
		{
			String dir = "run/" + name + "/";
			File directory = new File(dir);
			if (!directory.exists())
			{
				directory.mkdir();
			}
			
			PrintStream stdout = System.out;
			PrintStream stream = new PrintStream("run/" + name + "/" + dateTime + "_instance.txt");
			System.setOut(stream);
			print();
			System.setOut(stdout);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public List<Stop> getStops() {
		return stops;
	}

	public List<OD> getPairs() {
		return pairs;
	}

	public List<Line> getLines() {
		return lines;
	}

	public List<Edge> getEdges() {
		return edges;
	}

	public Edge findEdge(Stop s1, Stop s2)
	{
		for (Edge e : edges)
		{
			if (e.leftStop == s1 && e.rightStop == s2 || e.leftStop == s2 && e.rightStop == s1)
			{
				return e;
			}
		}
		System.out.println(edges);
		System.out.println(s1 + " " + s2);
		return null;

	}

	public Edge createEdge(Stop s1, Stop s2, int distance)
	{
		int size = edges.size();
		Edge e = new Edge(size, s1, s2, distance, distance, distance);
		this.edges.add(e);
		return e;
	}

	public int getDemand(Stop d1, Stop d2)
	{
		for (OD od : pairs)
		{
			if (od.origin == d1 && od.destination == d2)
			{
				return od.count;
			}
		}

		return 0;
	}

	public int getTotalDemand()
	{
		int demand = 0;
		for (OD od : pairs)
		{
			demand += od.count;
		}
		return demand;
	}

	public List<Line> generateLinePool(List<int[]> linesWithStops)
	{
		List<Line> lines = new ArrayList<Line>();
		for (int i = 0; i < linesWithStops.size(); i++)
		{
			Line l = new Line(i);
			List<Stop> stops = new ArrayList<Stop>();
			int[] array = linesWithStops.get(i);
			for (int j = 0; j < array.length; j++)
			{
				
				stops.add(findStopByShortString(array[j]));
			}
			l.addStops(stops);
			l.addEdge(this);
			lines.add(l);
			l.minFreq = 1;
			l.maxFreq = 4;
			l.costs = 1000;
		}
		return lines;
	}

	public void printLineFile(String path)
	{
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(path + "Pool.giv"));
			bw.write("# line_index; link_order; link_index\n");
			for (Line l : lines)
			{
				int i = 1;
				for (Edge e : l.edges)
				{
					bw.write(String.join(";", 
							l.id + "", 
							i + "", 
							e.id + "\n"));
					i++;
				}

			}
			bw.close();

		}
		catch (Exception e)
		{

		}
	}
}
