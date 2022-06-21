import java.io.File;
import java.util.*;
import java.util.Map.*;

import ilog.concert.*;
import ilog.cplex.*;
public class Model {
	private IloCplex cplex;
	private Instance i;
	private IloLinearNumExpr minTravelTime;
	private IloLinearNumExpr minLineCosts;
	
	public Model(Instance i) 
	{
		this.i = i;
		try
		{
			initVariables();
			populate();
			setObjective();
		}
		catch (Exception e)
		{
			System.out.println(e);
		}

	}

	private void initVariables() throws IloException
	{
		System.out.println("initVariables");
		cplex = new IloCplex();
		//cplex.setOut(null);
		cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap	, Settings.CPLEXMIPGAP);
		

		//x-variables
		System.out.println("initialize x-variables");
		for (Line l : i.getLines())
		{
			HashMap<Integer, IloIntVar> xvarLine = new HashMap<Integer, IloIntVar>();
			for (int f = l.minFreq; f <= l.maxFreq; f++)
			{
				IloIntVar x = cplex.boolVar();
				xvarLine.put(f, x);
				x.setName("L" + l.id + "_" + f);
			}
			l.lineFrequencyVar = xvarLine;
		}

		//y-variables
		System.out.println("initialize y-variables");
		for (Arc a : i.getArcs())
		{
			a.yvar = new HashMap<Stop, IloNumVar>();
			for (Stop s : i.getStops())
			{
				IloNumVar yvar = cplex.numVar(0, Settings.BIGM);
				String from = a.from.line != null ? a.from.line.shortString() : "--";
				String to = a.to.line != null ? a.to.line.shortString() : "--";
				yvar.setName("S" + s.id + " (" + from + "_" + to + " " + a.type.toString().toLowerCase() +  ")");
				a.yvar.put(s, yvar);
				;			
			}
		}

