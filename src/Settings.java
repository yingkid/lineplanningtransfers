import java.lang.reflect.Field;
import java.util.*;

public class Settings {
	
	public static final Random random = new Random(1);
	public static final double CPLEXMIPGAP = 1e-7;
	
	public static final int FASTTRANSFERTIME = 2;
	public static final int CYCLEPERIOD = 60;
	public static final int DELTADEVIATION = 3;
	public static final int LINECAPACITY = 1000;
	
	
	public static final boolean INCLUDETOTALLINECOSTS = true;
	public static final boolean SHORTTRANSFERS = true;
	public static final boolean MINTRAVELTIME = true;
	public static final boolean MINLINECOSTS = false;
	public static final int LONGTRANSFERCOSTSFACTOR = 1;
	public static final boolean LAGRANGIANRELAXATION = false;
	public static final int LINECOSTS = 10;
	public static final int MAXFREQUENCY = 4;
	public static int MAXLINECOSTS = 45;
	public static int BIGM = 0;
	
	public static void setMaxLineCosts(int val)
	{
		MAXLINECOSTS = val;
	}
	
	
	
	public static void setBIGM(int bIGM) {
		BIGM = bIGM;
	}



	public static String values() 
	{
		try
		{
			List<String> str = new ArrayList<String>();
			Field[] fields = Settings.class.getFields();

			for (Field f : fields)
			{
				Class<?> t = f.getType();
				if (t == int.class)
				{
					str.add(f.getName() + " " + f.getInt(null));
				}
				if (t == boolean.class)
				{
					str.add(f.getName() + " " + f.getBoolean(null));
				}
				
				
			}
			return String.join("\n", str);
		}
		catch (Exception e)
		{
			return "";
		}
		
	}
}
