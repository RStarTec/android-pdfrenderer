package com.sun.pdfview.function.postscript.operation;

import java.util.ArrayList;
import java.util.Stack;

import pdf.main.SavelogPDF;


final class Roll implements PostScriptOperation {
	private static final String TAG = Roll.class.getSimpleName()+"_class";
	private static final boolean debug = false;

	@Override
	public void eval(Stack<Object> environment) {
		SavelogPDF.d(TAG, debug, "eval");
		SavelogPDF.d(TAG, debug, "stack size=" + environment.size());

	    // <i>anyn-1 ... any0 n j</i> <b>roll</b> <i>any(j-1)mod n ... anyn-1 ... any</i>
	    // Roll n elements up j times

		
		Double j = (Double) environment.pop();
		Double n = (Double) environment.pop();
		
		int jval = j.intValue();
		int nval = n.intValue();
		SavelogPDF.d(TAG, debug, "items to rotate=" + nval + " direction=" + jval);
		
		ArrayList<Object> items = new ArrayList<Object>();
		for (int count=0; count<nval; count++) {
			SavelogPDF.d(TAG, debug, " getting " + count);
			items.add(environment.pop()); 
		}
		
		// Reverse the order so as to 
		// store the items in the same order as they are on the stack,
		// 0 is the bottom, nval-1 is top
		ArrayList<Object> itemsRev = new ArrayList<Object>();
		for (int count=0; count<nval; count++) {
			itemsRev.add(items.get(nval-count-1));
		}
		items = itemsRev;
		
		SavelogPDF.d(TAG, debug, " got all items");
		
		if (jval>0) { // rotate by moving right
			for (int count=0; count<jval; count++) {
				Object last = items.remove(nval-1);
				items.add(0, last);
			}
		}
		else if (jval<0) { // rotate by moving left
			for (int count=0; count<-jval; count++) {
				Object first = items.remove(0);
				items.add(nval-1, first);
			}
		}
		
		for (int count=0; count<nval; count++) {
			environment.push(items.get(count)); // Restore the items to the stack, 0 is the bottom, nval-1 is top
		}
		
		
	}
}
