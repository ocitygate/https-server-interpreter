import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XRegex
{
    public static boolean preg_match(String pattern, String input, String[][] matches)
    {
    	if (pattern.startsWith("/") & pattern.endsWith("/"))
    	{
    		pattern = pattern.substring(1, pattern.length() - 1);
    	}
    	
    	Pattern r = Pattern.compile(pattern);
    	Matcher m = r.matcher(input);
    	
		if (m.matches())
		{
			int groupCount = m.groupCount() + 1;
			matches[0] = new String[groupCount];
			for(int i = 0; i < groupCount; i++)
			{
				matches[0][i] = m.group(i);
			}
			return true;
		}
		else
		{
			matches[0] = new String[0];
			return false;
		}
    }	
}
