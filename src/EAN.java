import java.util.*;
import java.util.Map.Entry;


public class EAN {

	private List<Stop> stops;
	private List<Transfer> transfers;
	public List<Stop> transferStations;
	public List<Event> events;
	public List<Activity> activities;
	public boolean[][] adjacencyMatrix;
	public Activity[][] activityMatrix;

	/** creates EAN from solution and instance
	 * @param sol solution
	 * @param i instance
	 */
	public EAN(Solution sol, Instance i)
	{
		this.events = new ArrayList<Event>();
		this.activities = new ArrayList<Activity>();
		this.stops = i.getStops();
		this.transfers = generateTransfers(sol);
		generateEAforLines(sol);
		generateTransferActivities(sol);

		sol.ean = this;
		System.out.println("Size events:" + events.size());
		System.out.println("Size activities:" + activities.size());

	}

	/** generates transfer arcs in the EAN
	 * @param sol solution
	 * @return
	 */
	private List<Transfer> generateTransfers(Solution sol)
	{
		System.out.println("EAN: generate transfers");
		List<Transfer> transfers = new ArrayList<Transfer>();
		for (Arc a : sol.getTransferArcs())
		{
			Transfer t = new Transfer(a.from.line, a.to.line, a.from.stop, a);
			transfers.add(t);
		}
		return transfers;
	}

	/** generates events and activities for every line
	 * @param sol solution
	 */
	private void generateEAforLines(Solution sol)
	{
		System.out.println("EAN: generate events and activities for lines");
		for (Entry<Line, Boolean> entry : sol.lines.entrySet())
		{
			if (entry.getValue())
			{
				Line l = entry.getKey();
				//create Event and Activities for line in forward and backward direction
				createEAforLine(l, Event.Direction.FORWARD);
				createEAforLine(l, Event.Direction.BACKWARD);
			}

		}
	}

	/** generates transfer activities
	 * @param sol solution
	 */
	private void generateTransferActivities(Solution sol)
	{
		System.out.println("EAN: generate transfer activities");
		List<Stop> allStopLines = new ArrayList<Stop>();
		for (Entry<Line, Boolean> entry : sol.lines.entrySet())
		{
			if (entry.getValue())
			{
				Line l = entry.getKey();
				allStopLines.addAll(l.stops);
			}
		}

		transferStations = new ArrayList<Stop>();
		for (Stop s : stops)
		{
			if (Collections.frequency(allStopLines, s) > 1)
			{
				transferStations.add(s);
			}
		}

		for (Stop s : transferStations)
		{
			List<Event> arrivals = new ArrayList<Event>();
			List<Event> departures = new ArrayList<Event>();

			for (Event e : events)
			{
				if (e.stop == s)
				{
					switch (e.type)
					{
					case ARR :
						arrivals.add(e);
						break;
					case DEP :
						departures.add(e);
						break;
					}					
				}
			}

			for (Event arr : arrivals)
			{
				for (Event dep : departures)
				{
					if (arr.line != dep.line)
					{
						Transfer t = null;
						if ((t = checkTransfer(arr, dep)) != null)
						{
							Activity a = new Activity(arr, dep, Activity.Type.TRANSFER, t.arc);
							activities.add(a);
						}
						else
						{
							//System.out.println("Transfer not offered " + arr.line + " to " + dep.line + " at " + arr.stop);
						}


					}
				}
			}
		}


		int size = events.size();

		activityMatrix = new Activity[size][size];
		for (Activity a : activities)
		{
			int index_from = events.indexOf(a.from);
			int index_to = events.indexOf(a.to);
			activityMatrix[index_from][index_to] = a;
		}

	}

	/** find whether there is a transfer between two events
	 * @param arr
	 * @param dep
	 * @return transfer if found
	 */
	private Transfer checkTransfer(Event arr, Event dep)
	{
		Stop s = arr.stop;
		for (Transfer t : transfers)
		{
			if (t.from == arr.line && t.to == dep.line && t.stop == s)
			{
				return t;
			}
		}

		return null;

	}

	/** creates events and activities for given line in the given direction
	 * @param l line
	 * @param direction
	 */
	private void createEAforLine(Line l, Event.Direction direction)
	{
		List<Stop> stops = new ArrayList<Stop>();
		stops.add(l.forwardArcs.get(0).from.stop);
		for (Arc a : l.forwardArcs)
		{
			stops.add(a.to.stop);
		}
		//for loop for line in both directions
		Event dep = null;
		Event arr = null;
		int i_start = 0;
		int j_start = 1;
		boolean check = true;
		if (direction == Event.Direction.BACKWARD)
		{
			i_start = stops.size() - 1;
			j_start = stops.size() - 2;
		}

		for (int i = i_start, j = j_start; check;)
		{
			dep = new Event(l, stops.get(i), Event.Type.DEP, direction);
			if (i != i_start) 
			{
				Activity dwell = new Activity(arr, dep, Activity.Type.DWELL);
				activities.add(dwell);
			}
			arr = new Event(l, stops.get(j), Event.Type.ARR, direction);
			events.add(dep);
			events.add(arr);

			List<Arc> arcFromList = direction == Event.Direction.FORWARD ? l.forwardArcs : l.backwardArcs;
			Activity drive = new Activity(dep, arr, Activity.Type.DRIVE, getArcFromList(dep, arr, arcFromList));
			activities.add(drive);

			//increment and check condition for loop
			switch (direction)
			{
			case FORWARD : 
				i++;
				j++;
				check = i < stops.size() - 1;
				break;
			case BACKWARD :
				i--;
				j--;
				check = i > 0;
				break;
			}
		}
	}

	/** find arc from given list by departure and arrival event
	 * @param dep departure event
	 * @param arr arrival event
	 * @param arcs list of arcs
	 * @return the found arc
	 */
	private Arc getArcFromList(Event dep, Event arr, List<Arc> arcs)
	{
		Stop departureStop = dep.stop;
		Stop arrivalStop = arr.stop;
		for (Arc a : arcs) 
		{
			if (a.from.stop == departureStop && a.to.stop == arrivalStop)
			{
				return a;
			}
		}
		return null;
	}


}
