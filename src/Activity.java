
public class Activity {

	enum Type {DRIVE, DWELL, TRANSFER}
	public final Type type;
	public final Event from;
	public final Event to;
	public Arc arc;
	public final int value;

	public Activity(Event from, Event to, Type type, Arc... arcs)
	{
		this.type = type;
		this.from = from;
		this.to = to;
		this.from.addOut(to, this);
		this.to.addIn(from, this);
		if (arcs.length > 0)
		{
			arc = arcs[0];
		}
		
		switch (type)
		{
		case DRIVE :
			this.value = arc.value;
			break;
		case DWELL :
			this.value = 0;
			break;
		case TRANSFER :
			this.value = Settings.FASTTRANSFERTIME;
			break;
		default :
			this.value = 0;
			System.out.println("Unknown Activity type");
			break;
		}


	}

	@Override
	public String toString() {
		switch (type)
		{
		case DRIVE :
			return "Activity [" + type + " L" + from.line.id + ", from=" + from.stop.id + ", to=" + to.stop.id + ", value=" + value + "]"; 
		case DWELL :
			return "Activity [" + type + " L" + from.line.id + ", stop=" + from.stop.id + ", value=" + value + "]"; 
		case TRANSFER :
			return "Activity [" + type + " L" + from.line.id + ", stop=" + from.stop.id + ", value=" + value + "]"; 
		}
		return "";
	}
	
}
