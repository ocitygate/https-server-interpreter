import java.util.ArrayList;
import java.util.Stack;
import java.util.LinkedHashMap;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class XEvaluator
{
	static LinkedHashMap<String, XOperator> operators = XOperator.GetOperators();
	
	public final String Expr;
	public final XContext Context;
	
	int pos = 0;
	String op = "";
	boolean op_term = false;
	int op_start;
	XToken last_token = null;
	ArrayList<XToken> postfix = new ArrayList<XToken>();
	Stack<XToken> stack = new Stack<XToken>();
	Stack<Integer> arity = new Stack<Integer>();

	XEvaluator(String expr, XContext context)
		throws Exception
	{
		Expr = expr;
		Context = context;
		
		Parse();
	}

	void Parse()
		throws Exception
	{
		while (pos < Expr.length())
		{
			char c = Expr.charAt(pos);
			switch (c)
			{
				case '\'':
					popop(false);
					if (last_is_eval())
						throw new Exception(c + " unexpected");
					pos++;
					readstring();
					break;
				case '`':
					popop(false);
					if (last_is_eval())
						throw new Exception(c + " unexpected");
					pos++;
					readvariable();
					break;
				case '(':
					if (!"".equals(op))
					{
						popop(true);
						if (!"".equals(op)) // function
						{
							if (last_is_eval())
							{
								pos = op_start;
								throw new Exception(op + " unexpected");
							}
		
							process_token(XTokenType.Function, op, false);
							op = "";
							op_term = false;
						}
						else
						{
							if (last_is_eval())
								throw new Exception(c + " unexpected");
		
							process_token(XTokenType.Operator, "(", false);
						}
					}
					else
					{
						if (last_is_eval())
							throw new Exception(c + " unexpected");
		
						process_token(XTokenType.Operator, "(", false);
					}
					break;
				case ')':
		            popop(false);
		            
		            if (!last_is_eval() & !last_is_function()) 
		            	throw new Exception (c + " unexpected");
		            
		            if (last_is_function())
		            {
		            	arity.pop();
		            	arity.push(new Integer(0));
		            }
		            
		            process_token(XTokenType.Operator, ")", false);
		            break;
				case ' ':
				case '\t':
				case '\r':
				case '\n':
		            if (op != "") op_term = true;
					break;
				default:
		            boolean is_operator = false;

		            for(Object temp : operators.keySet().toArray())
		            {
		            	String operator = (String)temp;
		            	XOperator operator_ = operators.get(operator);
		            	
		            	if (!operator_.Alphabetic &&
		            		pos + operator.length() <= Expr.length() &&
		            		operator.equals(Expr.substring(pos, pos + operator.length())))
		            	{
		            		popop(false);
			                    
			                process_operator(operator);
			                
			                pos += operator.length() - 1;
			                is_operator = true;
			                break;
		            	}
		            }
		            
		            if (!is_operator)
		            {
		              if (op_term)
		              {
		            	  popop(false);
		              }

		              if ("".equals(op)) op_start = pos;
		              op += c;
		            }

					break;
			}
			
			pos++;
		}
		
		popop(false);
		
		if (!last_is_eval())
			throw new Exception("END unexpected");
		
		XToken peek = stack.isEmpty() ? null : stack.peek();
		while (peek != null && peek.Op != "(" && peek.Op != "{")
		{
			postfix.add(stack.pop());
			peek = stack.isEmpty() ? null : stack.peek();
		}
		
//		for(XToken token : postfix)
//		{
//			MyDbApp.Log(token.Type.toString() + " " + token.Op + " " + token.Arity);
//		}
		
		if (peek != null)
			throw new Exception("Parentesis mismatch");
	}
	
	Object Eval()
		throws Exception
	{
	    Stack<XValue> stack = new Stack<XValue>();
	    
	    for (XToken token : postfix)
	    {
	    	switch (token.Type)
	    	{
	    		case Variable:
	    			stack.push(new XValue(XValueType.Variable, token.Op, Context));
	    			break;
	    		case Constant:
	    			stack.push(new XValue(XValueType.Constant, token.Op, Context));
	    			break;
	    		case Operator:
	    			if (token.Unary)
	    			{
	    				XValue operand = stack.pop();
	    	            stack.push(new XValue(XValueType.Constant, operator((String)token.Op, operand), Context));
	    			}
	    			else
	    			{
	    				XValue operand2 = stack.pop();
	    				XValue operand1 = stack.pop();
	    	            stack.push(new XValue(XValueType.Constant, operator((String)token.Op, operand1, operand2), Context));
	    			}
	    			break;
	    		case Function:
	    			XValue[] operands = new XValue[token.Arity];
			        for(int i = token.Arity - 1; i >= 0; i--)
			        {
			        	operands[i] = stack.pop();
			        }
			        //stack.push(call_by_name((String)token.Op, operands));
			        stack.push(
		        		new XValue(
	        				XValueType.Constant, 
	        				function((String)token.Op, operands),
	        				Context));
	    			break;
	    	}
	    }
	    
	    return stack.pop().getValue();
	}
	
	boolean last_is_eval()
	{
		if (last_token == null)
			return false;
		else if (last_token.Type == XTokenType.Variable |
				 last_token.Type == XTokenType.Constant |
				 (last_token.Type == XTokenType.Operator & ")".equals(last_token.Op)))
			return true;
		else
			return false;
	}
	
	boolean last_is_function()
	{
		if (last_token == null)
			return false;
		else if (last_token.Type == XTokenType.Function)
	          return true;
        else
          return false;
	}
	
	void popop(boolean only_operator)
		throws Exception
	{
		if (!"".equals(op))
	    {
			boolean is_operator = false;
			for(Object temp : operators.keySet().toArray())
			{
				String operator = (String)temp;
			
				if (operator.equals(op))
				{
					try { process_operator(operator); }
					catch (Exception e) { pos = op_start; throw e; }
			
					is_operator = true;
				}
	        }
	        
	        if (only_operator & !is_operator)
	        {
	          return;
	        }
	        
	        if (!is_operator)
	        {
	        	if (last_is_eval())
	        	{
	        		pos = op_start;
	        		throw new Exception(op + " unexpected");
	        	}

	        	try
	        	{
	        		process_token(XTokenType.Constant, XHelper.Parse(op), false);
	        	}
	        	catch (Exception e)
	        	{
	        	  try { process_token(XTokenType.Variable, op, false); }
	        	  catch (Exception e1) { pos = op_start; throw e; }
	        	}
	        }
	    }
	        
	    op = "";
	    op_term = false;
	}
	
	void readstring()
		throws Exception
	{
		String op = "";
	      
	    while (pos < Expr.length())
	    {
	    	char c = Expr.charAt(pos);
	        
	        if (c == '\'')
	        {
	        	if (pos + 1 < Expr.length() && Expr.charAt(pos + 1) == '\'')
	        	{
	        		op += c;
	        		pos++;
	        	}
	        	else
	        	{
	        		process_token(XTokenType.Constant, op, false);
	        		return;
	        	}
	        }
	        else
	        {
	          op += c;
	        }
	        
	        pos++;
	    }
	      
	    throw new Exception("END unexpected");
	}
	
	void readvariable()
			throws Exception
	{
		String op = "";
	      
	    while (pos < Expr.length())
	    {
	    	char c = Expr.charAt(pos);
	        
	        if (c == '`')
	        {
	        	process_token(XTokenType.Variable, op, false);
	        	return;
	        }
	        else
	        {
	          op += c;
	        }
	        
	        pos++;
	    }
	      
	    throw new Exception("END unexpected");
	}
	
	void process_operator(String operator)
		throws Exception
	{
		if(operators.get(operator).Unary)
		{
			if (last_is_eval())
			{
			    throw new Exception(operator + " unexpected");
			}
			else
			{
				process_token(XTokenType.Operator, operator, true);
			}
		}
		else
	    {
	        if (last_is_eval())
	        {
	          process_token(XTokenType.Operator, operator, false);
	        }
	        else
	        {
	        	if ("-".equals(operator) | "+".equals(operator))
	        	{
	  	          	process_token(XTokenType.Operator, operator, true);
	        	}
	        	else
	        	{
				    throw new Exception(operator + " unexpected");
	        	}
	        }
	    }
	}

	void process_token(XTokenType type, Object op, boolean unary)
		throws Exception
	{
		XToken token = new XToken(type, op, unary);
		switch (type)
	    {
	    	case Variable:
	        case Constant:
	        	postfix.add(token);

	        	XToken peek = stack.isEmpty() ? null : stack.peek();
	        	while (peek != null && peek.Unary)
	        	{
	        		postfix.add(stack.pop());
            		peek = stack.isEmpty() ? null : stack.peek();
	        	}
	        	break;
	        case Operator:
	        	switch ((String)op)
	        	{
	            	case "(":
	            		stack.push(token);
	            		break;
	            	case ")":
	            		peek = stack.isEmpty() ? null : stack.peek();
	            		
	            		while (peek != null &&
	            			   peek.Op != "(" &&
	            			   peek.Op != "{")
	            		{
	    	        		postfix.add(stack.pop());
		            		peek = stack.isEmpty() ? null : stack.peek();
	            		}
	            		
	            		if (peek == null)
	            		{
	            			throw new Exception("Parentheses mismatch");
	            		}
	            		
	            		XToken pop = stack.pop();
	            		if ("{".equals(pop.Op))
	            		{
		            		XToken function = stack.pop();
		            		function.Arity = arity.pop();
			                postfix.add(function);
	            		}
	            		
	            		peek = stack.isEmpty() ? null : stack.peek();
	            		while (peek != null &&
	            			   peek.Unary)
	            		{
	            			postfix.add(stack.pop());
		            		peek = stack.isEmpty() ? null : stack.peek();
	            		}
	            		break;
	            	case ",":
	            		peek = stack.isEmpty() ? null : stack.peek();
	            		while (peek != null &&
	            			   peek.Op != "{")
	            		{
	            			postfix.add(stack.pop());
		            		peek = stack.isEmpty() ? null : stack.peek();
	            		}

	            		if (peek == null)
	            			throw new Exception(", unexpected");

	            		int arity_ = arity.pop();
	            		arity_++;
	            		arity.push(arity_);
            		
	            		break;
		            default:
		            	if (token.Unary)
		            	{
		            		stack.push(token);
		            	}
		            	else
		            	{
		            		peek = stack.isEmpty() ? null : stack.peek();
		            		while (peek != null &&
		            			   peek.Op != "(" &&
		            			   peek.Op != "{" &&
		            			   (operators.get(peek.Op).Precedence > operators.get(token.Op).Precedence | 
		            				operators.get(peek.Op).Precedence == operators.get(token.Op).Precedence	& !operators.get(peek.Op).RightAssoc))
		            		{
		            			postfix.add(stack.pop());
			            		peek = stack.isEmpty() ? null : stack.peek();
		            		}
			                stack.push(token);
		            	}
        		}
	        	break;
	        case Function:
	        	stack.push(token);
	        	stack.push(new XToken(XTokenType.Operator, "{", false));
	        	arity.push(new Integer(1));
	          break;
	      }      
	      
	      last_token = token;
	}
	
	Boolean logical_operator(String operator, Object value1, Object value2)
		throws Exception
	{
		switch(operator)
		{
			case "and": 
				return XHelper.IsTrue(value1) & XHelper.IsTrue(value2);
			case "or":
				return XHelper.IsTrue(value1) | XHelper.IsTrue(value2);
			case "xor":
				return XHelper.IsTrue(value1) ^ XHelper.IsTrue(value2);
			default:
				throw new Exception(operator + " not definded for " + value1.getClass().toString());
		}
	}
	
	Object operator(String operator, Boolean value1, Boolean value2)
		throws Exception
	{
		switch(operator)
		{
			case "=":
				return value1.equals(value2);
			case "<>":
				return !value1.equals(value2);
			default:
				throw new Exception(operator + " is undefined for type boolean.");
		}
	}
	
	Object operator(String operator, Long value1, Long value2)
		throws Exception
	{
		switch(operator)
		{
			case "=":
				return value1.equals(value2);
			case "<>":
				return !value1.equals(value2);
			case ">=":
				return value1 >= value2;
			case ">":
				return value1 > value2;
			case "<":
				return value1 < value2;
			case "<=":
				return value1 <= value2;
			case "+":
				return value1 + value2;
			case "-":
				return value1 - value2;
			case "*":
				return value1 * value2;
			case "/":
				return value1 / value2;
			case "mod":
				return value1 % value2;
			case "**":
				return Math.pow(value1,  value2);
			default:
				throw new Exception(operator + " not definded for " + value1.getClass().toString());
		}
	}
	
	Object operator(String operator, Double value1, Double value2)
		throws Exception
	{
		switch(operator)
		{
			case "=":
				return value1.equals(value2);
			case "<>":
				return !value1.equals(value2);
			case ">=":
				return value1 >= value2;
			case ">":
				return value1 > value2;
			case "<":
				return value1 < value2;
			case "<=":
				return value1 <= value2;
			case "+":
				return value1 + value2;
			case "-":
				return value1 - value2;
			case "*":
				return value1 * value2;
			case "/":
				return value1 / value2;
			case "mod":
				return value1 % value2;
			case "**":
				return Math.pow(value1, value2);
			default:
				throw new Exception(operator + " undifinded for " + value1.getClass().toString());
		}
	}
	
	Object operator(String operator, String value1, String value2)
		throws Exception
	{
		switch(operator)
		{
			case "=":
				return value1.compareTo(value2) == 0;
			case "<>":
				return value1.compareTo(value2) != 0;
			case ">=":
				return value1.compareTo(value2) >= 0;
			case ">":
				return value1.compareTo(value2) > 0;
			case "<":
				return value1.compareTo(value2) < 0;
			case "<=":
				return value1.compareTo(value2) <= 0;
			case "+":
			case "-":
			case "*":
			case "/":
			case "mod":
			case "**":
				return operator(operator, XHelper.Parse(value1), XHelper.Parse(value2));
			default:
				throw new Exception(operator + " not definded for " + value1.getClass().toString());
		}
	}

	Object operator(String operator, Object value1, Object value2)
		throws Exception
	{
		switch(operator)
		{
			case "and": 
			case "or":
			case "xor":
				return logical_operator(operator, value1, value2);
			case "||":
				return XHelper.ToString(value1) + XHelper.ToString(value2);
			default:
				if (value1 == null | value2 == null) return null;

				if (value1.getClass() == Boolean.class | value2.getClass() == Boolean.class)
				{
					return operator(operator, XHelper.IsTrue(value1), XHelper.IsTrue(value2));
				}
				else if (value1.getClass() == Double.class | value2.getClass() == Double.class)
				{
					return operator(operator, XHelper.ToDouble(value1), XHelper.ToDouble(value2));
				}
				else if (value1.getClass() == Long.class | value2.getClass() == Long.class)
				{
					return operator(operator, XHelper.ToLong(value1), XHelper.ToLong(value2));
				}
				else if (value1.getClass() == String.class | value2.getClass() == String.class);
				{
					return operator(operator, (String)value1, (String)value2);
				}
		}
	}

	
	Object operator(String operator, XValue operand)
		throws Exception
	{
		switch (operator)
		{
			case "not":
				return !XHelper.IsTrue(operand.getValue());
			case "-":
				Object value = operand.getValue();
				if (value.getClass() == Long.class)
					return -(Long)value;
				if (value.getClass() == Double.class)
					return -(Double)value;
				throw new Exception(operator + " not definded for " + value.getClass().toString());
		}
		
		return null;
	}

	Object operator(String operator, XValue operand1, XValue operand2)
		throws Exception
	{
		if (operator == ":=")
		{
			if (operand1.Type != XValueType.Variable)
				throw new Exception("LHS of assignment is not a variable");
			
			Object value = operand2.getValue();
			Context.Set((String)operand1.Value, value);
			return value;
		}
		else
		{
			return operator(operator, operand1.getValue(), operand2.getValue());
		}
	}

	Object function(String function, XValue[] operands)
		throws Exception
	{
		switch (function)
		{
			case "range":
				Long start = new Long(0);
				Long stop;
				Long step = new Long(1);
				
				if (operands.length == 1)
				{
					stop = XHelper.ToLong(operands[0].getValue());
				}
				else if (operands.length == 2)
				{
					start = XHelper.ToLong(operands[0].getValue());
					stop =  XHelper.ToLong(operands[1].getValue());
				}
				else if (operands.length == 3)
				{
					start = XHelper.ToLong(operands[0].getValue());
					stop =  XHelper.ToLong(operands[1].getValue());
					step =  XHelper.ToLong(operands[2].getValue());
				}
				else
				{
					throw new Exception("range([<start>,] <stop>[, <step>])");
				}
				
				if (step == 0)
				{
					throw new Exception("range step is 0");
				}
				
				XTree range = new XTree();
				for(Long i = start; step < 0 ? i > stop: i < stop; i += step)
				{
					range.map.put(i.toString(), i);
				}
				
				return range;

			case "isset":
				if (operands[0].Type == XValueType.Variable)
					return Context.IsSet((String)operands[0].Value);
				else
					throw new Exception("isset expects variable");
			
			case "unset":
				if (operands[0].Type == XValueType.Variable)
					Context.UnSet((String)operands[0].Value);
				else
					throw new Exception("usset expects variable");
			
			case "istree":
				Object value = operands[0].getValue();
				return value != null && value.getClass() == XTree.class;
			
			case "parse_str":
				return XUrl.ParseQuery(XHelper.ToString(operands[0].getValue()));
			
			case "parse_url":
				return XUrl.ParseUrl(XHelper.ToString(operands[0].getValue()));
				
			case "build_query":
				return XUrl.BuildQuery((XTree)operands[0].getValue());

			case "preg_match":
				
				if (operands.length == 2)
				{
					String[][] matches = new String[1][];
					boolean ismatch = XRegex.preg_match(XHelper.ToString(operands[0].getValue()), XHelper.ToString(operands[1].getValue()), matches);
					return ismatch;
				}
				else if (operands.length == 3)
				{
					if (operands[2].Type != XValueType.Variable)
						throw new Exception("preg_match expects parameter 3 to be a variable");
					
					String[][] matches = new String[1][];
					boolean ismatch = XRegex.preg_match(XHelper.ToString(operands[0].getValue()), XHelper.ToString(operands[1].getValue()), matches);
					
					XTree tree = new XTree();
					for (int i = 0; i < matches[0].length; i++)
						tree.map.put(XHelper.ToString(i), matches[0][i]);
					Context.Set((String)operands[2].Value, tree);
					
					return ismatch;
				}
				else
				{
					throw new Exception("preg_match(<pattern>, <input>[, <matches>])");
				}

			case "database":
				String host 	= XHelper.ToString(operands[0].getValue());
				String database = XHelper.ToString(operands[1].getValue());
				String user		= XHelper.ToString(operands[2].getValue());
				String password = XHelper.ToString(operands[3].getValue());
				
				return new XDatabase(host, database, user, password);

			case "tree":
				return new XTree();
				
			case "merge":
				XTree[] trees = new XTree[operands.length];
				for(int i = 0; i < operands.length; i++)
				{
					XValue operand = operands[i];
					trees[i] = (XTree)operand.getValue();
				}
				return XHelper.Merge(trees);
				
			case "keys":
				return XHelper.Keys((XTree)operands[0].getValue());
				
			case "url_fillin":
				if (operands.length == 1)
				{
					return XUrl.FillIn(XHelper.ToString(operands[0].getValue()), Context);
				}
				else if (operands.length == 2)
				{
					return XUrl.FillIn(XHelper.ToString(operands[0].getValue()), (XTree)operands[1].getValue());
				}
				else
				{
					throw new Exception("url_fillin(<url>[, <tree>])");
				}
			
			case "urlmerge":
				return XUrl.Merge(
						XHelper.ToString(operands[0].getValue()),
						XHelper.ToString(operands[1].getValue()));

			case "sql_fillin":
				if (operands.length == 1)
				{
					return XDatabase.FillIn(XHelper.ToString(operands[0].getValue()), Context);
				}
				else if (operands.length == 2)
				{
					XContext context = new XContext(new XTree(), (XTree)operands[1].getValue());
					return XDatabase.FillIn(XHelper.ToString(operands[0].getValue()), context);
				}
				else
				{
					throw new Exception("url_fillin(<url>[, <tree>])");
				}
			
			case "parse_query":
				return XUrl.ParseQuery(XHelper.ToString(operands[0].getValue()));
				
			case "trim":
				return (XHelper.ToString(operands[0].getValue())).trim();
				
			case "md5":
				MessageDigest md5 = MessageDigest.getInstance("MD5");
				md5.update((XHelper.ToString(operands[0].getValue())).getBytes("ISO-8859-1"));
				byte[] digest = md5.digest();
				String hash = DatatypeConverter.printHexBinary(digest).toLowerCase(); 
				return hash;

			case "split":
				if (operands.length == 2)
				{
					return XHelper.Split(XHelper.ToString(operands[0].getValue()), XHelper.ToString(operands[1].getValue()));
				}
				else if (operands.length == 3)
				{
					return XHelper.Split(XHelper.ToString(operands[0].getValue()), XHelper.ToString(operands[1].getValue()), XHelper.ToLong(operands[2].getValue()));
				}
				else
				{
					throw new Exception("url_fillin(<url>[, <tree>])");
				}

			case "count":
				return XHelper.Count((XTree)operands[0].getValue());
				
			case "len":
				return XHelper.Length(XHelper.ToString(operands[0].getValue()));
				
			case "replace":
				return XHelper.Replace(
					XHelper.ToString(operands[0].getValue()),
					XHelper.ToString(operands[1].getValue()),
					XHelper.ToString(operands[2].getValue()));
				
			case "fillin":
				return XHelper.FillIn(XHelper.ToString(operands[0].getValue()), Context);
				
			case "html_fillin":
				return XHtml.FillIn(XHelper.ToString(operands[0].getValue()), Context);

			case "number_format":
				return operands[0].getValue();
				
			case "iif":
				return XHelper.IsTrue(operands[0].getValue()) ? operands[1].getValue() : operands[2].getValue();
				
			case "substr":
				if (operands.length == 2)
				{
					return XHelper.ToString(operands[0].getValue()).substring(
							XHelper.ToLong(operands[1].getValue()).intValue());
				}
				else if (operands.length == 3)
				{
					return XHelper.ToString(operands[0].getValue()).substring(
							XHelper.ToLong(operands[1].getValue()).intValue(), 
							XHelper.ToLong(operands[2].getValue()).intValue() -
							XHelper.ToLong(operands[1].getValue()).intValue());
				}
				else
				{
					throw new Exception("sbustr(<string>, <begin>[, <count>])");
				}
				
			case "image_resize":
				return XImage.Resize(
						XHelper.ToString(operands[0].getValue()),
						XHelper.ToString(operands[1].getValue()),
						XHelper.ToLong(operands[2].getValue()),
						XHelper.ToLong(operands[3].getValue()),
						XHelper.ToString(operands[4].getValue()),
						XHelper.ToString(operands[5].getValue()),
						XHelper.ToLong(operands[6].getValue()));
				
			case "exec":
				XContext context;
				if (operands.length == 2)
				{
					context = new XContext(Context.Global);
				}
				else if (operands.length == 3)
				{
					context = new XContext(Context.Global, (XTree)operands[2].getValue());
				}
				else
				{
					throw new Exception("exec(<ident>, <code>[, <tree>])");
				}
				
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				Writer writer = new OutputStreamWriter(outputStream, "ISO-8859-1");
				
				XMachine machine = new XMachine(XHelper.ToString(operands[0].getValue()), XHelper.ToString(operands[1].getValue()), writer, context);
				
				try { machine.Run(); }
				catch (XExit e)
				{
					
				}
				catch (XException e)
				{
					try { writer.append("Exception at " + e.Function + ":" + e.Line + ": " + e.Message + "\n");	}
					catch (IOException e1) { }
				}
				finally
				{
					if (writer != null)
					{
						try { writer.flush(); } 
						catch (IOException e) { }
					}
				}
				
				return new String(outputStream.toByteArray(), "ISO-8859-1");

			case "int":
				return XHelper.ToLong(operands[0].getValue());
				
			default:

				return null;
				//throw new Exception(function + " is undefined");
		}
	}
}
