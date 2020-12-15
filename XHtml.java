public class XHtml
{
	static XNeedle[] lang = new XNeedle[]
	{
		new XNeedle("$(", ")"),
		new XNeedle("$[", "]"),
	};
	
	public static String Encode(String value)
	{
		value = value.replace("&", "&amp;");
		value = value.replace("<", "&lt;");
		value = value.replace(">", "&gt;");
		value = value.replace("\"", "&quot;");
		
		return value;
	}

	public static String FillIn(String xhtml, XContext context)
		throws Exception
	{
		String[] tokens = XLanguage.Tokenize(xhtml, lang);
		
		StringBuilder out = new StringBuilder();
		String optype = "";
		
		StringBuilder expression = null;
	    
	    for(int i = 0; i < tokens.length; i++)
	    {
	    	String token = tokens[i];
	      
	    	switch (optype)
	    	{
	        	case "$(":
	        		
	        		if (")".equals(token)) //execute op
	        		{
	        			out.append(Encode(XHelper.ToString(context.Get(expression.toString()))));
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
}
