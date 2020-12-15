import java.util.LinkedHashMap;

public class XContext
{
	public final XTree Global;
	public final XTree Local;
	
 	public XContext(XTree global)
	{
		Global = global;
		Local = new XTree();
	}
	
	public XContext(XTree global, XTree local)
	{
		Global = global;
		Local = local;
	}
	
	void navigate(String key, LinkedHashMap<String, Object>[] map, String[] index, boolean create)
		throws Exception
	{
		int i = 0;
				
		if (key.startsWith("."))
		{
			map[0] = Global.map;
			i++;
		}
		else
		{
			map[0] = Local.map;
		}

		boolean in = false;
		StringBuilder temp = new StringBuilder();
		StringBuilder variable = null;
		for( ; i < key.length(); i++)
		{
			char c = key.charAt(i);
			if (in)
			{
				if (c == ']') //out
				{
					temp.append(Get(variable.toString()));
					in = false;
				}
				else
				{
					variable.append(c);
				}
			}
			else
			{
				if (c == '.')
				{
					String prefix = temp.toString();
					Object item = null;
					if (map[0].containsKey(prefix))
					{
						item = map[0].get(prefix);
					}
					else
					{
						if (create)
						{
							item = new XTree();
							map[0].put(prefix, item);
						}
					}
					
					if (item != null && item.getClass() == XTree.class)
					{
						map[0] = ((XTree)item).map;
					}
					else
					{
						throw new Exception("Invalid path - " + key);	
					}
					temp = new StringBuilder();
				}
				else if(c == '[') //in
				{
					variable = new StringBuilder();
					in = true;
				}
				else
				{
					temp.append(c);
				}
			}
		}
			
		if (in) throw new Exception("SQUARE_BRACKETS_NOT_CLOSED");
		
		if (map[0] == null) throw new Exception("VARIABLE_NOT_FOUND");

		index[0] = temp.toString();
	}

	public void Set(String key, Object value)
		throws Exception
	{ 
		if (key == null) return;
		
		@SuppressWarnings("unchecked")
		LinkedHashMap<String, Object>[] map = (LinkedHashMap<String, Object>[]) new LinkedHashMap[1];
		String[] index = new String[1];
		
		navigate(key, map, index, true);

		map[0].put(index[0], value);
	}
	
	public Object Get(String key)
			throws Exception
	{
		if (".".equals(key))
		{
			return Global;
		}
		else if ("~".equals(key))
		{
			return Local;
		}

		@SuppressWarnings("unchecked")
		LinkedHashMap<String, Object>[] map = (LinkedHashMap<String, Object>[]) new LinkedHashMap[1];
		String[] index = new String[1];
		
		try
		{
			navigate(key, map, index, false);
		}
		catch (Exception e)
		{
			return null;
		}

		return map[0].get(index[0]);
	}
	
	public boolean IsSet(String key)
	{
		@SuppressWarnings("unchecked")
		LinkedHashMap<String, Object>[] map = (LinkedHashMap<String, Object>[]) new LinkedHashMap[1];
		String[] index = new String[1];

		try
		{
			navigate(key, map, index, false);
			return map[0].containsKey(index[0]);
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public void UnSet(String key)
	{
		if (key == null) return;
		
		@SuppressWarnings("unchecked")
		LinkedHashMap<String, Object>[] map = (LinkedHashMap<String, Object>[]) new LinkedHashMap[1];
		String[] index = new String[1];
		
		try { navigate(key, map, index, true); } 
		catch (Exception e) { return; }

		map[0].remove(index[0]);
	}
}
