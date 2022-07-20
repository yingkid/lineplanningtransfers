import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * @author ying_
 *
 */
public class Instance {
	private LocalDateTime date = LocalDateTime.now();
	public String dateTime = date.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
	public String name = "";
	public String path = "";

	private List<Stop> stops;
	private List<Edge> edges;
	private List<OD> pairs;
	private List<Line> lines;
	private List<List<Line>> incompatibleLines;



	private List<Vertex> vertices;
	private List<Arc> arcs;


	/** create instance for the problem
	 * @param name name of the instance
	 * @param path path of the instance at the computer
	 */
	public Instance (String name, String path)
	{
		System.out.println("Instance path: " + path);
		this.name = name;
		this.path = path;

		this.stops = readStops(path + "Stop.giv");
		this.edges = readEdges(path + "Edge.giv");
		this.lines = readLines(path + "Pool.giv", path + "Pool-Cost.giv");
		readLoad(path + "Load.giv");
		this.incompatibleLines = new ArrayList<List<Line>>();
		this.pairs = readODpairs(path + "OD.giv");
		setStopFromDemand();
		setFrequencies();
		this.vertices = createVertices();
		this.arcs = createArcs();

		System.out.println("vertices " + vertices.size());
		System.out.println("arcs " + arcs.size());
	}

