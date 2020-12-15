public class XValue
{
	public final XValueType Type;
	public final Object Value;
	public final XContext Context;
	
	public XValue(XValueType type, Object value, XContext context)
	{
		Type = type;
		Value = value;
		Context = context;
	}
	
	public Object getValue()
		throws Exception
	{
		if (Type == XValueType.Variable)
		{
			return Context.Get((String)Value);
		}
		else if (Type == XValueType.Constant)
		{
			return Value;
		}
		return null;
	}
}
