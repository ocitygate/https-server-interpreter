import java.util.ArrayList;

public class XLanguage
{
	public static String[] Tokenize(String haystack, XNeedle[] needles)
	{
	    int haystack_len = haystack.length();
	    int h = 0;
	    ArrayList<String> tokens = new ArrayList<String>();
	    String terminator = null;
	    
	    int i = 0;
	    haystack: for( ; i < haystack_len; i++)
	    {
	    	XNeedle[] needles_ = terminator == null ? needles : new XNeedle[] { new XNeedle(terminator, null) }; 
	    	needle: for (XNeedle needle : needles_)
	    	{
		        for (int j = 0; j < needle.Needle.length(); j++)
		        {
					if ((haystack_len <= i + j) || (haystack.charAt(i + j) != needle.Needle.charAt(j)))
					{
						continue needle;
					}
		        }
	    	
		        if (h < i) tokens.add(haystack.substring(h, i));
		        tokens.add(needle.Needle);
		        i += needle.Needle.length() - 1;
		        h = i + 1;
	        
		        if (terminator == null)
		        {
		        	terminator = needle.Terminator;
		        }
		        else
		        {
		        	terminator = null;
		        }
	        
		        continue haystack;
	    	}
	    }
	    
	    if (h < i) tokens.add(haystack.substring(h, i));
	    
	    String[] temp = new String[tokens.size()];
	    tokens.toArray(temp);
	    
	    return temp;
	}
}
