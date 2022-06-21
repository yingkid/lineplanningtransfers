import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class Solution {
	public final long objectiveValue;
	public final List<Line> lines;
	public final HashMap<Line, Integer> frequencies;
	public final List<Arc> arcs;
	public final List<Arc> transferArcs;
	public List<Cycle> cycles;
	public final Model model;
	public final IloCplex cplex;
	public final Instance i;
	public final int iteration;
	public long time;
	public EAN ean;
	public int nrFRPLATF = 0;
	public int nrTRANSF = 0;

	public Solution(Model model, IloCplex cplex, Instance i, int iteration, long time) throws IloException
	{
		this.lines = new ArrayList<Line>();
		this.frequencies = new HashMap<Line, Integer>();
		this.arcs = new ArrayList<Arc>();
		this.transferArcs = new ArrayList<Arc>();
		this.model = model;
		this.cplex = cplex;
		this.i = i;
		this.iteration = iteration;
		this.time = time;

		this.objectiveValue = Math.round(cplex.getObjValue());

		getLinesAndFrequencies();
		getArcs();
		getTransfers();


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

	private void getLinesAndFrequencies() throws IloException
	{
		outerloop:
			for (Line l : i.getLines())
			{
				for (Entry<Integer, IloIntVar> entry : l.lineFrequencyVar.entrySet())
				{
					int value = (int) Math.round(cplex.getValue(entry.getValue()));
					if (value > 0)
					{
						lines.add(l);
						frequencies.put(l, entry.getKey());
						continue outerloop;
					}
				}

			}
	}

	private void getArcs() throws IloException
	{
		List<Arc> copyArcs = new ArrayList<Arc>(i.getArcs());
		Collections.sort(copyArcs, (a1, a2) -> a1.type.compareTo(a2.type));        
		for (Arc a : copyArcs)
		{
			for (Map.Entry<Stop, IloNumVar> entry : a.yvar.entrySet())
			{
				int value = (int) Math.round(cplex.getValue(entry.getValue()));
				//if (a.value == 0) continue; //skip in, out and toplatform arcs
				if (value != 0)
				{
					arcs.add(a);
					if (a.type == Arc.Type.FRPLAT)
					{
						nrFRPLATF += value;
					}
					else if (a.type == Arc.Type.TRANSF)
					{
						nrTRANSF += value;
					}
				}
			}
		}

	}

	private void getTransfers() throws IloException
	{
		if (Settings.SHORTTRANSFERS)
		{
			System.out.println("z decision variables");
			for (Arc a : i.getArcs())
			{
				if (a.type != Arc.Type.TRANSF) continue;
				int value = (int) Math.round(cplex.getValue(a.zvar));
				if (value != 0)
				{
					transferArcs.add(a);
				}
				System.out.println(value + "\t" + a + " stop=" + a.from.stop.shortName);

			}
		}
	}

	private void printSolution() throws IloException
	{
		System.out.println("objective value " + this.objectiveValue);
		System.out.println("minLineCosts " + model.getMinLineCostsObj());
		System.out.println("x decision variables");
		outerloop:
			for (Line l : i.getLines())
			{
				String str;
				for (Entry<Integer, IloIntVar> entry : l.lineFrequencyVar.entrySet())
				{
					int value = (int) Math.round(cplex.getValue(entry.getValue()));
					if (value > 0)
					{
						str = value + "\t"+ String.format("%s_%1d %3.2f (%s)", l.shortString(), entry.getKey(), l.costs, l.stopsString());
						System.out.println(str);
						continue outerloop;
					}
				}
				str = 0 + "\t"+ String.format("%s_%1d %3.2f (%s)", l.shortString(), 0, l.costs, l.stopsString());
				System.out.println(str);

			}

		System.out.println("y decision variables (only arcs with a flow and a weight are printed)");
		List<Arc> copyArcs = new ArrayList<Arc>(i.getArcs());
		Collections.sort(copyArcs, (a1, a2) -> a1.type.compareTo(a2.type));        
		for (Arc a : copyArcs)
		{
			for (Map.Entry<Stop, IloNumVar> entry : a.yvar.entrySet())
			{
				int value = (int) Math.round(cplex.getValue(entry.getValue()));
				if (value > 0) 
				{
					System.out.println(value + "\t" + a + " oStop=" + entry.getKey().shortName);

				}
			}
		}

		//z-variables
		if (Settings.SHORTTRANSFERS)
		{
			System.out.println("z decision variables");
			for (Arc a : i.getArcs())
			{
				if (a.type != Arc.Type.TRANSF) continue;
				int value = (int) Math.round(cplex.getValue(a.zvar));
				if (value == 0)
				{
					System.out.println(value + "\t" + a + " oStop=" + a.from.stop.shortName);
				}

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