	/** create instance for the problem with given incompatibilities
	 * @param name
	 * @param path
	 * @param lIds
	 * @param lines
	 * @param incompatibles
	 */
	public Instance (String name, String path, List<Integer> lIds, List<int[]> lines, List<int[]> incompatibles)
	{
		System.out.println("Instance path: " + path);
		this.name = name;
		this.path = path;

		this.stops = readStops(path + "Stop.giv");
		this.edges = readEdges(path + "Edge.giv");
		this.lines = generateLinePool(lIds, lines);
		this.incompatibleLines = convertToLineList(incompatibles);
		readLoad(path + "Load.giv");
		this.pairs = readODpairs(path + "OD.giv");
		setStopFromDemand();
	
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
	
	public List<List<Line>> getIncompatibleLines() {
		return incompatibleLines;
	}

	private void setFrequencies()
	{
		for (Line l : lines)
		{
			l.setFrequencies();
		}
	}

	
	/** create vertices in the flow formulation
	 * @return list of vertices
	 */
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
	/** create arcs for the flow formulation
	 * @return list of arcs
	 */
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
				Arc leftToRight = new Arc(left, right, Arc.Type.TRAVEL, e.maxTraveltime, l, Arc.Direction.FORWARD);
				Arc rightToLeft = new Arc(right, left, Arc.Type.TRAVEL, e.maxTraveltime, l, Arc.Direction.BACKWARD);
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

	/** read edges from dataset from given file
	 * @param path of file
	 * @return list of edges
	 */
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

	/**read load from given file
	 * @param path of file
	 */
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

	/** read stops from given file
	 * @param path of file
	 * @return list of stops
	 */
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

	/** read od pairs from given file
	 * @param path of file
	 * @return list of od pairs
	 */
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

	/** read lines from given file
	 * @param path of file
	 * @param pathCosts path of file containing costs
	 * @return list of lines
	 */
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

	/** method to read files
	 * @param path of file
	 * @return list of strings
	 */
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
	
	private void setStopFromDemand()
	{
		for (Stop source : stops)
		{
			int demand = 0;
			for (Stop sink : stops)
			{
				demand += getDemand(source, sink);
			}
			source.demandFromThisOrigin = demand;
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
		System.err.println("cannot find stop " + id);
		return null;
	}

	private Stop findStopByShortString(int id)
	{
		for (Stop s : stops)
		{
			if (s.shortName.equals(Integer.toString(id))) return s;
		}
		System.err.println("cannot find stop by string " + id);
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
		for (Vertex v : vertices)
		{
			if (v.line == line && v.stop == stop)
			{
				return v;
			}
		}
		return null;
	}

	public Arc findArc(Stop s, int f)
	{
		for (Arc a : s.platform.getArcsOut())
		{
			if (a.type == Arc.Type.FRPLAT)
			{
				if (a.freq == f)
				{
					return a;
				}
			}
		}
		return null;
	}
	
	public int getTotalPassengers()
	{
		int passengers = 0;
		for (OD pair : pairs)
		{
			passengers += pair.count;
		}
		return passengers;
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
		List<String> shortNames = new ArrayList<String>();
		for (Stop s : stops)
		{
			shortNames.add(s.shortName);
			List<String> str = new ArrayList<String>();
			for (Stop s2 : stops)
			{
				int demand = getDemand(s, s2);
				str.add(String.format("%3d",demand));
			}
			String join = String.join(", ", str);
			System.out.printf("%3s. demand: %-20s\n", s.shortName, join);
		}
		System.out.println("destination: " + String.join(", ", shortNames));
		System.out.println("total passengers= " + getTotalPassengers());

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
		int counter = 1;

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

	/** find edge by two stops
	 * @param s1 stop 1
	 * @param s2 stop 2
	 * @return edge if found
	 */
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

	/** creates edges and add id
	 * @param s1 stop
	 * @param s2 stop
	 * @param distance between stops
	 * @return edge
	 */
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


	/** generates line pool with given stops
	 * @param linesWithStops
	 * @return list of lines
	 */
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
			l.maxFreq = Settings.MAXFREQUENCY;
			l.costs = Settings.LINECOSTS;
		}
		return lines;
	}
	
	/** generate line pool with given ids and list of stops
	 * @param IDs given ids for the line
	 * @param linesWithStops lines with stops
	 * @return
	 */
	public List<Line> generateLinePool(List<Integer> IDs, List<int[]> linesWithStops)
	{
		List<Line> lines = new ArrayList<Line>();
		for (int i = 0; i < linesWithStops.size(); i++)
		{
			int id = IDs.get(i);
			Line l = new Line(id);
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
			l.maxFreq = Settings.MAXFREQUENCY;
			l.costs = Settings.LINECOSTS;
		}
		return lines;
	}

	/** exporter lines if self created
	 * @param path
	 */
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

	public void instanceReduction(String path, int[] stopIDs)
	{
		List<Stop> stopsToRemove = new ArrayList<Stop>();
		for (int i = 0; i < stopIDs.length; i++)
		{
			Stop s = findStopByShortString(stopIDs[i]);
			stopsToRemove.add(s);
		}

		List<Stop> reducedStops = new ArrayList<Stop>(stops);
		List<Edge> reducedEdges = new ArrayList<Edge>();
		List<OD> reducedOD = new ArrayList<OD>();

		reducedStops.removeAll(stopsToRemove);
		for (Edge e : edges)
		{
			if (stopsToRemove.contains(e.leftStop)) continue;
			if (stopsToRemove.contains(e.rightStop)) continue;
			reducedEdges.add(e);
		}

		for (OD pair : pairs)
		{
			if (stopsToRemove.contains(pair.origin)) continue;
			if (stopsToRemove.contains(pair.destination)) continue;
			reducedOD.add(pair);
		}


		try 
		{
			BufferedWriter bw = new BufferedWriter(new FileWriter(path + "Stop.giv"));
			bw.write("# stop-id; short-name; long-name; x-coord; y-coord\n");
			for (Stop s : reducedStops)
			{
				bw.write(String.join(";", s.id + "", s.shortName, s.longName, s.x + "", s.y + "\n"));
			}
			bw.close();
		}
		catch (IOException e) 
		{
			System.out.print(e.getMessage());
		}

		try 
		{
			BufferedWriter bw = new BufferedWriter(new FileWriter(path + "Edge.giv"));
			bw.write("# edge-id; left-stop-id; right-stop-id;  length;  lower-bound; upper-bound\n");
			for (Edge e : reducedEdges)
			{
				bw.write(String.join(";", 
						e.id + "", 
						e.leftStop.id + "", 
						e.rightStop.id + "", 
						e.length + "", 
						e.minTraveltime  + "", 
						e.maxTraveltime + "\n"));
			}
			bw.close();
		}
		catch (IOException e) 
		{
			System.out.print(e.getMessage());
		}

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(path + "Load.giv"));
			bw.write("# link_index; load; minimal_frequency; maximal_frequency\n");
			for (Edge e : reducedEdges)
			{
				bw.write(String.join(";", 
						e.id + "", 
						e.load + "", 
						e.minFreq + "", 
						e.maxFreq + "\n"));
			}
			bw.close();
		}
		catch (IOException e) 
		{
			System.out.print(e.getMessage());
		}

		try 
		{
			BufferedWriter bw = new BufferedWriter(new FileWriter(path + "OD.giv"));
			for (OD od : reducedOD)
			{
				bw.write(String.join(";", 
						od.origin.id + "", 
						od.destination.id + "", 
						od.count + "\n"));

			}
			bw.close();
		}
		catch (IOException e) 
		{
			System.out.print(e.getMessage());
		}


	}
	
	private List<List<Line>> convertToLineList(List<int[]> incompatibles)
	{
		List<List<Line>> listLines = new ArrayList<List<Line>>();
		
		for(int[] inc : incompatibles)
		{
			List<Line> incLines = new ArrayList<Line>();
			for (int i = 0; i < inc.length; i++)
			{
				incLines.add(this.findLine(lines, inc[i]));
			}
		}
		return listLines;
	}
}
