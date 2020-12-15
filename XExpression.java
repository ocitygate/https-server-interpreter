public class XExpression
{

	public static Object Eval(String expr, XContext context)
		throws Exception
	{
		XEvaluator xevaluator = new XEvaluator(expr, context);
		return xevaluator.Eval();
	}

}
