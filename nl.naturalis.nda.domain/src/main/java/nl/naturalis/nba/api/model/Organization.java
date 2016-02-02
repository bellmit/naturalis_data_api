package nl.naturalis.nba.api.model;

public class Organization extends Agent {

	private String name;

	public Organization()
	{
	}

	public Organization(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		}
		if (obj == null || !(obj instanceof Organization)) {
			return false;
		}
		Organization other = (Organization) obj;
		return eq(name, other.name);
	}

	public int hashCode()
	{
		int hash = 17;
		hash = (hash * 31) + name == null ? 0 : name.hashCode();
		return hash;
	}

	public String toString()
	{
		return "{name: " + quote(name) + "}";
	}

	private static String quote(Object obj)
	{
		return obj == null ? "null" : '"' + String.valueOf(obj) + '"';
	}

	private static boolean eq(Object obj0, Object obj1)
	{
		if (obj0 == null) {
			if (obj1 == null) {
				return true;
			}
			return false;
		}
		return obj1 == null ? false : obj0.equals(obj1);
	}
}
