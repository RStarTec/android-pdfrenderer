package net.sf.andpdf.refs;

import pdf.main.SavelogPDF;


public class SoftReference<T> {
	private static final String TAG = SoftReference.class.getSimpleName()+"_class";
	private static final boolean debug = false;
	
	java.lang.ref.SoftReference<T> softRef;
	HardReference<T> hardRef;
	
	public SoftReference(T o) {
		if (HardReference.sKeepCaches)
			hardRef = new HardReference<T>(o);
		else
			softRef = new java.lang.ref.SoftReference<T>(o);
	}

	public T get() {
		if (HardReference.sKeepCaches) {
			SavelogPDF.d(TAG, debug, "Requesting softreference implemented as hard-ref");
			return hardRef.get();
		}
		else {
			SavelogPDF.d(TAG, debug, "Requesting softreference implemented as soft-ref");
			return softRef.get();
		}
	}
	
}
