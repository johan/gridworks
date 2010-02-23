package com.metaweb.gridworks.expr;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.metaweb.gridworks.expr.Scanner.NumberToken;
import com.metaweb.gridworks.expr.Scanner.Token;
import com.metaweb.gridworks.expr.Scanner.TokenType;
import com.metaweb.gridworks.expr.controls.ForEach;
import com.metaweb.gridworks.expr.controls.ForNonBlank;
import com.metaweb.gridworks.expr.controls.If;
import com.metaweb.gridworks.expr.controls.With;
import com.metaweb.gridworks.expr.functions.And;
import com.metaweb.gridworks.expr.functions.Ceil;
import com.metaweb.gridworks.expr.functions.EndsWith;
import com.metaweb.gridworks.expr.functions.Floor;
import com.metaweb.gridworks.expr.functions.Get;
import com.metaweb.gridworks.expr.functions.IndexOf;
import com.metaweb.gridworks.expr.functions.IsBlank;
import com.metaweb.gridworks.expr.functions.IsNotBlank;
import com.metaweb.gridworks.expr.functions.IsNotNull;
import com.metaweb.gridworks.expr.functions.IsNull;
import com.metaweb.gridworks.expr.functions.Join;
import com.metaweb.gridworks.expr.functions.LastIndexOf;
import com.metaweb.gridworks.expr.functions.Length;
import com.metaweb.gridworks.expr.functions.Ln;
import com.metaweb.gridworks.expr.functions.Log;
import com.metaweb.gridworks.expr.functions.Max;
import com.metaweb.gridworks.expr.functions.Min;
import com.metaweb.gridworks.expr.functions.Mod;
import com.metaweb.gridworks.expr.functions.Not;
import com.metaweb.gridworks.expr.functions.Or;
import com.metaweb.gridworks.expr.functions.Replace;
import com.metaweb.gridworks.expr.functions.Round;
import com.metaweb.gridworks.expr.functions.Slice;
import com.metaweb.gridworks.expr.functions.Split;
import com.metaweb.gridworks.expr.functions.StartsWith;
import com.metaweb.gridworks.expr.functions.ToLowercase;
import com.metaweb.gridworks.expr.functions.ToNumber;
import com.metaweb.gridworks.expr.functions.ToString;
import com.metaweb.gridworks.expr.functions.ToTitlecase;
import com.metaweb.gridworks.expr.functions.ToUppercase;

public class Parser {
	protected Scanner 	_scanner;
	protected Token 	_token;
	protected Evaluable _root;
	
	static public Map<String, Function> functionTable = new HashMap<String, Function>();
    static public Map<String, Control> controlTable = new HashMap<String, Control>();
    
	static {
		functionTable.put("toString", new ToString());
		functionTable.put("toNumber", new ToNumber());
		
		functionTable.put("toUppercase", new ToUppercase());
		functionTable.put("toLowercase", new ToLowercase());
		functionTable.put("toTitlecase", new ToTitlecase());
		
		functionTable.put("get", new Get());
		functionTable.put("slice", new Slice());
		functionTable.put("substring", new Slice());
		functionTable.put("replace", new Replace());
		functionTable.put("split", new Split());
		functionTable.put("length", new Length());
		
		functionTable.put("indexOf", new IndexOf());
		functionTable.put("lastIndexOf", new LastIndexOf());
		functionTable.put("startsWith", new StartsWith());
		functionTable.put("endsWith", new EndsWith());
		functionTable.put("join", new Join());
		
		functionTable.put("round", new Round());
		functionTable.put("floor", new Floor());
		functionTable.put("ceil", new Ceil());
		functionTable.put("mod", new Mod());
		functionTable.put("max", new Max());
		functionTable.put("min", new Min());
		functionTable.put("log", new Log());
		functionTable.put("ln", new Ln());
		
		functionTable.put("and", new And());
		functionTable.put("or", new Or());
		functionTable.put("not", new Not());
		functionTable.put("isNull", new IsNull());
		functionTable.put("isNotNull", new IsNotNull());
        functionTable.put("isBlank", new IsBlank());
        functionTable.put("isNotBlank", new IsNotBlank());

        controlTable.put("if", new If());
        controlTable.put("with", new With());
        controlTable.put("forEach", new ForEach());
        controlTable.put("forNonBlank", new ForNonBlank());
    }
    
	public Parser(String s) throws Exception {
		this(s, 0, s.length());
	}
	
	public Parser(String s, int from, int to) throws Exception {
		_scanner = new Scanner(s, from, to);
		_token = _scanner.next();
		
		_root = parseExpression();
	}
	
	public Evaluable getExpression() {
		return _root;
	}
	
	protected void next() {
		_token = _scanner.next();
	}
	
	protected Exception makeException(String desc) {
		int index = _token != null ? _token.start : _scanner.getIndex();
		
		return new Exception("Parsing error at offset " + index + ": " + desc);
	}
	
