import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import ilog.concert.IloException;

public class Run {

	enum Experiment {ATHENSREDUCED, GRID, TOY, RANDOM};
	public Experiment experiment;
	public Object[] args;
	public String path;
	
	public Run(Run.Experiment ex, Object...args)
	{
		this.experiment = ex;
		this.args = args;
		
		switch (ex)
		{
		case ATHENSREDUCED:
			path = "datasets\\athens\\basisreduced\\";
			break;
		case GRID:
			path = "datasets\\grid\\basis\\";
			break;
		case RANDOM:
			path = "";
			break;
		case TOY:
			path = "datasets\\toy\\basis\\";
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
			runGrid();
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
		default:
			break;
		
		}
	}





	private void runToy() 
	{
		for (int c = 6; c <= 10; c++)
		{
			Instance i = new Instance(path);
			i.print();
			i.printToFile();


			Settings.setMaxLineCosts(c*1000);
			Model m = new Model(i);
			m.solveIteratively();
		}		
	}

	private void runGrid() 
	{
		List<int[]> lines = new ArrayList<int[]>();

		lines.add(new int[] {101, 102, 103, 104, 105});
		lines.add(new int[] {201, 202, 203, 204, 205});
		lines.add(new int[] {301, 302, 303, 304, 305});
		lines.add(new int[] {401, 402, 403, 404, 405});
		lines.add(new int[] {501, 502, 503, 504, 505});
		lines.add(new int[] {101, 102, 202, 203, 303, 304, 404, 405, 505});
		lines.add(new int[] {101, 201, 202, 302, 303, 403, 404, 504, 505});
		lines.add(new int[] {501, 401, 402, 302, 303, 203, 204, 104, 105});
		lines.add(new int[] {501, 502, 402, 403, 303, 304, 204, 205, 105});
		lines.add(new int[] {101, 102});
		lines.add(new int[] {102, 103});
		lines.add(new int[] {104, 105});
		lines.add(new int[] {101, 201, 301, 401, 501});
		lines.add(new int[] {102, 202, 302, 402, 502});
		lines.add(new int[] {103, 203, 303, 403, 503});
		lines.add(new int[] {104, 204, 304, 404, 504});
		lines.add(new int[] {105, 205, 305, 405, 505});
		
		
	
		Instance i = new Instance(path, lines);
		i.printToFile();
		i.generateLinePool(lines);
		i.printLineFile(path);
		Settings.setMaxLineCosts(7000);
		Model m = new Model(i);
		m.solveIteratively();		
	}

	private void runAthens() {
		//c = maxlineCosts
		for (int c = 42; c < 55; c++)
		{
			Instance i = new Instance(path);
			i.print();
			i.printToFile();

			Settings.setMaxLineCosts(c);
			Model m = new Model(i);
			m.solveIteratively();
		}
		
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
		i.printToFile();
		Model m = new Model(i);
		m.solveIteratively();
	}
}
