
public class OD {

	public final Stop origin;
	public final Stop destination;
	public int count;
	
	/** origin-destination for the passenger flow model
	 * @param origin
	 * @param destination
	 * @param count
	 */
	public OD(Stop origin, Stop destination, int count) {
		this.origin = origin;
		this.destination = destination;
		this.count = count;
	}
	
	public boolean add(OD other)
	{
		if (origin == other.origin && destination == other.destination)
		{
			count += other.count;
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public String toString() {
		return "OD [origin=" + origin.id + ", destination=" + destination.id + ", count=" + count + "]";
	}
	
	
}
