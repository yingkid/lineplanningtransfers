import java.io.File;
import java.util.*;
import java.util.Map.*;

import ilog.concert.*;
import ilog.cplex.*;
public class Model {
	private IloCplex cplex;
	private Instance i;
	private HashMap<Line,HashMap<Integer, IloIntVar>> xvars;
	private HashMap<Arc, HashMap<Stop, IloNumVar>> yvars;
	private HashMap<Arc, IloNumVar> zvars;
	public HashMap<Arc, IloConstraint> shortTransferConstraints;
	private IloLinearNumExpr minTravelTime;
	private IloLinearNumExpr minLineCosts;
	private List<Cycle> excludedCycles = new ArrayList<Cycle>();

	/** class for modeling using cplex
	 * @param i instance
	 */
	public Model(Instance i) 
	{
		this.i = i;
		try
		{
			initVariables();
			initConstraints();
			initObjective();
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
		i.printToFile();
	}

	/** initialize decision variables
	 * @throws IloException
	 */
	private void initVariables() throws IloException
	{
		//big-m for yvars and transfer big-M round up to 1000
		Double BIGM = Math.ceil((double) i.getTotalPassengers()/100)*100;
		Settings.setBIGM(BIGM.intValue());

		System.out.println("initVariables");
		cplex = new IloCplex();
		//cplex.setOut(null);
		cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap	, Settings.CPLEXMIPGAP);


		xvars = new HashMap<Line,HashMap<Integer, IloIntVar>>();
		yvars = new HashMap<Arc, HashMap<Stop, IloNumVar>>();
		zvars = new HashMap<Arc, IloNumVar>();

		//x-variables
		System.out.println("initialize x-variables");
		for (Line l : i.getLines())
		{
			HashMap<Integer, IloIntVar> xvarLine = new HashMap<Integer, IloIntVar>();
			for (int f = l.minFreq; f <= l.maxFreq; f++)
			{
				IloIntVar x = cplex.boolVar();
				x.setName("L" + l.id + "_" + f);
				xvarLine.put(f, x);
			}
			xvars.put(l, xvarLine);
		}

		//y-variables
		System.out.println("initialize y-variables");
		
		for (Arc a : i.getArcs())
		{
			HashMap<Stop, IloNumVar> yvar = new HashMap<Stop, IloNumVar>();
			for (Stop s : i.getStops())
			{		
				IloNumVar y = cplex.numVar(0, s.demandFromThisOrigin);
				y.setName("oS" + s.id + "_" + a.type);
				yvar.put(s, y);					
			}
			yvars.put(a, yvar);
		}

		//z-variables
		if (Settings.SHORTTRANSFERS)
		{
			System.out.println("initialize z-variables");

			for (Arc a : i.getArcs())
			{
				if (a.type != Arc.Type.TRANSF) continue;
				IloNumVar zvar = cplex.boolVar();
				zvar.setName("z");
				zvars.put(a, zvar);
			}
		}


	}

	/** initialize the constraints
	 * @throws IloException
	 */
	private void initConstraints() throws IloException
	{
		initXconstraints();
		initYconstraints();
		initZconstraints();
	}

	/** initialize the x-constraints (line-frequency)
	 * @throws IloException
	 */
	private void initXconstraints() throws IloException 
	{
		//x-variables
		System.out.println("x-constraints");
		for (Line l : i.getLines())
		{
			IloLinearIntExpr expr = cplex.linearIntExpr();
			for (IloIntVar x : xvars.get(l).values())
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
				for (Map.Entry<Integer, IloIntVar> x : xvars.get(l).entrySet())
				{
					expr.addTerm(l.costs, x.getValue());
				}
			}
			cplex.addLe(expr, Settings.MAXLINECOSTS);

		}

