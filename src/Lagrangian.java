import ilog.concert.IloException;

public class Lagrangian {

	private Model m;
	private double[] lambda;
	public Lagrangian(Model m)
	{
		this.m = m;
		this.lambda = new double[0];
		
	}
	
	public void start()
	{
		int iteration = 0;
		try {
			Solution s = m.solve(iteration, 0);
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
