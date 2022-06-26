import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

public class Solution {
	public final Instance i;
	public final long objectiveValue;
	public final HashMap<Line, Boolean> lines;
	public final HashMap<Line, Integer> frequencies;
	public final HashMap<Arc, HashMap<Stop, Integer>> arcs;
	public final HashMap<Arc, Boolean> transferArcs;
	public final List<Cycle> cycles;

	public final int iteration;
	public final long time;
	public EAN ean;
	public final int nrFRPLATF;
	public final int nrTRANSF;

	public Solution(Instance i, HashMap<Line, Boolean> lines, HashMap<Line, Integer> frequencies, HashMap<Arc, HashMap<Stop, Integer>> arcs, HashMap<Arc, Boolean> transferArcs, 
			double objectiveValue, int nrFRPLATF, int nrTRANSF, int iteration, long time)
	{
		this.i = i;
		this.lines = lines;
		this.frequencies = frequencies;
		this.arcs = arcs;
		this.transferArcs = transferArcs;
		this.cycles = new ArrayList<Cycle>();
		this.objectiveValue = Math.round(objectiveValue);

		this.iteration = iteration;
		this.time = time;
		this.nrFRPLATF = nrFRPLATF;
		this.nrTRANSF = nrTRANSF;
	}


	public void writeSummary()
	{
		String path = "run/" + i.name + "/" + i.dateTime + "_solution.txt";
		try
		{
			File f = new File(path);
			boolean exists = !f.exists();
			Writer output = new BufferedWriter(new FileWriter(path, true));

			List<String> outputStr = new ArrayList<String>();
			if (exists)
			{
				outputStr.add("iteration");
				outputStr.add("objectiveValue");
				outputStr.add("#lines selected");
				outputStr.add("#transferArcs");
				outputStr.add("#cycles");
				outputStr.add("#events");
				outputStr.add("#activities");
				outputStr.add("#shortTransfers");
				outputStr.add("#fastTransfers");
				outputStr.add("time");

				String join = String.join(", ", outputStr) + "\n";
				output.append(join);
			}
			outputStr.clear();

			outputStr.add(iteration + "");
			outputStr.add(objectiveValue + "");
			outputStr.add(getNSelectedLines() + "");
			outputStr.add(getNTransferArcs() + "");
			outputStr.add(cycles.size() + "");
			outputStr.add(ean.events.size() + "");
			outputStr.add(ean.activities.size() + "");
			outputStr.add(nrFRPLATF + "");
			outputStr.add(nrTRANSF + "");


			outputStr.add((double) Math.round(time/1e9 * 100) / 100 + "");

			String join = String.join(", ", outputStr) + "\n";
			output.append(join);
			output.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

	}
	
	private int getObjectiveValue()
	{
		int val = 0;
		for (Map.Entry<Arc, HashMap<Stop, Integer>> arc : arcs.entrySet())
		{
			for (Map.Entry<Stop, Integer> stop : arc.getValue().entrySet())
			{
				val += arc.getKey().value * stop.getValue();
			}
		}
		return val;
	}
	
	private int getNSelectedLines()
	{
		return Collections.frequency(lines.values(), true);
	}
	
	private int getNTransferArcs()
	{
		return Collections.frequency(transferArcs.values(), true);

	}

	private void printSolution()
	{
		System.out.println("objective value from cplex " + this.objectiveValue);
		System.out.println("Solution obj: " + this.getObjectiveValue());
		//System.out.println("Transfer arcs valid?:" + this.checkTransferSolution(this.getTransferArcs()));
		System.out.println("x decision variables");
		for (Line l : i.getLines())
		{
			String str;

			if (frequencies.containsKey(l))
			{
				str = 1 + "\t"+ String.format("%s_%1d %3.2f (%s)", l.shortString(), frequencies.get(l), l.costs, l.stopsString());
			}
			else
			{
				str = 0 + "\t"+ String.format("%s_%1d %3.2f (%s)", l.shortString(), 0, l.costs, l.stopsString());
			}
			System.out.println(str);

		}

		System.out.println("y decision variables (only arcs with a flow and a weight are printed)");
   
		for (Map.Entry<Arc, HashMap<Stop, Integer>> arc : arcs.entrySet())
		{
			for (Map.Entry<Stop, Integer> stop : arc.getValue().entrySet())
			{
				int value = stop.getValue();
				if (value > 0) 
				{
					System.out.println(value + "\t" + arc.getKey() + " oStop=" + stop.getKey().shortName);

				}
			}
		}

		//z-variables
		if (Settings.SHORTTRANSFERS)
		{
			System.out.println("z decision variables");
			for (Map.Entry<Arc, Boolean> transfer : transferArcs.entrySet())
			{
				System.out.println((transfer.getValue() ? 1 : 0) + "\t" + transfer.getKey() + " oStop=" + transfer.getKey().from.stop.shortName);
			}
		}
	}
	
	public List<Arc> getTransferArcs()
	{
		List<Arc> transfers = new ArrayList<Arc>();
		if (Settings.SHORTTRANSFERS)
		{
			for (Map.Entry<Arc, Boolean> transfer : transferArcs.entrySet())
			{
				if (transfer.getValue())
				{
					transfers.add(transfer.getKey());
				}
			}
		}
		return transfers;
	}
	
	public void fixSolution()
	{
		HashMap<Arc, Boolean> fixedTransferArcs = new LinkedHashMap<Arc, Boolean>(this.transferArcs);

		HashMap<Cycle, Integer> costs = new LinkedHashMap<Cycle, Integer>();
		for (Cycle c : cycles)
		{	
			Arc aMin = null;
			int min = Integer.MAX_VALUE;
			for (Arc a : c.arcs)
			{	
				int total = arcs.get(a).values().stream().mapToInt(Integer::intValue).sum();
				if (total < min)
				{
					min = total;
					aMin = a;
				}
			}
		}

	}
	
//	private boolean checkTransferSolution(List<Arc> transfers)
//	{
//		for (Cycle c : cycles)
//		{
//			int i = 0;
//			for(Arc a : c.arcs)
//			{
//				if (transfers.contains(a)) i++;
//					
//			}
//			if (i == c.arcs.size()) return false;
//		}
//		return true;
//	}
	

	public void writeSolutionIteration(String...args) 
	{
		
		String dir = "run/" + i.name + "/" + i.dateTime + "/";
		File directory = new File(dir);
		if (!directory.exists())
		{
			directory.mkdir();
		}
		
		String path = dir + "solution_" + iteration + ".txt";
		if (args.length > 0)
		{
			path = "run/" + i.name + "/" + i.dateTime + "_solution_final.txt";
		}
		try {
			PrintStream stdout = System.out;
			PrintStream stream = new PrintStream(path);
			System.setOut(stream);
			printSolution();
			System.out.println("Cycles:");
			int counter = 1;
			for (Cycle c : cycles)
			{
				if (c == null) continue;
				System.out.println("Cycle " + counter++ + " delta: " + c.getDelta() + ", length: " + c.getLength());
				for (Arc a : c.getArcs())
				{
					System.out.println("\t" + a + " at Stop " + a.from.stop.id + " " + a.from.stop.shortName);
				}
			}
			System.out.println("transferStations: ");
			for (Stop s : ean.transferStations)
			{
				System.out.println(s);
			}
			System.setOut(stdout);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void addCycles(List<Cycle> cycles)
	{
		this.cycles.addAll(cycles);
	}
	public void writeFinalSolution()
	{
		try 
		{
			writeSolutionIteration("do");
			//Write results overall file
			Writer output;
			output = new BufferedWriter(new FileWriter("run/" + i.name + "/all_solutions.txt", true));
			List<String> outputStr = new ArrayList<String>();
			outputStr.add(i.dateTime + "");
			outputStr.add(i.path + "");
			outputStr.add(objectiveValue + "");
			outputStr.add(iteration + "");
			outputStr.add(i.getStops().size() + "");
			outputStr.add(i.getEdges().size() + "");
			outputStr.add(i.getLines().size() + "");
			outputStr.add(i.getPairs().size() + "");
			outputStr.add(i.getVertices().size() + "");
			outputStr.add(i.getArcs().size() + "");
			outputStr.add(cycles.size() + "");
			outputStr.add(ean.events.size() + "");
			outputStr.add(ean.activities.size() + "");
			outputStr.add(nrFRPLATF + "");
			outputStr.add(nrTRANSF + "");
			outputStr.add(Settings.MAXLINECOSTS +"");
			outputStr.add(Math.round(objectiveValue/i.getTotalDemand()) + "");


			outputStr.add((double) Math.round(time/1e9 * 100) / 100 + "");

			String join = String.join(", ", outputStr) + "\n";
			output.append(join);
			output.close();
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