		for (List<Line> list : i.getIncompatibleLines())
		{
			IloLinearNumExpr expr = cplex.linearNumExpr();

			for (Line line : list)
			{
				for (Map.Entry<Integer, IloIntVar> x : xvars.get(line).entrySet())
				{
					expr.addTerm(1, x.getValue());
				}

				cplex.addLe(expr, list.size() - 1);
			}
		}

	}

	/** initialize the flow constraints y
	 * @throws IloException
	 */
	private void initYconstraints() throws IloException
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
					expr.addTerm(1, yvars.get(a).get(s));
				}
				for (Arc a : v.getArcsOut())
				{
					expr.addTerm(-1, yvars.get(a).get(s));
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
						exprLeft.addTerm(1, yvars.get(a).get(o));
					}

					for (Entry<Integer, IloIntVar> lf : xvars.get(l).entrySet())
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
			for (Map.Entry<Integer, IloIntVar> x : xvars.get(l).entrySet())
			{
				List<Arc> arcsLine = new ArrayList<Arc>();
				arcsLine.addAll(l.backwardArcs);
				arcsLine.addAll(l.forwardArcs);
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
								exprLeft.addTerm(1, yvars.get(a).get(origin));
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

	/** initialize short transfer constraints
	 * @throws IloException
	 */
	private void initZconstraints() throws IloException 
	{
		this.shortTransferConstraints = new HashMap<Arc, IloConstraint>();
		if (Settings.SHORTTRANSFERS && !Settings.LAGRANGIANRELAXATION)
		{
			System.out.println("initialize z-constraints");
			for (Arc a : i.getArcs())
			{
				IloLinearNumExpr exprLeft = cplex.linearNumExpr();
				IloLinearNumExpr exprRight = cplex.linearNumExpr();
				if (a.type == Arc.Type.TRANSF)
				{
					for (Stop s : i.getStops())
					{
						exprLeft.addTerm(1, yvars.get(a).get(s));
					}
					exprRight.addTerm(Settings.BIGM, zvars.get(a));
					IloConstraint z = cplex.addLe(exprLeft, exprRight);
					z.setName("transferArc");
					this.shortTransferConstraints.put(a, z);
				}
			}
		}
	}

	/** initialize constraints to exclude cycles
	 * @param cycles
	 * @throws IloException
	 */
	private void initZexcludedArcs(List<Cycle> cycles) throws IloException
	{
		if (cycles != null)
		{
			for (Cycle cycle : cycles)
			{
				IloLinearNumExpr expr = cplex.linearNumExpr();
				for (Arc a : cycle.getArcs())
				{
					expr.addTerm(1, zvars.get(a));
				}
				IloRange r = cplex.addLe(expr, cycle.getArcs().size() - 1);
				r.setName("cycle");
				excludedCycles.add(cycle);
			}

		}
	}


	/** initialize the objective function
	 * @throws IloException
	 */
	private void initObjective() throws IloException
	{
		minTravelTime = cplex.linearNumExpr();
		for (Arc a : i.getArcs())
		{
			for (IloNumVar yvar : yvars.get(a).values())
			{
				minTravelTime.addTerm(a.value, yvar);
			}
		}


		if (Settings.LAGRANGIANRELAXATION)
		{
			for (Arc a : i.getArcs())
			{

				if (a.type == Arc.Type.TRANSF)
				{
					HashMap<Stop, IloNumVar> yvarsStop = yvars.get(a);
					for (IloNumVar y : yvarsStop.values())
					{
						minTravelTime.addTerm(1, y);
					}

					minTravelTime.addTerm(-Settings.BIGM, zvars.get(a));
				}
			}


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
							exprLeft.addTerm(1, yvars.get(a).get(o));
						}

						for (Entry<Integer, IloIntVar> lf : xvars.get(l).entrySet())
						{
							exprRight.addTerm(-1 * l.capacity * lf.getKey(), lf.getValue());
						}
						minTravelTime.add(exprLeft);
						minTravelTime.add(exprRight);
					}
				}
			}

			for (Line l : i.getLines())
			{
				for (Map.Entry<Integer, IloIntVar> x : xvars.get(l).entrySet())
				{
					List<Arc> arcsLine = new ArrayList<Arc>();
					arcsLine.addAll(l.backwardArcs);
					arcsLine.addAll(l.forwardArcs);
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
									exprLeft.addTerm(1, yvars.get(a).get(origin));
								}
							}
							exprRight.addTerm(-1 * l.capacity, x.getValue());
							minTravelTime.add(exprLeft);
							minTravelTime.add(exprRight);
						}
					}
				}
			}
		}


		minLineCosts = cplex.linearNumExpr();
		for (Line l : i.getLines())
		{
			for (Map.Entry<Integer, IloIntVar> x : xvars.get(l).entrySet()) 
			{
				minLineCosts.addTerm(l.costs, x.getValue());
			}
		}


		if (Settings.MINTRAVELTIME)
		{
			cplex.addMinimize(minTravelTime);
			return;
		}
		if (Settings.MINLINECOSTS)
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
			return cplex.getValue(minLineCosts);
		} 
		catch (IloException e) 
		{
			e.printStackTrace();
		}
		return 0;
	}

	/** generates solution and inserts some statistics to this solution
	 * @param iteration
	 * @param itDuration
	 * @param duration
	 * @return solution object
	 * @throws IloException
	 */
	public Solution generateSolution(int iteration, long itDuration, long duration) throws IloException
	{
		//x-variables
		HashMap<Line, Boolean> lines = new LinkedHashMap<Line, Boolean>();
		HashMap<Line, Integer> frequencies = new LinkedHashMap<Line, Integer>();
		outerloop:
			for (Line l : i.getLines())
			{
				for (Entry<Integer, IloIntVar> entry : xvars.get(l).entrySet())
				{
					int value = (int) Math.round(cplex.getValue(entry.getValue()));
					if (value > 0)
					{
						lines.put(l, true);
						frequencies.put(l, entry.getKey());
						continue outerloop;
					}
				}
				lines.put(l, false);

			}

		//y-variables
		int nrFRPLATF = 0;
		int nrTRANSF = 0;
		HashMap<Arc, HashMap<Stop, Integer>> arcs = new LinkedHashMap<Arc, HashMap<Stop, Integer>>();
		for (Arc a : i.getArcs())
		{
			HashMap<Stop, Integer> flowPerOrigin = new HashMap<Stop, Integer>();
			for (Map.Entry<Stop, IloNumVar> entry : yvars.get(a).entrySet())
			{
				int value = (int) Math.round(cplex.getValue(entry.getValue()));
				flowPerOrigin.put(entry.getKey(), value);
				//if (a.value == 0) continue; //skip in, out and toplatform arcs
				if (value != 0)
				{
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
			arcs.put(a, flowPerOrigin);
		}

		//z-variables
		HashMap<Arc, Boolean> transferArcs = new LinkedHashMap<Arc, Boolean>();
		if (Settings.SHORTTRANSFERS)
		{
			for (Arc a : i.getArcs())
			{
				if (a.type != Arc.Type.TRANSF) continue;
				int value = (int) Math.round(cplex.getValue(zvars.get(a)));
				if (value == 1)
				{
					transferArcs.put(a, true);
				}
				else
				{
					transferArcs.put(a, false);
				}

			}
		}
		double minTT = this.getMinTravelTimeObj();
		double minLC = this.getMinLineCostsObj();
		return new Solution(i, lines, frequencies, arcs, transferArcs, cplex.getObjValue(), minTT, minLC, nrFRPLATF, nrTRANSF, iteration, itDuration, duration);
	}

	/** solve the problem
	 * @param iteration
	 * @param initTime
	 * @return solution object
	 * @throws IloException
	 */
	public Solution solve(int iteration, long initTime) throws IloException
	{
		System.out.println("solving...");
		Solution sol = null;
		long startTime = System.nanoTime();
		if (cplex.solve())
		{
			long endTime = System.nanoTime();
			long duration = endTime - initTime; //since init
			long itDuration = endTime - startTime; //duration iteration
			sol = generateSolution(iteration, itDuration, duration);
			//exportModel(iteration);
		}
		else
		{
			System.err.println(cplex.getStatus());
		}
		return sol;
	}

	public Solution solveRelaxed(int iteration, long initTime) throws IloException
	{
		System.out.println("solving...");
		Solution sol = null;
		for (IloNumVar a : zvars.values())
		{

			cplex.add(cplex.conversion(a, IloNumVarType.Float));
		}
		long startTime = System.nanoTime();
		if (cplex.solve())
		{

			long endTime = System.nanoTime();
			long duration = endTime - initTime; //since init
			long itDuration = endTime - startTime; //duration iteration
			sol = generateSolution(iteration, itDuration, duration);
			exportModel(iteration);
		}
		else
		{
			System.err.println(cplex.getStatus());
		}
		return sol;
	}

	/** solve the problem with iteratively finding cycles
	 * @return
	 */
	public List<Solution> solveIteratively()
	{
		System.out.println("Solve iteratively");
		List<Solution> solutions = new ArrayList<Solution>();
		try
		{
			int it = 1;
			long startTime = System.nanoTime();
			Solution sol = solve(it, startTime);
			solutions.add(sol);


			if (sol != null)
			{
				List<Cycle> cycles = new ArrayList<Cycle>();
				List<Cycle> z = cyclesCheck(sol);
				cycles.addAll(z);
				sol.addCycles(cycles);
				sol.writeSummary();
				sol.writeSolutionIteration();

				while (!z.isEmpty())
				{
					it++;
					initZexcludedArcs(z);
					sol = solve(it, startTime);
					z = cyclesCheck(sol);
					cycles.addAll(z);
					sol.addCycles(cycles);
					sol.writeSummary();
					sol.writeSolutionIteration();
				}
				sol.writeFinalSolution();
			}
			else
			{
				System.err.println("No solution");
			}


			//isFeasible(solutions.get(solutions.size()-1));


			cplex.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}


		return solutions;
	}





//	private boolean isFeasible(Solution sol)
//	{
//		List<IloConstraint> constraints = new ArrayList<IloConstraint>();
//		boolean feasible = false; 
//		try {
//			for (Line l : i.getLines())
//			{
//				if (sol.frequencies.containsKey(l))
//				{
//					int f = sol.frequencies.get(l);
//					for (Entry<Integer, IloIntVar> entry : xvars.get(l).entrySet())
//					{
//						int val = f == entry.getKey() ? 1 : 0;
//						IloConstraint con = cplex.addEq(entry.getValue(), val);
//						constraints.add(con);
//					}
//				}
//				else
//				{
//					for (Entry<Integer, IloIntVar> entry : xvars.get(l).entrySet())
//					{
//						IloConstraint con = cplex.addEq(entry.getValue(), 0);
//						constraints.add(con);
//					}
//				}
//
//
//
//			}
//
//			for (Arc a : i.getArcs())
//			{
//				for (Map.Entry<Stop, IloNumVar> entry : yvars.get(a).entrySet())
//				{
//					int val = sol.arcs.get(a).get(entry.getKey());
//					IloConstraint con2 = cplex.addLe(entry.getValue(), val + 1);
//					constraints.add(con2);
//
//				}
//			}
//
//			//			if (Settings.SHORTTRANSFERS)
//			//			{
//			//				for (Arc a : i.getArcs())
//			//				{
//			//					if (a.type != Arc.Type.TRANSF) continue;
//			//					int val = sol.transferArcs.contains(a) ? 1 : 0;
//			//					IloConstraint con = cplex.addEq(zvars.get(a), val);
//			//					constraints.add(con);
//			//				}
//			//			}
//			if (feasible = cplex.solve())
//			{
//				System.out.println("This solution is feasible " + sol.iteration);
//			}
//			else
//			{
//				System.out.println("This solution is infeasible " + sol.iteration);
//			}
//
//			for (IloConstraint con : constraints)
//			{
//				cplex.remove(con);
//			}
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			return feasible;
//		} 
//		catch (IloException e) 
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return false;
//	}

	/** export cplex model to .lp files
	 * @param iteration
	 * @throws IloException
	 */
	private void exportModel(int iteration) throws IloException
	{
		String dirPath = "run/" + i.name + "/" + i.dateTime + "/";
		File directory = new File(dirPath);
		if (!directory.exists())
		{
			directory.mkdir();
		}
		cplex.exportModel(dirPath + "iteration" + iteration + ".lp");

	}

	
	/** finds cycles in the solution if possible
	 * @param sol
	 * @return list of invalid cycles
	 */
	private List<Cycle> cyclesCheck(Solution sol) 
	{
		EAN network = new EAN(sol, i);
		List<Cycle> cycles = new ArrayList<Cycle>();

		for (Stop transferStation : network.transferStations)
		{
			for (Event e : network.events)
			{
				if (e.stop == transferStation && e.type == Event.Type.DEP)
				{
					ShortestPath sp = new ShortestPath(network);
					int[] dist = sp.Dijkstra(e);
					//int min_length = Integer.MAX_VALUE;
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
									if (delta > Settings.DELTADEVIATION)
									{
										Cycle cycle = sp.getCycle(e, e2);
										cycle.setDelta(delta);
										cycle.setLength(length);
										if (cycle.transfers > 2)
										{
											cycle_candidate = cycle;
										}
									}			
								}
							}
						}
					}
					if (cycle_candidate != null)
					{
						if (!alreadyExists(cycles, cycle_candidate))
						{
							cycles.add(cycle_candidate);
						}
					}
				}

			}
		}

		return cycles;
	}

	/** check if cycle already exists
	 * @param cycles
	 * @param o
	 * @return
	 */
	private boolean alreadyExists(List<Cycle> cycles, Cycle o)
	{
		for (Cycle c : cycles)
		{
			if (new HashSet<>(c.activities).equals(new HashSet<>(o.activities))) return true;
		}
		return false;
	}
}

