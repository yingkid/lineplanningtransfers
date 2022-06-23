import java.io.*;
import java.util.*;

public class Solution {
	public final Instance i;
	public final long objectiveValue;
	public final List<Line> lines;
	public final HashMap<Line, Integer> frequencies;
	public final HashMap<Arc, HashMap<Stop, Integer>> arcs;
	public final List<Arc> transferArcs;
	public List<Cycle> cycles;

	public final int iteration;
	public final long time;
	public EAN ean;
	public final int nrFRPLATF;
	public final int nrTRANSF;

	public Solution(Instance i, List<Line> lines, HashMap<Line, Integer> frequencies, HashMap<Arc, HashMap<Stop, Integer>> arcs, List<Arc> transferArcs, 
			double objectiveValue, int nrFRPLATF, int nrTRANSF, int iteration, long time)
	{
		this.i = i;
		this.lines = lines;
		this.frequencies = frequencies;
		this.arcs = arcs;
		this.transferArcs = transferArcs;
		this.objectiveValue = Math.round(objectiveValue);

		this.iteration = iteration;
		this.time = time;
		this.nrFRPLATF = nrFRPLATF;
		this.nrTRANSF = nrTRANSF;
	}


	public void writeSummary(String path)
	{
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
			outputStr.add(lines.size() + "");
			outputStr.add(transferArcs.size() + "");
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

	private void printSolution()
	{
		System.out.println("objective value from cplex " + this.objectiveValue);
		System.out.println("Solution obj: " + this.getObjectiveValue());

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
			for (Arc a : transferArcs)
			{
				System.out.println(0 + "\t" + a + " oStop=" + a.from.stop.shortName);
			}
		}
	}

	public void writeSolutionIteration(String...args) 
	{
		String dir = "run/" + i.dateTime + "/";
		File directory = new File(dir);
		if (!directory.exists())
		{
			directory.mkdir();
		}
		String path = dir + i.dateTime +  "_solution_" + iteration + ".txt";
		if (args.length > 0)
		{
			path = "run/" + i.dateTime + "_solution_final.txt";
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
		this.cycles = cycles;
	}
	public void writeFinalSolution()
	{
		try 
		{
			writeSolutionIteration("run/" + i.dateTime + "_solution_final.txt");


			//Write results overall file
			Writer output;
			output = new BufferedWriter(new FileWriter("run/all_solutions.txt", true));
			List<String> outputStr = new ArrayList<String>();
			outputStr.add(i.dateTime + "");
			outputStr.add(i.name + "");
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
