import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Run {

	enum Experiment {ATHENSREDUCED, GRID, GRID4, TOY, RANDOM, TEST};
	public Experiment experiment;
	public Object[] args;
	public String name;
	public String path;
	
	public Run(Run.Experiment ex, Object...args)
	{
		this.experiment = ex;
		this.args = args;
		
		switch (ex)
		{
		case ATHENSREDUCED:
			name = "athens";
			path = "datasets\\athens\\basisreduced\\";
			break;
		case GRID:
			name = "grid";
			path = "datasets\\grid\\basis\\";
			break;
		case GRID4:
			name = "grid4x4";
			path = "datasets\\grid4x4\\basis\\";
			break;
		case RANDOM:
			name = "random";
			path = "";
			break;
		case TOY:
			name = "toy";
			path = "datasets\\toy\\basis\\";
			break;
		case TEST:
			name = "test";
			
			break;
		default:
			break;
		
		}
	}
	
	public void start()
	{

		switch (experiment)
		{
		case ATHENSREDUCED:
			runAthens();
			break;
		case GRID:
			name = "grid_8z_f" + Settings.MAXFREQUENCY;
			runGrid();
//			name = "grid_5h5v_f" + Settings.MAXFREQUENCY;
//			runGrid();
//			name = "grid_5h4d_f" + Settings.MAXFREQUENCY;
//			runGrid();
//			name = "grid_5v4d_f" + Settings.MAXFREQUENCY;
//			runGrid();
//			name = "grid_5h5v4d_f" + Settings.MAXFREQUENCY ;
//			runGrid();
			break;
		case GRID4:
			name = "grid4_4h4v_f" + Settings.MAXFREQUENCY;
			runGrid4();
			name = "grid4_4h4d_f" + Settings.MAXFREQUENCY;
			runGrid4();
			name = "grid4_4v4d_f" + Settings.MAXFREQUENCY;
			runGrid4();
			name = "grid4_4h4v4d_f" + Settings.MAXFREQUENCY;
			runGrid4();
			break;
		case RANDOM:
			if (args.length > 0)
			{
				if (args[0] instanceof Integer)
				{
					runRandom((int) args[0]);
				}
				else
				{
					System.err.println("Argument is not integer");
				}
			}
			else
			{
				System.err.println("No argument");

			}
			break;
		case TOY:
			runToy();
			break;
		case TEST:
			runTest();
			break;
		default:
			break;
		
		}
		
		
	}

	public void writeAllSolutions()
	{
		try 
		{
			String path = "run/" + name + "/";
			File directory = new File(path);
			if (!directory.exists())
			{
				directory.mkdir();
			}
			//Write results overall file
			Writer output;
			output = new BufferedWriter(new FileWriter(path + "all_solutions.txt", true));
			List<String> outputStr = new ArrayList<String>();
			outputStr.add("date");
			outputStr.add("name");
			outputStr.add("objectiveValue");
			outputStr.add("minTravelTime");
			outputStr.add("minLineCosts");
			outputStr.add("iteration");
			outputStr.add("#selectedLines");
			outputStr.add("#transferArcs");
			outputStr.add("#transferArcsUsed");
			outputStr.add("#cycles");
			outputStr.add("#events");
			outputStr.add("#activities");
			outputStr.add("#nrFRPLATF");
			outputStr.add("#nrTOPLATF");
			outputStr.add("MAXLINECOSTS");
			outputStr.add("time");

			String join = String.join(", ", outputStr) + "\n";
			output.append(join);
			output.close();
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}



	private void runToy() 
	{
		for (int c = 6; c <= 10; c++)
		{
			Instance i = new Instance(name, path);
			i.print();


			Settings.setMaxLineCosts(c*1000);
			Model m = new Model(i);
			m.solveIteratively();
		}		
	}

	private void runGrid() 
	{
		writeAllSolutions();

		List<Integer> lIds = new ArrayList<Integer>();
		List<int[]> lines = new ArrayList<int[]>();

		if (name.contains("5h"))
		{
			lines.add(new int[] {101, 102, 103, 104, 105});
			lines.add(new int[] {201, 202, 203, 204, 205});
			lines.add(new int[] {301, 302, 303, 304, 305});
			lines.add(new int[] {401, 402, 403, 404, 405});
			lines.add(new int[] {501, 502, 503, 504, 505});
			lIds.add(1);
			lIds.add(2);
			lIds.add(3);
			lIds.add(4);
			lIds.add(5);
		}

		if (name.contains("5v"))
		{
			lines.add(new int[] {101, 201, 301, 401, 501});
			lines.add(new int[] {102, 202, 302, 402, 502});
			lines.add(new int[] {103, 203, 303, 403, 503});
			lines.add(new int[] {104, 204, 304, 404, 504});
			lines.add(new int[] {105, 205, 305, 405, 505});
			lIds.add(6);
			lIds.add(7);
			lIds.add(8);
			lIds.add(9);
			lIds.add(10);
		}

		if (name.contains("4d"))
		{
			lines.add(new int[] {201, 202, 203, 303, 403, 404, 405});
			lines.add(new int[] {102, 202, 302, 303, 304, 404, 504});
			lines.add(new int[] {401, 402, 403, 303, 203, 204, 205});
			lines.add(new int[] {502, 402, 302, 303, 304, 204, 104});
			lIds.add(11);
			lIds.add(12);
			lIds.add(13);
			lIds.add(14);
		}
		
		if (name.contains("8l"))
		{
			lines.add(new int[] {101, 102, 103, 104, 105, 205, 305, 405, 505});
			lines.add(new int[] {201, 202, 203, 204, 205, 305, 405, 505});
			lines.add(new int[] {301, 302, 303, 304, 305, 405, 505});
			lines.add(new int[] {401, 402, 403, 404, 405, 505});
			lines.add(new int[] {101, 201, 202, 203, 204, 205});
			lines.add(new int[] {101, 201, 301, 302, 303, 304, 305});
			lines.add(new int[] {101, 201, 301, 401, 402, 403, 404, 405});
			lines.add(new int[] {101, 201, 301, 401, 501, 502, 503, 504, 505});
			lIds.add(15);
			lIds.add(16);
			lIds.add(17);
			lIds.add(18);
			lIds.add(19);
			lIds.add(20);
			lIds.add(21);
			lIds.add(22);
		}
		
		if (name.contains("8z"))
		{
			lines.add(new int[] {101, 102, 103, 203, 303, 403, 503, 504, 505});
			lines.add(new int[] {201, 202, 203, 303, 403, 404, 405});
			lines.add(new int[] {401, 402, 403, 303, 203, 204, 205});
			lines.add(new int[] {501, 502, 503, 403, 303, 203, 103, 104, 105});
			lines.add(new int[] {101, 201, 301, 302, 303, 304, 305, 405, 505});
			lines.add(new int[] {102, 202, 302, 303, 304, 404, 405});
			lines.add(new int[] {104, 204, 304, 303, 302, 402, 502});
			lines.add(new int[] {501, 401, 301, 302, 303, 304, 305, 205, 105});
			lIds.add(23);
			lIds.add(24);
			lIds.add(25);
			lIds.add(26);
			lIds.add(27);
			lIds.add(28);
			lIds.add(29);
			lIds.add(30);
		}
		
		List<int[]> incompatibles = new ArrayList<int[]>();
//		incompatibles.add(new int[] {2, 11, 13});
//		incompatibles.add(new int[] {4, 11, 13});
//		incompatibles.add(new int[] {7, 12, 14});
//		incompatibles.add(new int[] {9, 12, 14});
		
	
		for (int c = 5; c <= lines.size(); c++)
		{
			System.out.println("c = " + c);
			Instance i = new Instance(name, path, lIds, lines, incompatibles);
			i.generateLinePool(lines);
			i.printLineFile(path);
			Settings.setMaxLineCosts(c*10);
			Model m = new Model(i);
			m.solveIteratively();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}		
		
	}
	
	public List<int[]> incompatibilityCheck(List<int[]> lIds, List<int[]> incs)
	{
		List<int[]> newIncs = new ArrayList<int[]>();
		outerloop:
		for (int[] inc : incs)
		{
			for (int i = 0; i < inc.length; i++)
			{
				if (!lIds.contains(inc[i]))
				{
					continue outerloop;
				}
			}
			newIncs.add(inc);
		}
		return newIncs;
	}
	
	private void runGrid4() 
	{
		writeAllSolutions();

		List<Integer> lIds = new ArrayList<Integer>();
		List<int[]> lines = new ArrayList<int[]>();

		if (name.contains("4h"))
		{
			lines.add(new int[] {101, 102, 103, 104});
			lines.add(new int[] {201, 202, 203, 204});
			lines.add(new int[] {301, 302, 303, 304});
			lines.add(new int[] {401, 402, 403, 404});
			lIds.add(1);
			lIds.add(2);
			lIds.add(3);
			lIds.add(4);
		}

		if (name.contains("4v"))
		{
			lines.add(new int[] {101, 201, 301, 401});
			lines.add(new int[] {102, 202, 302, 402});
			lines.add(new int[] {103, 203, 303, 403});
			lines.add(new int[] {104, 204, 304, 404});
			lIds.add(5);
			lIds.add(6);
			lIds.add(7);
			lIds.add(8);
		}
		
		if (name.contains("4d"))
		{
			lines.add(new int[] {101, 102, 202, 203, 303, 304, 404});
			lines.add(new int[] {101, 201, 202, 302, 303, 403, 404});
			lines.add(new int[] {401, 301, 302, 303, 203, 204, 104});
			lines.add(new int[] {401, 402, 302, 303, 304, 204, 104});
			lIds.add(9);
			lIds.add(10);
			lIds.add(11);
			lIds.add(12);
		}
		
		List<int[]> incompatibles = new ArrayList<int[]>();

		for (int c = 5; c <= lines.size(); c++)
		{
			Instance i = new Instance(name, path, lIds, lines, incompatibles);
			i.generateLinePool(lines);
			i.printLineFile(path);
			Settings.setMaxLineCosts(c*10);
			Model m = new Model(i);
			m.solveIteratively();		
		}		
		
	}

	private void runAthens() {
		//c = maxlineCosts
		
		writeAllSolutions();
		for (int c = 42; c < 55; c++)
		{
			Instance i = new Instance(name, path);
			i.print();

			Settings.setMaxLineCosts(c);
			Model m = new Model(i);
			m.solveIteratively();
		}
		
	}

	
	private void runTest() 
	{
		path = "datasets\\grid4x4\\basis\\";
	
		
	}
	
	public void runRandom(int nr)
	{
		System.out.println("Random experiment" + nr);
		List<int[]> lines = new ArrayList<int[]>();

		switch (nr)
		{
		case 0 :
			lines.add(new int[] {0, 1});
			lines.add(new int[] {1, 2});
			lines.add(new int[] {2, 0});

			break;
		case 1 :
			lines.add(new int[] {0, 2, 5, 8});
			//lines.add(new int[] {1, 2, 3, 6, 9, 8, 7, 4, 1});
			lines.add(new int[] {1, 2, 4, 5});
			lines.add(new int[] {4, 5, 8, 7});
			lines.add(new int[] {2, 3, 6, 5});
			lines.add(new int[] {5, 6, 9, 8});

			break;
		case 2 : 
			//5x5 grid with cross
			lines.add(IntStream.rangeClosed(0, 4).toArray());
			lines.add(IntStream.rangeClosed(5, 9).toArray());
			lines.add(IntStream.rangeClosed(10, 14).toArray());
			lines.add(IntStream.rangeClosed(15, 19).toArray());
			lines.add(IntStream.rangeClosed(20, 24).toArray());
			lines.add(new int[] {0, 5, 10, 15, 20});
			lines.add(new int[] {1, 6, 11, 16, 21});
			lines.add(new int[] {2, 7, 12, 17, 22});
			lines.add(new int[] {3, 8, 13, 18, 23});
			lines.add(new int[] {4, 9, 14, 19, 24});
			lines.add(new int[] {0, 6, 12, 18, 24});
			lines.add(new int[] {4, 8, 12, 16, 20});
			//			lines.add(new int[] {0, 5});
			//			lines.add(new int[] {5, 10});
			//			lines.add(new int[] {10, 15});
			//			lines.add(new int[] {15, 20});
			break;

		case 3 : 
			//4x4 grid with cross
			lines.add(IntStream.rangeClosed(0, 3).toArray());
			lines.add(IntStream.rangeClosed(4, 7).toArray());
			lines.add(IntStream.rangeClosed(8, 11).toArray());
			lines.add(IntStream.rangeClosed(12, 15).toArray());
			lines.add(new int[] {0, 4, 8, 12});
			lines.add(new int[] {1, 5, 9, 13});
			lines.add(new int[] {2, 6, 10, 14});
			lines.add(new int[] {3, 7, 11, 15});
			break;
		case 4 :
			lines.add(IntStream.rangeClosed(0, 3).toArray());
			lines.add(IntStream.rangeClosed(4, 7).toArray());
			lines.add(IntStream.rangeClosed(8, 11).toArray());
			lines.add(IntStream.rangeClosed(12, 15).toArray());
			lines.add(new int[] {16, 0, 4, 8, 12});
			lines.add(new int[] {1, 2, 3, 18});
			lines.add(new int[] {4, 5, 6, 7});
			lines.add(new int[] {9, 10});
			lines.add(new int[] {1, 5, 9, 13});
			lines.add(new int[] {2, 6, 10});
			lines.add(new int[] {3, 7, 11, 15, 17});
			lines.add(new int[] {7, 21});
			lines.add(new int[] {19, 12, 13});
			lines.add(new int[] {19, 12, 13, 14, 15, 20});


			break;
		case 5: 
			lines.add(new int[] {0, 1, 5});
			lines.add(new int[] {6, 2, 3});
			lines.add(new int[] {4, 0, 2});
			lines.add(new int[] {1, 3, 7});
			break;
		case 6: 
			//			lines.add(new int[] {0, 1, 2});
			//			lines.add(new int[] {3, 4, 5});
			//			lines.add(new int[] {6, 7, 8});
			lines.add(new int[] {0, 1});
			lines.add(new int[] {1, 2});
			lines.add(new int[] {3, 4});
			lines.add(new int[] {4, 5});
			lines.add(new int[] {6, 7});
			lines.add(new int[] {7, 8});
			lines.add(new int[] {0, 3});
			lines.add(new int[] {3, 6});
			lines.add(new int[] {1, 4});
			lines.add(new int[] {4, 7});
			lines.add(new int[] {2, 5});
			lines.add(new int[] {5, 8});
			;
			break;

		default :
			System.err.println("Experiment number unknown");
			return;
		}
		run(nr, lines);
	}


	public void run(int nr, List<int[]> lines)
	{
		Generator g = new Generator("exp_" + nr, lines);
		Instance i = g.getInstance();
		i.print();
		Model m = new Model(i);
		m.solveIteratively();
	}
}
