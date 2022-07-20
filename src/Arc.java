public class Arc {

	enum Type {IN, OUT, TRAVEL, TOPLAT, FRPLAT, TRANSF}
	enum Direction {FORWARD, BACKWARD}
	public final Vertex from;
	public final Vertex to;
	public final Type type;
	public Direction direction;
	public final int value;
	public int freq;		//long transfer arcs
	public Line line;
	
	/**
	 * An arc from passengener flow model
	 * @param from vertex
	 * @param to vertex
	 * @param type see enum
	 * @param value costs of the arc
	 * @param obj other objects 
	 */
	public Arc(Vertex from, Vertex to, Arc.Type type, int value, Object...obj)
	{
		this.from = from;
		this.to = to;
		this.type = type;
		this.value = value;
		if (type == Arc.Type.FRPLAT)
		{
			this.freq = (int) obj[0];
		}
		if (type == Arc.Type.TRAVEL)
		{
			this.line = (Line) obj[0];
			this.direction = (Arc.Direction) obj[1];
		}
		
		from.addArcsOut(this);
		to.addArcsIn(this);
		from.vertexTo.add(to);
		to.vertexFrom.add(from);
	}

	@Override
	public String toString() {
		//return "A[from=" + from + ", to=" + to + ", type=" + type + ", value=" + value + "]";
		String typeString;
		switch (type)
		{
		case FRPLAT:
			typeString = "S" + this.from.stop.id + " L" + this.to.line.id;
			break;
		case IN:
			typeString = String.format("S%-2d  --->L%-2d", this.from.stop.id, this.to.line.id);
			break;
		case OUT:
			typeString = String.format("S%-2d L%-2d->--", this.to.stop.id, this.from.line.id);
			break;
		case TOPLAT:
			typeString = "S" + this.from.stop.id;
			break;
		case TRANSF:
			//typeString = this.from.line.shortString() + "->" + this.to.line.shortString() + " S" + this.from.st
			typeString = String.format("S%-2d L%-2d->L%-2d", this.from.stop.id, this.from.line.id, this.to.line.id);
			break;
		case TRAVEL:
			String dir = direction.toString().substring(0,1);
			typeString = String.format("L%-2d S%-2s->S%-2s %1s", this.from.line.id, this.from.stop.id, this.to.stop.id, dir);
			break;
		default:
			System.err.println("Arc.toString() error");
			typeString = "";
			break;
		
		}
		return String.format("Arc(%-6s %-12s value=%3d)", type, typeString, value);
	}
	
	
}
