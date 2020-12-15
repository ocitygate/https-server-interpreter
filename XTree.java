import java.lang.Object;
import java.util.LinkedHashMap;
import java.util.Iterator;

public class XTree
{
	public class XIterator
	{
		Iterator<String> iterator;
		Object[] keys;
		int current = -1;
		String key;
		Object value;
		private XIterator()
		{
			keys = map.keySet().toArray();
		}
		public boolean Next()
		{
			if (current == keys.length - 1)
				return false;
			current++;
			key = (String)keys[current];
			value = map.get(key);
			return true;
		}
		public String getKey()
		{
			return key;
		}
		public Object getValue()
		{
			return value;
		}
	}
	
	public LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();

	public XIterator getIterator()
	{
		return new XIterator();
	}
}
