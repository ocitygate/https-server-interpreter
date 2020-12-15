import java.util.LinkedHashMap;

public class XOperator
{
	public String Operator;
	public int Precedence;
	public boolean Alphabetic;
	public boolean Unary;
	public boolean RightAssoc;
	
	public XOperator(String operator, int precedence, boolean alphabetic, boolean unary, boolean rightAssoc)
	{
		Operator = operator;
		Precedence = precedence;
		Alphabetic = alphabetic;
		Unary = unary;
		RightAssoc = rightAssoc;
	}
	
	public static LinkedHashMap<String, XOperator> GetOperators()
	{
		LinkedHashMap<String, XOperator> temp = new LinkedHashMap<String, XOperator>();
		
		temp.put(":=" , new XOperator(":=" , -1, false, false, true ));
		temp.put(","  , new XOperator(","  ,  0, false, false, false));
		temp.put("or" , new XOperator("or" ,  1, true , false, false));
		temp.put("xor", new XOperator("xor",  2, true , false, false));
		temp.put("and", new XOperator("and",  3, true , false, false));
		temp.put("not", new XOperator("not",  4, true , true , false));
		temp.put("="  , new XOperator("="  ,  6, false, false, false));
		temp.put("<>" , new XOperator("<>" ,  6, false, false, false));
		temp.put(">=" , new XOperator(">=" ,  6, false, false, false));
		temp.put(">"  , new XOperator(">"  ,  6, false, false, false));
		temp.put("<=" , new XOperator("<=" ,  6, false, false, false));
		temp.put("<"  , new XOperator("<"  ,  6, false, false, false));
		temp.put("-"  , new XOperator("-"  , 10, false, false, false));
		temp.put("+"  , new XOperator("+"  , 10, false, false, false));
		temp.put("**" , new XOperator("**" , 12, false, false, true ));
		temp.put("*"  , new XOperator("*"  , 11, false, false, false));
		temp.put("/"  , new XOperator("/"  , 11, false, false, false));
		temp.put("mod", new XOperator("mod", 11, true, false, false));
		temp.put("||" , new XOperator("||" , 13, false, false, false));

		return temp;
	}
}
