package com.sun.pdfview.function.postscript.operation;

import java.util.Stack;

import pdf.main.SavelogPDF;


final class Dup implements PostScriptOperation {
	private static final String TAG = Dup.class.getSimpleName()+"_class";
	private static final boolean debug = false;

	@Override
	/**
	 * <i>any</i> <b>dup</b> <i>any any</i> <p>
	 *
	 * duplicates the top element on the operand stack.
	 * dup copies only the object; the value of a composite
	 * object is not copied but is shared.
	 * See Section 3.3, "Data Types and Objects." <p>
	 *
	 * errors: stackoverflow, stackunderflow
	 */
	public void eval(Stack<Object> environment) {
		SavelogPDF.d(TAG, debug, "eval");

		Object obj = environment.pop();
	    environment.push(obj);
	    environment.push(obj);
	}
}
