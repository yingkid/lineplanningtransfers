import java.util.*;


public class ShortestPath {

	private EAN network;
	private List<Event> events;
	private int[] dist;
	private Event[] prev;
	
	/** creates shortest path class for ean 
	 * @param network
	 */
	public ShortestPath(EAN network)
	{
		this.network = network;
		this.events = network.events;
	}

	/** solve dijkstra from start event
	 * @param startEvent event
	 * @return
	 */
	public int[] Dijkstra(Event startEvent)
	{
		int size = events.size();
		dist = new int[size];
		prev = new Event[size];
		boolean[] visited = new boolean[size];
		Arrays.fill(dist, Integer.MAX_VALUE);
		Arrays.fill(visited, false);
		dist[events.indexOf(startEvent)] = 0;

		for (int count = 0; count < size; count++)
		{
			int u = min(dist, visited);
			visited[u] = true;
			List<Event> out = events.get(u).out;

			for (int v = 0; v < size; v++)
			{
				if (!visited[v])
				{
					int indexOf = out.indexOf(events.get(v));
					if (indexOf != -1)
					{
						int distuv = events.get(u).outActivity.get(indexOf).value;
						
						if (dist[u] + distuv < dist[v])
						{
							dist[v] = dist[u] + distuv;
							prev[v] = events.get(u);
						}
					}
				}
			}
		}
		
		return dist;
	}
	
	/** get cycle 
	 * @param startEvent
	 * @param endEvent
	 * @return cycle object
	 */
	public Cycle getCycle(Event startEvent, Event endEvent)
	{		
		List<Activity> cycle = new ArrayList<Activity>();
		int idx = network.events.indexOf(endEvent);
		Event current = endEvent;
		Event prev_event = prev[idx];

		while (current != null)
		{
			if (prev_event != null)
			{
				Activity a = current.getInActivity(prev_event);
				cycle.add(a);
				current = prev_event;
				idx = network.events.indexOf(prev_event);
				prev_event = prev[idx];
			}
			else
			{
				current = null;
			}
		
		}
		Collections.reverse(cycle);
		cycle.add(endEvent.getOutActivity(startEvent)); //connecting the endEvent with the startEvent
		return new Cycle(cycle);
	}
	
	

	private int min(int[] dist, boolean[] visited)
	{
		int minVal = Integer.MAX_VALUE;
		int minIndex = -1;
		for (int i = 0; i < dist.length; i++)
		{
			if (visited[i] == false && dist[i] <= minVal)
			{
				minVal = dist[i];
				minIndex = i;
			}
		}
		return minIndex;
	}
	


}
