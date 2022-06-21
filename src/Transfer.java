
public class Transfer 
{
	public final Line from;
	public final Line to;
	public final Stop stop;
	public final Arc arc;
	
	public Transfer(Line from, Line to, Stop stop, Arc arc)
	{
		this.from = from;
		this.to = to;
		this.stop = stop;
		this.arc = arc;
	}
}
