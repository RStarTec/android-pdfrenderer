package pdf.main;


public class SavelogPDF {
	// Call your own Savelog if you have it defined in your project. 
	// Otherwise, use Log as the default logging alternative.

	public static void i(String tag, String message) {
		com.android.example.pdfRenderer.Savelog.i(tag, message);
		// Default alternative: 
		// Log.i(tag, message);
	}
	public static void e(String tag, String message) {
		com.android.example.pdfRenderer.Savelog.e(tag, message);
		// Default alternative: 
		// Log.e(tag, message);
	}
	public static void d(String tag, boolean debug, String message) {
		com.android.example.pdfRenderer.Savelog.d(tag, debug, message);
		// Default alternative: 
		// if (debug) Log.d(tag, message);
	}

	public static void w(String tag, String message) {
		com.android.example.pdfRenderer.Savelog.w(tag, message);
		// Default alternative: 
		// Log.w(tag, message);
	}

	public static String getStack(Throwable e) {
		return com.android.example.pdfRenderer.Savelog.getStack(e);
		// Default alternative: 
		/*
		String data = "";
		StackTraceElement[] errStack = e.getStackTrace();
		for (StackTraceElement er : errStack) {
			data += er.toString() + "\n";
		}
		return data;
		*/
	}
}
