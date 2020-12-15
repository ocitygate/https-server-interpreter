import java.io.Writer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class XMachine
{
	public final String Function;
	public final Writer Writer;
	public final XContext Context;

	ArrayList<String> lines = new ArrayList<String>();

	int counter;
	int xptr = -1;
	
	Stack<XNode> xstack = new Stack<XNode>();
	
	public XMachine(String function, String code, Writer writer, XContext context)
			throws XException
	{
		Function = function;
		Writer = writer;
		Context = context;

		for (String line : code.split("\\r?\\n"))
		{
			lines.add(line.trim());
		}
	}
	
	void printStack()
	{
		StringBuilder tmp = new StringBuilder();
		tmp.append("[");
		
		for(XNode xnode : xstack)
		{
			tmp.append(xnode.Command);
			if (xnode.Flag) tmp.append("*");
			tmp.append(" ");
		}
		tmp.append("\n");
		
		try { Writer.write(tmp.toString()); }
		catch (IOException e) {	/*e.printStackTrace();*/ }
	}
	
	XNode push(String command, String variable, String key, String value, String argument, int index, ResultSet resultSet, XTree.XIterator iterator, boolean flag)
	{
		XNode push = xstack.push(new XNode(command, variable, key, value, argument, index, resultSet, iterator, flag));
		//printStack();
		return push; 
	}

	XNode peek()
	{
		if (xstack.size() == 0)
		{
			return null;
		}
		else
		{
			return xstack.peek();
		}
	}
	
	XNode pop()
	{
		if (xstack.size() == 0)
			return null;
		else
		{
			XNode pop = xstack.pop();
			if (xptr == xstack.size())
				xptr--;
			//printStack();
			return pop;
		}
	}
	
	void assertTop(String command, String xcommand)
			throws XException
	{
		XNode peek = peek();
		if (peek == null)
		{
			throw new XException(Function, counter, "Unexpected " + xcommand + ": STACK_EMPTY");
		}
		else if (!command.equals(peek.Command))
		{
			throw new XException(Function, counter, "Unexpected " + xcommand + ": UNCLOSED : " + peek.Command);
		}
	}
	
	boolean xon()
	{
		return xptr == xstack.size() - 1;
	}
	
	void echo (String xhtml) throws XException
	{
		if (xon() == false) return;
		try
		{
			try
			{
				xhtml = XHtml.FillIn(xhtml, Context);
			}
			catch (Exception e)
			{
				throw new XException(Function, counter, e.getClass().toString() + ": " + e.getMessage());
			}
			Writer.write(xhtml + "\r\n");
		}
		catch (IOException e) {	throw new XException(Function, counter, "IO : " + e.getMessage()); }
	}
	
	void out(String expression)
		throws Exception
	{
		if (xon() == false) return;
		Object value = XExpression.Eval(expression, Context);

		try	{ Writer.write(XHelper.ToString(value)); }
		catch (IOException e) {	throw new XException(Function, counter, "IO : " + e.getMessage()); }
			
	}

	void let(String expression) throws XException
	{
		if (xon() == false) return;
		try
		{
			XExpression.Eval(expression, Context);
		}
		catch (Exception e)
		{
			//e.printStackTrace();
			throw new XException(Function, counter, e.getClass().toString() + ": " + e.getMessage());
		}
	}
	
	void if_(String expression, boolean flag)
		throws XException
	{
		boolean xon = xon();
		push("if", null, null, null, expression, counter, null, null, flag);
		if (xon == false) return;
		try
		{
			if (XHelper.IsTrue(XExpression.Eval(expression, Context)))
			{
				xptr++;
			}
		}
		catch (Exception e)
		{
			throw new XException(Function, counter, e.getClass().toString() + ": " + e.getMessage());
		}
	}
	
	void if_(String expression)
		throws XException
	{
		if_(expression, false);
	}
	
	void while_(String expression)
		throws XException
	{
		boolean xon = xon();
		push("while", null, null, null, expression, counter, null, null, false);
		if (xon == false) return;
		try
		{
			if (XHelper.IsTrue(XExpression.Eval(expression, Context)))
			{
				xptr++;
			}
		}
		catch (Exception e)
		{
			throw new XException(Function, counter, e.getClass().toString() + ": " + e.getMessage());
		}
	}

	void else_()
			throws XException
	{
		assertTop("if", "else");
		boolean xon = xon();
		if (xon)
		{
			xptr--;
		}
		else if (xptr == xstack.size() - 2)
		{
			xptr++;
		}
	}

	void elif(String expression)
		throws XException
	{
		else_();
		if_(expression, true);
	}
	
	void endif()
		throws XException
	{
		XNode pop;
		do
		{
			assertTop("if", "/if");
			pop = pop();
		}
		while(pop.Flag);
	}

	void endwhile()
			throws XException
	{
		boolean xon = xon();
		assertTop("while", "/while");

		if (xon)
		{
			XNode xnode = peek();
			try
			{
				if (XHelper.IsTrue(XExpression.Eval(xnode.Expression, Context)))
				{
					counter = xnode.Index;
					return;
				}
			}
			catch (Exception e)
			{
				throw new XException(Function, counter, e.getClass().toString() + ": " + e.getMessage());
			}
		}

		pop();
	}
	
	void call(String function)
		throws XException, XExit, Exception
	{
		if (xon() == false) return;
		
		XContext context = new XContext(Context.Global);
		
		XMachine xmachine = new XMachine(function, XHelper.ReadFile(function), Writer, context);
		xmachine.Run();
	}
	
	void call(String function, String variable)
			throws XException, XExit, Exception
	{
		if (xon() == false) return;
		
		XContext context = new XContext(Context.Global, (XTree)Context.Get(variable));
		
		XMachine xmachine = new XMachine(function, XHelper.ReadFile(function), Writer, context);
		xmachine.Run();
	}
		
	void exception(String message)
		throws XException
	{
		if (xon() == false) return;
		throw new XException(Function, counter, message);
	}
	
	void exit()
			throws XExit
	{
		if (xon() == false) return;
		throw new XExit();
	}

	void database(String host, String database, String user, String password)
		throws XException, Exception
	{
		Context.Set("_DATABASE.default", new XDatabase(host, database, user, password));
	}

	void SQL(String variable, String database, String sql)
		throws XException
	{
		if (xon() == false)
		{
			push("SQL", variable, null, null, sql, counter, null, null, false);
			return;
		}
		
		ResultSet resultSet = null;
		try
		{
			Object xdatabase = Context.Get(database);
			
			if (xdatabase == null)
				throw new XException(Function, counter, "NO_DATABASE");
			
			if (xdatabase.getClass() != XDatabase.class)
				throw new XException(Function, counter, "INVALID_DATABASE");
			
			resultSet = ((XDatabase)xdatabase).ExecuteReader(XDatabase.FillIn(sql, Context));

			if (resultSet.next())
			{
				if (variable != null) Context.Set(variable, new XTree());	
				ResultSetMetaData meta = resultSet.getMetaData();
				for(int i = 1; i <= meta.getColumnCount(); i++)
				{
					String name = meta.getColumnLabel(i);
					byte[] bytes = resultSet.getBytes(i);
					String value = null;
					if (bytes != null)
						value = new String(resultSet.getBytes(i), "ISO-8859-1");
					
					if (variable != null)
					{
						if (variable != null) Context.Set(variable + "." + name, value);
					}
				}

				xptr++;
			}
		}
		catch (XException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			//e.printStackTrace();
			throw new XException(Function, counter, e.getClass().toString() + ": " + e.getMessage());
		}

		push("SQL", variable, null, null, sql, counter, resultSet, null, false);
	}
	
	void sql(String sql)
			throws XException
	{
		if (xon() == false)
		{
			push("sql", null, null, null, sql, counter, null, null, false);
			return;
		}
		
		ResultSet resultSet = null;
		try
		{
			Object xdatabase = Context.Get("._DATABASE.default");
			
			if (xdatabase == null)
				throw new XException(Function, counter, "NO_DATABASE");
			
			if (xdatabase.getClass() != XDatabase.class)
				throw new XException(Function, counter, "INVALID_DATABASE");
			
			resultSet = ((XDatabase)xdatabase).ExecuteReader(XDatabase.FillIn(sql, Context));

			if (resultSet.next())
			{
				ResultSetMetaData meta = resultSet.getMetaData();
				for(int i = 1; i <= meta.getColumnCount(); i++)
				{
					String name = meta.getColumnLabel(i);
					byte[] bytes = resultSet.getBytes(i);
					String value = null;
					if (bytes != null)
						value = new String(resultSet.getBytes(i), "ISO-8859-1");

					Context.Set(name, value);
				}

				xptr++;
			}
		}
		catch (XException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new XException(Function, counter, e.getClass().toString() + ": " + e.getMessage());
		}

		push("sql", null, null, null, sql, counter, resultSet, null, false);
	}
		
	void endSQL()
		throws XException
	{
		boolean xon = xon();
		assertTop("SQL", "/SQL");

		if (xon)
		{
			XNode xnode = peek();
			try
			{
				if (xnode.ResultSet.next())
				{
					if (xnode.Variable != null)	Context.Set(xnode.Variable, new XTree());

					ResultSetMetaData meta = xnode.ResultSet.getMetaData();
					for(int i = 1; i <= meta.getColumnCount(); i++)
					{
						String name = meta.getColumnLabel(i);
						byte[] bytes = xnode.ResultSet.getBytes(i);
						String value = null;
						if (bytes != null)
							value = new String(xnode.ResultSet.getBytes(i), "ISO-8859-1");

						if (xnode.Variable != null)	Context.Set(xnode.Variable + "." + name, value);
					}

					counter = xnode.Index;
					return;
				}
			}
			catch (SQLException e)
			{
				throw new XException(Function, counter, "SQL : " + e.getMessage());
			}
			catch (Exception e)
			{
				throw new XException(Function, counter, e.getClass().toString() + ": " + e.getMessage());
			}
		}

		pop();
	}
	
	void endsql()
			throws XException
	{
		boolean xon = xon();
		assertTop("sql", "/sql");

		if (xon)
		{
			XNode xnode = peek();
			try
			{
				if (xnode.ResultSet.next())
				{
					ResultSetMetaData meta = xnode.ResultSet.getMetaData();
					for(int i = 1; i <= meta.getColumnCount(); i++)
					{
						String name = meta.getColumnLabel(i);
						byte[] bytes = xnode.ResultSet.getBytes(i);
						String value = null;
						if (bytes != null)
							value = new String(xnode.ResultSet.getBytes(i), "ISO-8859-1");
						
						Context.Set(name, value);
					}
					
					counter = xnode.Index;
					return;
				}
			}
			catch (SQLException e)
			{
				throw new XException(Function, counter, "SQL : " + e.getMessage());
			}
			catch (Exception e)
			{
				throw new XException(Function, counter, e.getClass().toString() + ": " + e.getMessage());
			}
		}

		pop();
	}
		
	void for_(String key, String value, String expression)
		throws XException
	{
		if (xon() == false)
		{
			push("for", null, key, value, "", counter, null, null, false);
			return;
		}
		
		Object list;
		try
		{
			list = XExpression.Eval(expression, Context);
		}
		catch (Exception e)
		{
			throw new XException(Function, counter, e.getClass().toString() + ": " + e.getMessage());
		}
		XTree.XIterator iterator = null;
		if (list != null && list.getClass() == XTree.class)
		{
			iterator = ((XTree)list).getIterator();
			if (iterator.Next())
			{
				try
				{
					Context.Set(key, iterator.getKey());
					Context.Set(value, iterator.getValue());
				}
				catch (Exception e)
				{
					throw new XException(Function, counter, e.getClass().toString() + ": " + e.getMessage());
				}

				xptr++;
			}
		}
		push("for", null, key, value, "", counter, null, iterator, false);
	}
	
	void endfor()
		throws XException
	{
		boolean xon = xon();
		assertTop("for", "/for");

		if (xon)
		{
			XNode xnode = peek();
		
			XTree.XIterator iterator = xnode.Iterator;
			if (iterator.Next())
			{
				try
				{
					Context.Set(xnode.Key, iterator.getKey());
					Context.Set(xnode.Value, iterator.getValue());
				}
				catch (Exception e)
				{
					throw new XException(Function, counter, e.getClass().toString() + ": " + e.getMessage());
				}

				counter = xnode.Index;
				return;
			}
		}

		pop();
	}

	void page(String argument)
		throws XException, XExit, Exception
	{
		if (xon() == false) return;
		
		argument = XUrl.FillIn(argument, Context);

		Context.Set("init", new XTree());
		call("init", "init");

		Context.Set("page", new XTree());
		Context.Set("page.value", Context.Get("init.value"));
		Context.Set("page.query", argument);
		
		call("page", "page");
		
		out("page.out");
	}
	
	boolean process(String command, String argument)
		throws XException, XExit, Exception
	{
//		try { Writer.write((xon() ? "" : "#") + command + " " + argument + "\r\n"); }
//		catch (IOException e) {	e.printStackTrace(); }

		if ("".equals(command) || command.startsWith("#"))
			return true;
		
		switch (command)
    	{
    		case "call":
    			String[] argument_s = argument.split(" ");
    			if (argument_s.length == 1)
    			{
    				call(argument_s[0]);
    			}
    			else if (argument_s.length == 2)
    			{
    				call(argument_s[0], argument_s[1]);
    			}
    			else
    			{
    				throw new XException(Function, counter, "call <function> [<tree>]");
    			}
    			break;
    		case "echo":
    			echo(argument);
    			break;
    		case "out":
    			out(argument);
    			break;
    		case "let":
    			let(argument);
    			break;
    		case "if":
    			if_(argument);
    			break;
    		case "while":
    			while_(argument);
    			break;
    		case "for":
    			argument_s = argument.split(":", 3);
    			String key;
    			String value;
    			String expression;
    			if (argument_s.length == 3)
    			{
        			key = argument_s[0].trim();
        			value = argument_s[1].trim();
        			expression = argument_s[2].trim();
    			}
    			else if (argument_s.length == 2)
    			{
        			key = null;
        			value = argument_s[0].trim();
        			expression = argument_s[1].trim();
    			}
    			else
    			{
    				throw new XException(Function, counter, "for [<key>:]<value>:<list>");
    			}
    			for_(key, value, expression);
    			break;
    		case "else":
    			else_();
    			break;
    		case "elif":
    			elif(argument);
    			break;
    		case "/if":
    			endif();
    			break;
    		case "/while":
    			endwhile();
    			break;
    		case "/for":
    			endfor();
    			break;
    		case "database":
    			argument_s = argument.split(" ");
    			if (argument_s.length != 4)
    			{
    				throw new XException(Function, counter, "database <host> <database> <user> <password>");
    			}
    			database(argument_s[0], argument_s[1], argument_s[2], argument_s[3]);
    			break;
    		case "sql":
    			sql(argument);
    			break;
    		case "/sql":
    			endsql();
    			break;
    		case "SQL":
    			argument_s = argument.split(":", 3);
    			
    			if (argument_s.length != 3)
    			{
    				throw new XException(Function, counter, "SQL <variable> : <database> : <sql>");
    			}
    			String variable = argument_s[0].trim();
    			String database = argument_s[1].trim();
    			String sql = argument_s[2].trim();

    			SQL(variable, database, sql);
    			break;
    		case "/SQL":
    			endSQL();
    			break;
    		case "return":
    			if (xon()) 
    				return false;
    		case "exit":
    			exit();
    			break;
    		case "exception":
    			exception(argument);
    			break;
    		case "page":
    			page(argument);
    			break;
    		default:
				throw new Exception("Invalid command - " + command);
    	}
    	return true;
	}

	void Run()
		throws XException, XExit
	{
		try
		{
			for (counter = 1; counter <= lines.size(); counter++)
	        {
	        	String line = lines.get(counter - 1);

	        	String command;
		    	String argument;

	        	String[] line_split = line.split(" ", 2);
	        	command = line_split[0];
	        	argument = line_split.length > 1 ? line_split[1] : "";

	        	while (argument.endsWith("\\"))
	        	{
	        		counter++;
	        		if (counter > lines.size())
	        		{
	        			throw new XException(Function, counter, "Unexpected EOF");
	        		}
	        		line = lines.get(counter - 1);
	        		argument = argument.substring(0, argument.length() - 1) + "\n" + line;
	        	}
	        	
	   			if (!process(command, argument))
	   				return;
	        }
		}
		catch (XExit e)
		{
			throw e;
		}
		catch (XException e)
		{
			//e.printStackTrace();
			throw e;
		}
		catch (Exception e)
		{
			//e.printStackTrace();
			throw new XException(Function, counter, e.getClass().toString() + ": " + e.getMessage());
		}
		finally
		{
		}
	}
}