		//z-variables
		if (Settings.SHORTTRANSFERS)
		{
			for (Arc a : i.getArcs())
			{
				if (a.type != Arc.Type.TRANSF) continue;
				IloNumVar zvar = cplex.boolVar();
				a.zvar = zvar;
				zvar.setName("z");
				
			}
		}
	}

	private void populate() throws IloException
	{
		populateX();
		populateY();
		populateZ();
	}

	private void populateX() throws IloException 
	{
		//x-variables
		System.out.println("x-constraints");
		for (Line l : i.getLines())
		{
			IloLinearIntExpr expr = cplex.linearIntExpr();
			for (IloIntVar x : l.lineFrequencyVar.values())
			{
				expr.addTerm(1, x);
			}
			IloConstraint constr = cplex.addLe(expr, 1);
			constr.setName(l.toString());
			//constraints.add(constr);
		}

		if (Settings.INCLUDETOTALLINECOSTS)
		{
			IloLinearNumExpr expr = cplex.linearNumExpr();
			for (Line l : i.getLines())
			{
				for (Map.Entry<Integer, IloIntVar> x : l.lineFrequencyVar.entrySet())
				{
					expr.addTerm(l.costs, x.getValue());
				}
			}
			cplex.addLe(expr, Settings.MAXLINECOSTS);

		}

	}

	private void populateY() throws IloException
	{
		//y-variables
		//flow conservation constraints
		System.out.println("y-constraints flow conservation");
		for (Stop s : i.getStops())
		{
			for (Vertex v : i.getVertices())
			{

				int delta = 0;

				IloLinearNumExpr expr = cplex.linearNumExpr();

				for (Arc a : v.getArcsIn())
				{
					expr.addTerm(1, a.yvar.get(s));
				}
				for (Arc a : v.getArcsOut())
				{
					expr.addTerm(-1, a.yvar.get(s));
				}
				if (v.type == Vertex.Type.OUT) 
				{
					delta = i.getDemand(s, v.stop);//sink
				}
				else if (v.type == Vertex.Type.IN)
				{
					if (v.stop != s) continue;
					for (Stop s2 : i.getStops())
					{
						delta -= i.getDemand(s, s2);
					}
				}
				IloRange constr = cplex.addEq(expr, delta);
				//System.out.println(delta + " " + v + " " + constr);
			}
		}

		//constraint 3.10
		System.out.println("y-constraints capacity 3.10");
		for (Line l : i.getLines())
		{
			for (Arc a : i.getArcs())
			{
				if (a.type == Arc.Type.TRAVEL && a.line == l)
				{

					IloLinearNumExpr exprLeft = cplex.linearNumExpr();
					IloLinearNumExpr exprRight = cplex.linearNumExpr();
					for (Stop o : i.getStops())
					{
						exprLeft.addTerm(1, a.yvar.get(o));
					}

					for (Entry<Integer, IloIntVar> lf : l.lineFrequencyVar.entrySet())
					{
						exprRight.addTerm(l.capacity * lf.getKey(), lf.getValue());
					}
					IloConstraint constr = cplex.addLe(exprLeft, exprRight);	
					constr.setName(a + " " + l);
					//constraints.add(constr);
				}
			}
		}
		//constraint 3.11
		System.out.println("y-constraints capacity 3.11");

		//System.out.println(counter++ + " "  + a);
		for (Line l : i.getLines())
		{
			for (Map.Entry<Integer, IloIntVar> x : l.lineFrequencyVar.entrySet())
			{
				List<Arc> arcsLine = new ArrayList<Arc>();
				arcsLine.addAll(l.backwardArcs);
				arcsLine.addAll(l.forwardArcs);
				System.out.println(l + " " + arcsLine);
				for (Arc a : arcsLine)
				{
					if (a.type == Arc.Type.FRPLAT)
					{
						IloLinearNumExpr exprLeft = cplex.linearNumExpr();
						IloLinearNumExpr exprRight = cplex.linearNumExpr();
						for (Stop origin : i.getStops())
						{
							if (x.getKey() == a.freq)
							{
								exprLeft.addTerm(1, a.yvar.get(origin));
							}
						}
						exprRight.addTerm(l.capacity, x.getValue());
						IloConstraint constr = cplex.addLe(exprLeft, exprRight);
						//System.out.println(constr.toString());
						//constr.setName("3.11");
						//constraints.add(constr);
					}
				}
			}
		}
	}

	private void populateZ() throws IloException 
	{
		if (Settings.SHORTTRANSFERS)
		{
			for (Arc a : i.getArcs())
			{
				IloLinearNumExpr exprLeft = cplex.linearNumExpr();
				IloLinearNumExpr exprRight = cplex.linearNumExpr();
				if (a.type == Arc.Type.TRANSF)
				{
					for (Stop s : i.getStops())
					{
						exprLeft.addTerm(1, a.yvar.get(s));
					}
					exprRight.addTerm(Settings.BIGM, a.zvar);
					IloConstraint z = cplex.addLe(exprLeft, exprRight);
					z.setName("yz");
					
//					IloConstraint z0 = cplex.eq(a.zvar, 0);
//					IloConstraint z2 = cplex.eq(exprLeft, 0);
//					cplex.add(cplex.ifThen(z0, z2));
					

					
				}
				
			}
		}
	}

	private void populateZexcludedArcs(Cycle cycle) throws IloException
	{
		if (cycle != null)
		{
			List<Cycle> excludedCycles = i.excludedCycles;
			IloLinearNumExpr expr = cplex.linearNumExpr();
			System.out.println(cycle);
			for (Arc a : cycle.getArcs())
			{
				System.out.println("\t" + a);
				expr.addTerm(1, a.zvar);
			}
			IloRange r = cplex.addLe(expr, cycle.getArcs().size() - 1);
			r.setName("cycle");
			excludedCycles.add(cycle);
		}


	}
	private void setObjective() throws IloException
	{
		minTravelTime = cplex.linearNumExpr();
		for (Arc a : i.getArcs())
		{
			for (IloNumVar yvar : a.yvar.values())
			{
				minTravelTime.addTerm(a.value, yvar);
			}
		}


		minLineCosts = cplex.linearNumExpr();
		for (Line l : i.getLines())
		{
			for (Map.Entry<Integer, IloIntVar> x : l.lineFrequencyVar.entrySet()) 
			{
				minLineCosts.addTerm(l.costs, x.getValue());
			}
		}


		if (Settings.MINTRAVELTIME)
		{
			cplex.addMinimize(minTravelTime);
			return;
		}
		if (Settings.MINTRAVELTIME)
		{
			cplex.addMinimize(minLineCosts);
			return;
		}
		IloNumExpr[] objArray = new IloNumExpr[] {minTravelTime, minLineCosts};

		cplex.add(cplex.minimize(cplex.staticLex(objArray)));
	}
	
	public double getMinTravelTimeObj()
	{
		try 
		{
			return cplex.getValue(minTravelTime);
		} 
		catch (IloException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	public double getMinLineCostsObj()
	{
		try 
		{
			return cplex.getValue( minLineCosts);
		} 
		catch (IloException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	public Solution solve(int iteration, long startTime) throws IloException
	{
		System.out.println("solving...");
		Solution sol = null;
		if (cplex.solve())
		{
			long endTime = System.nanoTime();
			long duration = endTime - startTime;
			sol = new Solution(this, cplex, i, iteration, duration);
			//exportModel(iteration);
		}
		else
		{
			System.err.println(cplex.getStatus());
		}
		return sol;
	}


	public boolean solveIteratively() throws IloException
	{
		System.out.println("Solve iteratively");
		List<Solution> solutions = new ArrayList<Solution>();

		int it = 1;
		long startTime = System.nanoTime();
		Solution sol = solve(it, startTime);
		solutions.add(sol);

		if (sol != null)
		{
			List<Cycle> cycles = new ArrayList<Cycle>();
			Cycle z = cycleCheck(sol);
			cycles.add(z);
			sol.addCycles(cycles);
			sol.writeSummary("run/" + i.dateTime + "_solution.txt");
			sol.writeSolutionIteration();
			
			while (z != null)
			{
				it++;
				populateZexcludedArcs(z);
				sol = solve(it, startTime);
				z = cycleCheck(sol);
				cycles.add(z);
				sol.addCycles(cycles);
				sol.writeSummary("run/" + i.dateTime + "_solution.txt");
				sol.writeSolutionIteration();
			}
			sol.writeFinalSolution();
		}
		else
		{
			
			System.err.println("No solution");
		}

		cplex.close();

		return true;
	}
	private void exportModel(int iteration) throws IloException
	{
		String dirPath = "run/" + i.dateTime + "/";
		File directory = new File(dirPath);
	    if (!directory.exists())
	    {
	        directory.mkdir();
	    }
		cplex.exportModel(dirPath + "iteration" + iteration + ".lp");
		
	}

	private Cycle cycleCheck(Solution sol) 
	{
		EAN network = new EAN(sol, i);
		List<Cycle> cycle_family = new ArrayList<Cycle>();

		for (Stop transferStation : network.transferStations)
		{
			for (Event e : network.events)
			{
				if (e.stop == transferStation && e.type == Event.Type.DEP)
				{
					ShortestPath sp = new ShortestPath(network);
					int[] dist = sp.Dijkstra(e);
					//int min_length = Integer.MAX_VALUE;
					int max_delta = Integer.MIN_VALUE;
					Cycle cycle_candidate = null;

					for (Event e2 : network.events)
					{
						if (e2.type == Event.Type.ARR)
						{
							if (e2.hasNextEvent(e))
							{
								int idx = network.events.indexOf(e2);
								if (dist[idx] >= 0 && dist[idx] < Integer.MAX_VALUE) //consider only feasible paths
								{
									int length = dist[idx] + e2.getOutActivity(e).value;
									int cp = Settings.CYCLEPERIOD;
									int remainder = length % cp;
									int delta = cp / 2 - Math.abs(cp / 2 - remainder);

									//System.out.println(length + " " + delta);
									if (delta > max_delta)
									{

										if (delta > Settings.DELTADEVIATION)
										{
											Cycle cycle = sp.getCycle(e, e2);
											cycle.setDelta(delta);
											cycle.setLength(length);
											if (cycle.transfers > 2)
											{
												max_delta = delta;
												cycle_candidate = cycle;
											}


										}

									}
								}
							}
						}
					}
					if (cycle_candidate != null)
					{
						cycle_family.add(cycle_candidate);
					}
				}

			}
		}

		if (cycle_family.isEmpty())
		{
			return null;
		}
		else
		{		
			Collections.sort(cycle_family);

			return cycle_family.get(cycle_family.size() - 1);
		}
	}
}

