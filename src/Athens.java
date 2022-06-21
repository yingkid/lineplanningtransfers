//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class Athens {
//
//	public void reduceAthens()
//	{
//		// Section 1: stations KIF - PEI
//		// Section 2: stations AAN - ADM
//		// Section 3: stations EGA - AER
//
//		// Transfer stations: ATT, OMO, MON, SYN
//
//		// Stations to remove: KIF (1) - KPAT (12) -> AGNI (13)
//		// Stations to remove: PET (19) - PEI (24) -> THI (18)
//		// Stations to remove: AAN (25) -> SEP (26)
//		// Stations to remove: FIX (32) - ADM (36) -> AKR (31)
//		// Stations to remove: EGA (37) - ELA (38) -> KER (39)
//		// Stations to remove: NMO (41) - AER (51) -> EVA (40)
//
//		List<Stop> reducedStops = new ArrayList<Stop>();
//		List<Line> reducedLines = new ArrayList<Line>();
//		List<Edge> reducedEdges = new ArrayList<Edge>();
//		List<OD> reducedOD = new ArrayList<OD>();
//		for (Stop s : stops)
//		{
//			if (s.id <= 12) continue;
//			if (s.id >= 19 && s.id <= 24) continue;
//			if (s.id == 25) continue;
//			if (s.id >= 32 && s.id <= 38) continue;
//			if (s.id >= 41 && s.id <= 51) continue;
//			reducedStops.add(s);
//		}
//
//		for (Stop s : reducedStops)
//		{
//			System.out.println(s);
//		}
//
//		outerloopLines:
//			for (Line l : lines)
//			{
//				List<Stop> stops = new ArrayList<Stop>(l.stops);
//				for (Stop s : l.stops)
//				{
//					if (!reducedStops.contains(s))
//					{
//						stops.remove(s);
//					}
//				}
//
//				if (stops.size() > 1) 
//				{
//					List<Edge> edges = new ArrayList<Edge>(l.edges);
//					for (Edge e : l.edges)
//					{
//						if (!reducedStops.contains(e.leftStop) || !reducedStops.contains(e.rightStop))
//						{
//							edges.remove(e);
//						}
//					}
//					Line reducedLine = new Line(l, edges, stops);
//					//check for same lines
//					for (Line r : reducedLines)
//					{
//						if (r.stops.equals(reducedLine.stops))
//						{
//							//sets min/max if smaller/larger respectively
//							if (r.minFreq < l.minFreq) l.minFreq = r.minFreq;
//							if (r.maxFreq > l.maxFreq) l.maxFreq = r.maxFreq;
//							continue outerloopLines;
//						}
//					}
//					reducedLines.add(reducedLine);
//				}
//			}
//
//		for (Line l : reducedLines)
//		{
//			System.out.println(l + " " + l.stopsString() + " " + l.minFreq + " " + l.maxFreq);
//		}
//
//		for (Edge e : edges)
//		{
//			if (reducedStops.contains(e.leftStop) && reducedStops.contains(e.rightStop))
//			{
//				reducedEdges.add(e);
//			}
//		}
//		for (Edge e : reducedEdges)
//		{
//			System.out.println(e);
//		}
//
//		List<OD> tempODs = new ArrayList<OD>();
//		for (OD od : pairs)
//		{
//			if (reducedStops.contains(od.origin) && reducedStops.contains(od.destination))
//			{
//				reducedOD.add(new OD(od.origin, od.destination, od.count));
//			}
//			else
//			{
//				Stop newOrigin = convertStop(od.origin);
//				Stop newDestination = convertStop(od.destination);
//
//				if (newOrigin == newDestination) continue;
//
//				OD tempOD = new OD(newOrigin, newDestination, od.count);
//				tempODs.add(tempOD);
//			}
//		}
//
//		for (OD od : tempODs)
//		{
//			boolean added = false;
//			for (OD odr : reducedOD)
//			{
//				if (odr.add(od))
//				{
//					added = true;
//					break;
//				}
//			}
//			if (!added) System.out.println("error: " + od);
//		}
//
//
//		try {
//			BufferedWriter bw = new BufferedWriter(new FileWriter("datasets/athens/basisreduced/Stop.giv"));
//			bw.write("# stop-id; short-name; long-name; x-coord; y-coord\n");
//			for (Stop s : reducedStops)
//			{
//				bw.write(String.join(";", s.id + "", s.shortName, s.longName, s.x + "", s.y + "\n"));
//			}
//			bw.close();
//		}
//		catch (IOException e) 
//		{
//			System.out.print(e.getMessage());
//		}
//
//		try {
//			BufferedWriter bw = new BufferedWriter(new FileWriter("datasets/athens/basisreduced/Edge.giv"));
//			bw.write("# edge-id; left-stop-id; right-stop-id;  length;  lower-bound; upper-bound\n");
//			for (Edge e : reducedEdges)
//			{
//				bw.write(String.join(";", 
//						e.id + "", 
//						e.leftStop.id + "", 
//						e.rightStop.id + "", 
//						e.length + "", 
//						e.minTraveltime  + "", 
//						e.maxTraveltime + "\n"));
//			}
//			bw.close();
//		}
//		catch (IOException e) 
//		{
//			System.out.print(e.getMessage());
//		}
//
//		try {
//			BufferedWriter bw = new BufferedWriter(new FileWriter("datasets/athens/basisreduced/Edge.giv"));
//			bw.write("# edge-id; left-stop-id; right-stop-id;  length;  lower-bound; upper-bound\n");
//			for (Edge e : reducedEdges)
//			{
//				bw.write(String.join(";", 
//						e.id + "", 
//						e.leftStop.id + "", 
//						e.rightStop.id + "", 
//						e.length + "", 
//						e.minTraveltime  + "", 
//						e.maxTraveltime + "\n"));
//			}
//			bw.close();
//		}
//		catch (IOException e) 
//		{
//			System.out.print(e.getMessage());
//		}
//
//		try {
//			BufferedWriter bw = new BufferedWriter(new FileWriter("datasets/athens/basisreduced/Load.giv"));
//			bw.write("# link_index; load; minimal_frequency; maximal_frequency\n");
//			for (Edge e : reducedEdges)
//			{
//				bw.write(String.join(";", 
//						e.id + "", 
//						e.load + "", 
//						e.minFreq + "", 
//						e.maxFreq + "\n"));
//			}
//			bw.close();
//		}
//		catch (IOException e) 
//		{
//			System.out.print(e.getMessage());
//		}
//
//		try {
//			BufferedWriter bw = new BufferedWriter(new FileWriter("datasets/athens/basisreduced/Pool.giv"));
//			bw.write("# line_index; link_order; link_index\n");
//			for (Line l : reducedLines)
//			{
//				int i = 1;
//				for (Edge e : l.edges)
//				{
//					bw.write(String.join(";", 
//							l.id + "", 
//							i + "", 
//							e.id + "\n"));
//					i++;
//				}
//
//			}
//			bw.close();
//		}
//		catch (IOException e) 
//		{
//			System.out.print(e.getMessage());
//		}
//
//		try {
//			BufferedWriter bw = new BufferedWriter(new FileWriter("datasets/athens/basisreduced/Pool-Cost.giv"));
//			bw.write("# line_index; length; cost\n");
//			for (Line l : reducedLines)
//			{
//				bw.write(String.join(";", 
//						l.id + "", 
//						l.length + "", 
//						l.costs + "\n"));
//
//			}
//			bw.close();
//		}
//		catch (IOException e) 
//		{
//			System.out.print(e.getMessage());
//		}
//
//		try {
//			BufferedWriter bw = new BufferedWriter(new FileWriter("datasets/athens/basisreduced/OD.giv"));
//			for (OD od : reducedOD)
//			{
//				bw.write(String.join(";", 
//						od.origin.id + "", 
//						od.destination.id + "", 
//						od.count + "\n"));
//
//			}
//			bw.close();
//		}
//		catch (IOException e) 
//		{
//			System.out.print(e.getMessage());
//		}
//	}
//
//
//	private Stop convertStop(Stop original)
//}
//
