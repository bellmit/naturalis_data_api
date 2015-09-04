package nl.naturalis.nda.elasticsearch.load;

/**
 * 
 * @author Ayco Holleman
 * 
 * @deprecated Should be handled and logged by {@link Transformer}s without
 *             throwing an exception.
 *
 */
@SuppressWarnings("serial")
public class MalformedDataException extends Exception {

	public MalformedDataException()
	{
		super();
	}


	public MalformedDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}


	public MalformedDataException(String message, Throwable cause)
	{
		super(message, cause);
	}


	public MalformedDataException(String message)
	{
		super(message);
	}


	public MalformedDataException(Throwable cause)
	{
		super(cause);
	}

}
