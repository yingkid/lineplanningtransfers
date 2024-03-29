import java.util.*;

public class Stop {
	
	public final int id;
	public final String shortName;
	public final String longName;
	public final double x;
	public final double y;
	public final Vertex in;
	public final Vertex out;
	public Vertex platform;
	public final HashMap<Line, Vertex> lines;
	public final List<Vertex> vertices;
	public int demandFromThisOrigin;
	
	/** stop in the ptn
	 * @param id
	 * @param shortName
	 * @param longName
	 * @param x
	 * @param y
	 */
	public Stop(int id, String shortName, String longName, double x, double y) {
		super();
		this.id = id;
		this.shortName = shortName.trim();
		this.longName = longName.trim();
		this.x = x;
		this.y = y;
		this.vertices = new ArrayList<Vertex>();
		
		this.in = new Vertex(this, Vertex.Type.IN);
		this.out = new Vertex(this, Vertex.Type.OUT);
		this.lines = new LinkedHashMap<Line, Vertex>();
	}
	
	/** add platform vertex for long transfers
	 * @param platformVertex
	 */
	public void addPlatformVertex(Vertex platformVertex)
	{
		this.platform = platformVertex;
	}
	
	/** add relatex vertex to this stop
	 * @param v
	 */
	public void addVertex(Vertex v) 
	{
		this.vertices.add(v);
	}

	@Override
	public String toString() {
		//return "Stop [id=" + id + ", shortName=" + shortName + ", x=" + x + ", y=" + y + "]";
		
		return String.format(Locale.US, "Stop [id=%2d, shortName=%s, x=%.2f, y=%.2f]", id, shortName, x, y);
	}

}
