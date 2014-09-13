package com.sun.pdfview.function.postscript.operation;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import pdf.main.SavelogPDF;

import com.sun.pdfview.function.postscript.PostScriptParser;


final class If implements PostScriptOperation {
	private static final String TAG = If.class.getSimpleName()+"_class";
	private static final boolean debug = false;

	
	private List<String> tokens;
	
	@Override
	/**
	 * <i>bool {proc}</i> <b>if</b> - <p>
	 *
	 * removes both operands from the stack, then executes proc
	 * if bool is true. The if operator pushes no results of
	 * its own on the operand stack, but proc may do so (see
	 * Section 3.5, "Execution"). <p>
	 *
	 * Examples <p>
	 * 3 4 lt {(3 is less than 4)} if <p>
	 *
	 * @modified 06.24.2013 by A.Hui 
	 */
	public void eval(Stack<Object> environment) {
		SavelogPDF.d(TAG, debug, "eval");
		String expr = (String) environment.pop();
		Boolean bool = (Boolean) environment.pop();
		
		if (bool) {
			// TODO: evaluate expr and store value
	    	this.tokens = new PostScriptParser().parse(expr);
			// Assume this is a very simple expression without any sub-expressions
	    	
	    	for (Iterator<String> iterator = this.tokens.iterator(); iterator.hasNext(); ) {
				String token = iterator.next();
				
				PostScriptOperation op = OperationSet.getInstance().getOperation(token);
				op.eval(environment);
			}

		}
	}
}
