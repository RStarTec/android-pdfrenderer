package com.android.example.pdfRenderer;


import java.io.File;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends Activity implements OpenDialogFragment.Callbacks {
	private static final String TAG = MainActivity.class.getSimpleName()+"_class";
	private static final boolean debug = true;
	
	
	// A handler object, used for deferring UI operations.
	private Handler mHandler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Savelog.d(TAG, debug, "onCreate()");
		
		FragmentManager fm = getFragmentManager();
		Fragment oldViewerFragment = fm.findFragmentById(R.id.activityMain_container_id);
		if (oldViewerFragment==null)
			refreshFragment(null);
		
		Savelog.d(TAG, debug, "onCreate() completed");
	}
	

	
	public boolean onCreateOptionsMenu(Menu menu) {		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_open:
			{
				FragmentManager fm = getFragmentManager();
				OpenDialogFragment openDialog = OpenDialogFragment.newInstance();
				openDialog.show(fm, OpenDialogFragment.dialogTag);
				return true;
			}
			case R.id.menu_emptyInternal:
			{
				clearInternalFiles(this);
				return true;
			}
			
			default:
				return super.onOptionsItemSelected(item);
		}

	}
	
	

	private void refreshFragment(String filename) {
		// Force a change of fragments
		
		// First, check if there are already fragments
		FragmentManager fm = getFragmentManager();
		Fragment oldViewerFragment = fm.findFragmentById(R.id.activityMain_container_id);

		// Create the new fragments
		Fragment newViewerFragment = getNewViewerFragment(this, filename);

		// Now hook up the fragments through a fragment transaction
		FragmentTransaction ft = fm.beginTransaction();

		if (oldViewerFragment!=null) {
			ft.replace(R.id.activityMain_container_id, newViewerFragment);
		}
		else
			ft.add(R.id.activityMain_container_id, newViewerFragment);
		
		// Finalize the fragment change
		ft.commitAllowingStateLoss();
		

		// Defer an invalidation of the options menu (on modern devices, the action bar). This
		// can't be done immediately because the transaction may not yet be committed. Commits
		// are asynchronous in that they are posted to the main thread's message loop.
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				invalidateOptionsMenu();
			}
		});
	}


	
	private Fragment getNewViewerFragment(Context context, String filename) {
		if (filename!=null) {
			try {
				return ViewerFragment.newInstance(filename);
			}
			catch (Exception e) {
				Savelog.e(TAG, "ViewFragment failed to create" + Savelog.getStack(e));
				return BlankFragment.newInstance("Unable to open file "+filename);
			}
		}
		else {
			return BlankFragment.newInstance("No file opened yet.");
		}
	}
	
	
	public static void clearInternalFiles(Context context) {
		File[] internalFiles = context.getFilesDir().listFiles();
		for (File f : internalFiles) {
			Savelog.d(TAG, debug, "Deleting internal file " + f.getName());
			f.delete();
		}
	}



	@Override
	public void onFinishOpenDialog(String filename) {
		Savelog.d(TAG, debug, "going to change to " + filename);
		refreshFragment(filename);
	}

}
