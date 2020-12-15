import java.sql.*;  

public class XDatabase
{
	public final String Host;
	public final String Database;
	public final String User;
	public final String Password;

	Connection conn;
   
	static XNeedle[] lang = new XNeedle[] {
		new XNeedle("$((", "))"),
		new XNeedle("$(", ")"),
		new XNeedle("$[", "]"),
	};
	
	public XDatabase(String host, String database, String user, String password)
	{
		Host = host;
		Database = database;
		User = user;
		Password = password;
	}
	
	public void requireDB()
		throws SQLException
	{
		if (conn == null)
		{
//			try	{ Class.forName("com.mysql.cj.jdbc.Driver");}
//			catch (ClassNotFoundException e) { }
			
			conn = DriverManager.getConnection("jdbc:mysql://" + Host + "/" + Database, User, Password);
		}
	}
	
	static String Escape(String value)
	{
		value = value.replace("\\", "\\\\");
		value = value.replace("'", "\\'");
		
		return value;
	}
	
	static String FillIn(String sql, XContext context)
		throws Exception
  	{
		String[] tokens = XLanguage.Tokenize(sql, lang);
		
		StringBuilder out = new StringBuilder();
		String optype = "";
		
		StringBuilder expression = null;
	    
	    for(int i = 0; i < tokens.length; i++)
	    {
	    	String token = tokens[i];
	      
	    	switch (optype)
	    	{
	        	case "$((": 
	        		
	        		if ("))".equals(token)) //execute op
	        		{
	        			out.append(Escape(XUrl.Encode(XHelper.ToString(context.Get(expression.toString())))));
	        			optype = "";
	        		}
	        		else
	        		{
	        			expression.append(token);
	        		}
	        		break;

	        	case "$(":
	        		
	        		if (")".equals(token)) //execute op
	        		{
	        			out.append(Escape(XHelper.ToString(context.Get(expression.toString()))));
	        			optype = "";
	        		}
	        		else
	        		{
	        			expression.append(token);
	        		}
	        		break;
	        		
	        	case "$[": 

	        		if ("]".equals(token)) //execute op
	        		{
	        			out.append(XHelper.ToString(context.Get(expression.toString())));
	        			optype = "";
	        		}
	        		else
	        		{
	        			expression.append(token);
	        		}
	        		break;

	        	case "":
	        		
	        		switch (token)
	        		{
	            		case "$((": 
	            		case "$(": 
	            		case "$[": 
	            			optype = token;
	            			expression = new StringBuilder();
	            			break;
	            		default:
	            			out.append(token);
	            			break;
	        		}
	        		break;
	    	}
    	}

	    return out.toString();
  	}
	
	public ResultSet ExecuteReader(String sql)
		throws SQLException
	{
		requireDB();
		
		//MyDBApp.Log(sql);
		
		Statement statement = conn.createStatement();  
		return statement.executeQuery(sql);  
	}

}