package nl.naturalis.nba.api.query;

import java.util.Iterator;
import java.util.List;

/**
 * Java bean representing the result of a query request.
 * 
 * @author Ayco Holleman
 *
 * @param <T>
 *            The type of object returned by the query request.
 */
public class QueryResult<T> implements Iterable<T> {

	private long totalSize;
	private List<T> resultSet;

	@Override
	public Iterator<T> iterator()
	{
		return resultSet.iterator();
	}

	/**
	 * Returns the number of documents in this {@code QueryResult}.
	 * 
	 * @return
	 */
	public int size()
	{
		return resultSet.size();
	}

	/**
	 * Returns the document with the specified index.
	 * 
	 * @param index
	 * @return
	 */
	public T get(int index)
	{
		return resultSet.get(index);
	}

	/**
	 * Returns the total number of documents conforming to the {@link QuerySpec
	 * query specification} that produced this query result.
	 * 
	 * @return
	 */
	public long getTotalSize()
	{
		return totalSize;
	}

	/**
	 * Sets the total number of documents conforming to the {@link QuerySpec
	 * query specification} that produced this query result. Not meant to be
	 * called by clients.
	 * 
	 * @param totalSize
	 */
	public void setTotalSize(long totalSize)
	{
		this.totalSize = totalSize;
	}

	/**
	 * Sets the result set of this {@code QueryResult}. Not meant to be called
	 * by clients.
	 * 
	 * @param resultSet
	 */
	public void setResultSet(List<T> resultSet)
	{
		this.resultSet = resultSet;
	}

}