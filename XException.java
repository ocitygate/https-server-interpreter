@SuppressWarnings("serial")
public class XException extends Exception
{
	public final String Function;
	public final int Line;
	public final String Message;
	
	public XException(String function, int line, String message)
	{
		Function = function;
		Line = line;
		Message = message;
	}
}
