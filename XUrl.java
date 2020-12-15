import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

public class XUrl
{
	static XNeedle[] lang = new XNeedle[]
	{
		new XNeedle("$(", ")"),
		new XNeedle("$[", "]"),
	};

	public static XTree ParseUrl(String URL)
		throws URISyntaxException
	{
		URI uri = new URI(URL);

		XTree temp = new XTree();

		temp.map.put("protocol", uri.getScheme());
		temp.map.put("host", uri.getHost());
		temp.map.put("port", uri.getPort());
		temp.map.put("path", uri.getPath());
		temp.map.put("query", uri.getQuery());
		temp.map.put("fragment", uri.getFragment());

		return temp;
	}
    
	public static XTree ParseQuery(String query)
		throws UnsupportedEncodingException
	{
		XTree tree = new XTree();
	    String[] pairs = query.split("&");
	    for (String pair : pairs)
	    {
	    	if ("".equals(pair)) continue;
	        int idx = pair.indexOf("=");
	        if (idx == -1)
	        	tree.map.put(URLDecoder.decode(pair, "UTF-8"), "");
	        else
	        	tree.map.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
	    }
	    return tree;
	}

	public static String Encode(String value)
	{
		try { return URLEncoder.encode(value, "UTF-8");	}
		catch (UnsupportedEncodingException e) { };
		return null;
	}
	
	public static String BuildQuery(XTree tree)
	{
		StringBuilder temp = new StringBuilder();
	    for (Map.Entry<String, Object> pair : tree.map.entrySet()) 
	    {
	    	if (temp.length() != 0)
		    	temp.append("&");

	    	temp.append(pair.getKey());
	    	temp.append("=");
	    	temp.append(Encode(pair.getValue().toString()));
	    }
	    
	    return temp.toString();
	}
	
	public static String FillIn(String URL, XContext context)
		throws Exception
	{
		String[] tokens = XLanguage.Tokenize(URL, lang);
		
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

	public static String FillIn(String URL, XTree tree)
		throws Exception
	{
		return FillIn(URL, new XContext(new XTree(), tree));
	}

	public static String Merge(String url, String query)
		throws Exception
	{
		XTree parse_url = ParseUrl(url);
		String parse_url_query = XHelper.ToString(parse_url.map.get("query"));
		XTree value0 = ParseQuery(parse_url_query);
		XTree value1 = ParseQuery(query);
	    String query_ = BuildQuery(XHelper.Merge(new XTree[] { value0, value1 }));

	    return XHelper.ToString(parse_url.map.get("path")) +
	    		("".equals(query_) ? "" : ("?" + query_));
	}
}
