import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Main {

	//public static String path = "datasets\\athens\\basisreduced\\";
	public static String path = "datasets\\athens\\basis\\";
	//public static String path = "datasets\\toy\\basis\\";
	//public static String path = "datasets\\grid\\basis\\";

	public static void main(String[] args) throws Exception {
		 //TODO Auto-generated method stub

		
//		List<int[]> lines = new ArrayList<int[]>();
//
//		lines.add(new int[] {101, 102, 103, 104, 105});
//		lines.add(new int[] {201, 202, 203, 204, 205});
//		lines.add(new int[] {301, 302, 303, 304, 305});
//		lines.add(new int[] {401, 402, 403, 404, 405});
//		lines.add(new int[] {501, 502, 503, 504, 505});
//		lines.add(new int[] {101, 102, 202, 203, 303, 304, 404, 405, 505});
//		lines.add(new int[] {101, 201, 202, 302, 303, 403, 404, 504, 505});
//		lines.add(new int[] {501, 401, 402, 302, 303, 203, 204, 104, 105});
//		lines.add(new int[] {501, 502, 402, 403, 303, 304, 204, 205, 105});
//		lines.add(new int[] {101, 102});
//		lines.add(new int[] {102, 103});
//		lines.add(new int[] {104, 105});
//		lines.add(new int[] {101, 201, 301, 401, 501});
//		lines.add(new int[] {102, 202, 302, 402, 502});
//		lines.add(new int[] {103, 203, 303, 403, 503});
//		lines.add(new int[] {104, 204, 304, 404, 504});
//		lines.add(new int[] {105, 205, 305, 405, 505});
//		Experiment e = new Experiment(lines);
//		
//	
//		Instance i = new Instance(path, e);
//		i.printToFile();
//		i.generateLinePool(lines);
//		i.printLineFile();
//		Settings.setMaxLineCosts(7000);
//		Model m = new Model(i);
//		m.solveIteratively();
		
		
		for (int c = 40; c < 55; c++)
		{
			Instance i = new Instance(path);
			//i.print();
			i.printToFile();


			Settings.setMaxLineCosts(c*100);
			Model m = new Model(i);
			m.solveIteratively();
		}
		
//		for (int c = 6; c <= 10; c++)
//		{
//			Instance i = new Instance(path);
//			i.print();
//			i.printToFile();
//
//
//			Settings.setMaxLineCosts(c*1000);
//			Model m = new Model(i);
//			m.solveIteratively();
//		}
//
//		Settings.setMaxLineCosts(10000);
//		Instance i = new Instance(path);
//		i.print();
//		i.printToFile();

//
//		
//		Model m = new Model(i);
//		m.solveIteratively();
//		for (int i=5; i< 11; i++)
//		{
//			Settings.setMaxLineCosts(i*1000);
//			Experiment e = new Experiment(2);
//		}



	}



}
