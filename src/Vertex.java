import java.util.*;

public class Vertex {

	enum Type {IN, OUT, LINE, PLAT};
	
	public final Stop stop;
	public final Type type;
	public Line line;
	private List<Arc> arcsOut = new ArrayList<Arc>(); //outgoing arcs
	private List<Arc> arcsIn = new ArrayList<Arc>(); //ingoing arcs
	public List<Vertex> vertexTo = new ArrayList<Vertex>();
	public List<Vertex> vertexFrom = new ArrayList<Vertex>();
	public static List<Vertex> all = new ArrayList<Vertex>(); 
	
	
	public Vertex(Stop s, Vertex.Type type, Line...line)
	{
		this.stop = s;
		this.type = type;
		if (type == Vertex.Type.LINE)
		{
			this.line = line[0];
		}
		all.add(this);
		s.addVertex(this);
	}
	
	public void addArcsOut(Arc arc)
	{
		this.arcsOut.add(arc);
		
	}
	
	public void addArcsIn(Arc arc)
	{
		this.arcsIn.add(arc);
	}
	
	public List<Arc> getArcsOut() {
		return arcsOut;
	}

	public List<Arc> getArcsIn() {
		return arcsIn;
	}

	@Override
	public String toString() {
		if (line != null)
		{
			return String.format("V(S%-4d %-4s%4d)", stop.id, type.toString().toLowerCase(), line.id);
		}
		return String.format("V(S%-4d %-4s ---)", stop.id, type.toString().toLowerCase());
		
	}
	

}
