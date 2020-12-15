public class XToken
{
	public final XTokenType Type;
	public final Object Op;
	public final boolean Unary;
	public int Arity;
	
	public XToken(XTokenType type, Object op, boolean unary)
	{
		Type = type;
		Op = op;
		Unary = unary;
	}
}
