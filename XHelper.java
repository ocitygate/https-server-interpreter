import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;

public class XHelper
{
	public static String readLine(InputStream stream)
	{
        StringBuilder sb = new StringBuilder();

        for (; ; )
        {
            int i;
			try { i = stream.read(); }
			catch (IOException e) {	return null; }
			
            if (i == -1)
            {
                if (sb.length() == 0) return null;
                return sb.toString();
            }

            char c = (char)i;
            switch (c)
            {
                case '\r':
                    break;
                case '\n':
                    return sb.toString();
                default:
                    sb.append(c);
                    break;
            }
        }
	}
	
	public static Boolean IsTrue(Object value)
	{
		if (value == null)
			return false;
		
		if (value.getClass() == String.class)
		{
			if ("".equals(value)) return false;
			
			try { value = Parse((String)value); }
			catch (Exception e) { return true; }
		}
		
		if (value == null)
			return false;
		else if (value.getClass() == Boolean.class)
			return (Boolean)value;
		else if (value.getClass() == Long.class)
			return (Long)value != 0;
		else if (value.getClass() == Double.class)
			return (Double)value != 0.0;
		else
			return true;
	}

	public static Double ToDouble(Object value)
		throws Exception
	{
		if (value == null) return null;
			
		if (value.getClass() == String.class)
		{
			value = Parse((String)value);
		}

		if (value.getClass() == Double.class)
		{
			return (Double)value;
		}
		else if (value.getClass() == Long.class)
		{
			return ((Long)value).doubleValue();
		}
		throw new Exception("ToDouble undefined");
	}
	
	public static Long ToLong(Object value)
		throws Exception
	{
		if (value == null) return null;

		if (value.getClass() == String.class)
		{
			value = Parse((String)value);
		}

		if (value.getClass() == Long.class)
		{
			return (Long)value;
		}
		else if (value.getClass() == Double.class)
		{
			return ((Double)value).longValue();
		}

		throw new Exception("ToLong undefined");
	}
	
	public static String ToString(Object value)
	{
		if (value == null) return "";
		
		return value.toString();
	}
	
	public static Object Parse(String s)
	{
		switch (s.trim())
		{
			case "null":
	        	return null;
			case "false":
	        	return Boolean.FALSE;
			case "true":
	        	return Boolean.TRUE;
	        default:
	        	try
	        	{
	        		return Long.parseLong(s);
	        	}
	        	catch (Exception e)
	        	{
	        		return Double.parseDouble(s);
	        	}
		}
	}

	public static XTree Split(String s, String sep, Long limit)
	{
		XTree temp = new XTree();
		
		int pos = 0;
		int i = 0;
		
		int sep_pos = s.indexOf(sep, pos);
		while (sep_pos != -1 & i < limit - 1)
		{
			temp.map.put(ToString(i), s.substring(pos, sep_pos));
			pos = sep_pos + sep.length();
			i++;
			sep_pos = s.indexOf(sep, pos);
		}
		
		temp.map.put(ToString(i), s.substring(pos));
		
		return temp;
	}

	public static XTree Split(String s, String sep)
	{
		return Split(s, sep, Long.MAX_VALUE);
	}
	
	public static Long Count(XTree tree)
	{
		return new Long(tree.map.size());
	}

	public static XTree Keys(XTree tree)
	{
		XTree temp = new XTree();
		int i = 0;
		for (String key : tree.map.keySet())
		{
			temp.map.put(ToString(i), key);
		    i++;
		}
		return temp;
	}

	public static Long Length(String s)
	{
		return new Long(s.length());
	}

	static XNeedle[] lang = new XNeedle[]
	{
		new XNeedle("$(", ")"),
		new XNeedle("${", "}"),
	};

	public static String FillIn(String s, XContext context)
		throws Exception
	{
		String[] tokens = XLanguage.Tokenize(s, lang);
		
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
	        			out.append(XHelper.ToString(context.Get(expression.toString())));
	        			optype = "";
	        		}
	        		else
	        		{
	        			expression.append(token);
	        		}
	        		break;
	        		
	        	case "${": 

	        		if ("}".equals(token)) //execute op
	        		{
	        			out.append(expression.toString().replace('_', ' '));
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
	            		case "${": 
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

	public static String Replace(String s, String r, String w)
	{
		return s.replace(r, w);
	}
	
	public static String ReadFile(String filename)
		throws IOException
	{
       return new String (Files.readAllBytes(Paths.get(filename)), "ISO-8859-1");
	}
	
	public static XTree Merge(XTree[] trees)
	{
		XTree temp = new XTree();
		for(XTree tree : trees)
		{
			for (Map.Entry<String, Object> entry : tree.map.entrySet())
			{
			    String key   = entry.getKey();
			    Object value_ = entry.getValue();
			    
			    temp.map.put(key, value_);
			}
		}
		return temp;
	}
	
}
