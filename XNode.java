import java.sql.ResultSet;

public class XNode
{
	public final String Command;
	public final String Variable;
	public final String Key;
	public final String Value;
	public final String Expression;
	public final int Index;
	public final ResultSet ResultSet;
	public final XTree.XIterator Iterator;
	public final boolean Flag;
	
	public XNode(String command, String variable, String key, String value, String expression, int index, ResultSet resultSet, XTree.XIterator iterator, boolean flag)
	{
		Command = command;
		Variable = variable;
		Key = key;
		Value = value;
		Expression = expression;
		Index = index;
		ResultSet = resultSet;
		Iterator = iterator;
		Flag = flag;
	}
}
