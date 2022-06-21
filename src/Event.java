import java.util.*;

public class Event {

	enum Type {DEP, ARR};
	enum Direction {FORWARD, BACKWARD};
	public final Stop stop;
	public final Line line;
	public final Type type;
	public final Direction direction;
	public final List<Event> out;
	public final List<Event> in;
	
	public final List<Activity> outActivity;
	public final List<Activity> inActivity;
	
	public Event(Line l, Stop s, Type type, Direction direction)
	{
		this.stop = s;
		this.line = l;
		this.type = type;
		this.direction = direction;
		this.out = new ArrayList<Event>();
		this.in = new ArrayList<Event>();
		this.outActivity = new ArrayList<Activity>();
		this.inActivity = new ArrayList<Activity>();
	}
	
	public void addOut(Event e, Activity a)
	{
		this.out.add(e);
		this.outActivity.add(a);
	}
	
	public void addIn(Event e, Activity a)
	{
		this.in.add(e);
		this.inActivity.add(a);
	}

	public boolean hasNextStop(Stop s)
	{
		for (Event e : out)
		{
			if (e.stop == s)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean hasNextEvent(Event next)
	{
		for (Event e : out)
		{
			if (e == next)
			{
				return true;
			}
		}
		return false;
	}
	
	public Activity getOutActivity(Event e)
	{
		int idx = this.out.indexOf(e);
		return this.outActivity.get(idx);
	}
	
	public Activity getInActivity(Event e)
	{
		int idx = this.in.indexOf(e);
		return this.inActivity.get(idx);
	}
	
	@Override
	public String toString() {
		return "Event [stop=" + stop.id + ", line=" + line.id + ", " + type + "]";
	}
	
	
}
