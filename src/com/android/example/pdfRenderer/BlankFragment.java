package com.android.example.pdfRenderer;


// The Dummy fragment is used when there is no file to display


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class BlankFragment extends Fragment {
	private static final String TAG = BlankFragment.class.getSimpleName()+"_class";
	private static final boolean debug = true;
	
	private static final String EXTRA_message = BlankFragment.class.getSimpleName()+".message";
	
	private String mMessage = "";
	
	// Supply a message as an argument to the newly created fragment.
	public static BlankFragment newInstance(String message) {
		Bundle args = new Bundle();
		args.putString(EXTRA_message, message);
		BlankFragment fragment = new BlankFragment();
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		Savelog.d(TAG, debug, "onCreate()");
		mMessage = getArguments().getString(EXTRA_message);
	} // end to implementing onCreate()
	
	
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View v;
		
		v = inflater.inflate(R.layout.fragment_blank, parent, false);

		TextView statusView = (TextView) v.findViewById(R.id.blank_status);
		statusView.setText(mMessage);
		
		return v;
	} // end to implementing onCreateView() 
	
}