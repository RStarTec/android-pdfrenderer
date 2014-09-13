package com.sun.pdfview.function.postscript.operation;

import java.util.Stack;



public class PushAsExpression implements PostScriptOperation {
	String expr;

	/*************************************************************************
	 * Constructor
	 * @param expr
	 * Push an un-parsed expression (in a string form) into the stack
	 ************************************************************************/

	public PushAsExpression(String expr) {
		super();
		this.expr = expr;
	}
	

	@Override
	public void eval(Stack<Object> environment) {
		environment.push(expr);
	}
}
