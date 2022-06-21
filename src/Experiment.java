import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import ilog.concert.IloException;

public class Experiment {

	public List<int[]> lines;
	public Experiment(List<int[]> lines)
	{
		this.lines = lines;
	}
	
	public List<int[]> getLinesWithStops()
	{
		return lines;
	}



	public Experiment(int...exps)
	{
		for (int i = 0; i < exps.length; i++)
		{
			System.out.println("Experiment" + exps[i]);
			runExperiment(exps[i]);
		}

	}

	public void runExperiment(int nr)
	{
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
		try {
			Generator g = new Generator("exp_" + nr, lines);
			Instance i = g.getInstance();
			i.print();
			i.printToFile();
			Model m = new Model(i);
			m.solveIteratively();
		} 
		catch (IloException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
