package com.android.example.pdfRenderer;
import android.util.Log;


// Primarily used as a means to centralize the processing of log messages.
// May introduce a log file to save the log messages here, if desired.

public class Savelog {
	public static final String TAG = Savelog.class.getSimpleName()+"_class";
	
	public static String getStack(Throwable err) {
		String data = "";
		StackTraceElement[] errStack = err.getStackTrace();
		for (StackTraceElement e : errStack) {
			data += e.toString() + "\n";
		}
		return data;
	}
	
	public static void d(String tag, boolean debug, String message) {
		if (debug) {
			Log.d(tag, message);
		}
	}
	
	public static void i(String tag, String message) {
		Log.i(tag, message);
	}
	public static void e(String tag, String message) {
		Log.e(tag, message);
	}
	public static void w(String tag, String message) {
		Log.w(tag, message);
	}

}