	protected Evaluable parseExpression() throws Exception {
		Evaluable sub = parseSubExpression();
		
		while (_token != null && 
				_token.type == TokenType.Operator && 
				">=<==!=".indexOf(_token.text) >= 0) {
			
			String op = _token.text;
			
			next();
			
			Evaluable sub2 = parseSubExpression();
			
			sub = new OperatorCallExpr(new Evaluable[] { sub, sub2 }, op);
		}
		
		return sub;
	}
	
	protected Evaluable parseSubExpression() throws Exception {
		Evaluable sub = parseTerm();
		
		while (_token != null && 
				_token.type == TokenType.Operator && 
				"+-".indexOf(_token.text) >= 0) {
			
			String op = _token.text;
			
			next();
			
			Evaluable sub2 = parseSubExpression();
			
			sub = new OperatorCallExpr(new Evaluable[] { sub, sub2 }, op);
		}
		
		return sub;
	}
	
	protected Evaluable parseTerm() throws Exception {
		Evaluable factor = parseFactor();
		
		while (_token != null && 
				_token.type == TokenType.Operator && 
				"*/".indexOf(_token.text) >= 0) {
			
			String op = _token.text;
			
			next();
			
			Evaluable factor2 = parseFactor();
			
			factor = new OperatorCallExpr(new Evaluable[] { factor, factor2 }, op);
		}
		
		return factor;
	}
	
	protected Evaluable parseFactor() throws Exception {
		if (_token == null) {
			throw makeException("Expression ends too early");
		}
		
		Evaluable eval = null;
		
		if (_token.type == TokenType.String) {
			eval = new LiteralExpr(_token.text);
			next();
		} else if (_token.type == TokenType.Number) {
			eval = new LiteralExpr(((NumberToken)_token).value);
			next();
		} else if (_token.type == TokenType.Operator && _token.text.equals("-")) { // unary minus?
			next();
			
			if (_token != null && _token.type == TokenType.Number) {
				eval = new LiteralExpr(-((NumberToken)_token).value);
				next();
			} else {
				throw makeException("Bad negative number");
			}
		} else if (_token.type == TokenType.Identifier) {
			String text = _token.text;
			next();
			
			if (_token == null || _token.type != TokenType.Delimiter || !_token.text.equals("(")) {
				eval = new VariableExpr(text);
			} else {
				Function f = functionTable.get(text);
				Control c = controlTable.get(text);
				if (f == null && c == null) {
					throw makeException("Unknown function or control named " + text);
				}
				
				next(); // swallow (
				
				List<Evaluable> args = parseExpressionList(")");
				
				if (c != null) {
                    eval = new ControlCallExpr(makeArray(args), c);
				} else {
				    eval = new FunctionCallExpr(makeArray(args), f);
				}
			}
		} else if (_token.type == TokenType.Delimiter && _token.text.equals("(")) {
			next();
			
			eval = parseExpression();
			
			if (_token != null && _token.type == TokenType.Delimiter && _token.text.equals(")")) {
				next();
			} else {
				throw makeException("Missing )");
			}
		} else {
			throw makeException("Missing number, string, identifier, or parenthesized expression");
		}
		
		while (_token != null) {
			if (_token.type == TokenType.Operator && _token.text.equals(".")) {
				next(); // swallow .
				
				if (_token == null || _token.type != TokenType.Identifier) {
					throw makeException("Missing function name");
				}
				
				String identifier = _token.text;
				next();
				
				if (_token != null && _token.type == TokenType.Delimiter && _token.text.equals("(")) {
					next(); // swallow (
					
					Function f = functionTable.get(identifier);
					if (f == null) {
						throw makeException("Unknown function " + identifier);
					}
					
					List<Evaluable> args = parseExpressionList(")");
					args.add(0, eval);
					
					eval = new FunctionCallExpr(makeArray(args), f);
				} else {
					eval = new FieldAccessorExpr(eval, identifier);
				}
			} else if (_token.type == TokenType.Delimiter && _token.text.equals("[")) {
				next(); // swallow [
				
				List<Evaluable> args = parseExpressionList("]");
				args.add(0, eval);
				
				eval = new FunctionCallExpr(makeArray(args), functionTable.get("get"));
			} else {
				break;
			}
		}
		
		return eval;
	}
	
	protected List<Evaluable> parseExpressionList(String closingDelimiter) throws Exception {
		List<Evaluable> l = new LinkedList<Evaluable>();
		
		if (_token != null && 
			(_token.type != TokenType.Delimiter || !_token.text.equals(closingDelimiter))) {
			
			while (_token != null) {
				Evaluable eval = parseExpression();
				
				l.add(eval);
				
				if (_token != null && _token.type == TokenType.Delimiter && _token.text.equals(",")) {
					next(); // swallow comma, loop back for more
				} else {
					break;
				}
			}
		}
		
		if (_token != null && _token.type == TokenType.Delimiter && _token.text.equals(closingDelimiter)) {
			next(); // swallow closing delimiter
		} else {
			throw makeException("Missing " + closingDelimiter);
		}
		
		return l;
	}
	
	protected Evaluable[] makeArray(List<Evaluable> l) {
		Evaluable[] a = new Evaluable[l.size()];
		l.toArray(a);
		
		return a;
	}
}
