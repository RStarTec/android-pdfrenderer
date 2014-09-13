package com.android.example.pdfRenderer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class OpenDialogFragment extends DialogFragment {
	public static final String TAG = OpenDialogFragment.class.getSimpleName()+"_class";
	private static final boolean debug = true;
	
	
	private static final String EXTRA_filename = OpenDialogFragment.class.getSimpleName()+ ".filename";
	public static final String dialogTag = OpenDialogFragment.class.getSimpleName()+"_tag";

	private File[] mFileList = null;
	private String mFilename = null;

	private TextView mFilenameView = null;
	private ListView mListView = null;
	private Dialog mDialog = null;
	
	// The following are for memory management to avoid leaks
	private Button mOkButton = null;
	private Button mCancelButton = null;
	private OkOnClickListener mOkOnClickListener = null;
	private CancelOnClickListener mCancelOnClickListener = null;
	private FilenameTextWatcher mFilenameTextWatcher = null;
	private ListOnItemClickListener mListOnItemClickListener = null;
	
	public static OpenDialogFragment newInstance() {
		
		OpenDialogFragment fragment = new OpenDialogFragment();
		
		Bundle args = new Bundle();
		args.putString(EXTRA_filename, "");  // initial value is empty
		fragment.setArguments(args);
		return fragment;
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mFilename = getArguments().getString(EXTRA_filename);
		
		// If directory does not exist, create an empty fileList
		mFileList = getExternalPdfFileList();
	}

		
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_open, null);

		mFilenameView = (TextView) v.findViewById(R.id.dialogOpen_filename_id);
		mFilenameView.setText(mFilename);
		mFilenameTextWatcher = new FilenameTextWatcher(this);
		mFilenameView.addTextChangedListener(mFilenameTextWatcher);
		
		// Show the directory where the files are to be loaded.
		TextView pathView = (TextView) v.findViewById(R.id.dialogOpen_path_id);
		pathView.setText(FileLoader.getExternalDirectory().getAbsolutePath() + ":");
		
		mListView = (ListView) v.findViewById(R.id.dialogOpen_filelist_id);
		FileListAdapter adapter = new FileListAdapter(mFileList);
		mListView.setAdapter(adapter);

		mListOnItemClickListener = new ListOnItemClickListener(this);
		mListView.setOnItemClickListener(mListOnItemClickListener);
		
		mOkOnClickListener = new OkOnClickListener(this);
		mCancelOnClickListener = new CancelOnClickListener();

		/* Use the Builder class for convenient dialog construction
		 * The dialog builder just needs to handle OK and Cancel.
		 */
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(v)
			.setPositiveButton(R.string.button_OK, mOkOnClickListener)
			.setNegativeButton(R.string.button_cancel, mCancelOnClickListener);
		
		mDialog = builder.create();
		
		mDialog.show(); // Never call show() in other dialogfragments which do not need to control the buttons.		
		
		// Set up the checkbox and the ok button based on existing modes and chosenFilename.
		// Do this after showing the dialog.
		if (mFilename == null || mFilename.equals("") ) {
			((AlertDialog) mDialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
		}
		else {
			((AlertDialog) mDialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
		}
		return mDialog;
		
	} // end to onCreateDialog()
	
	
	@Override
	public void onStart() {
		super.onStart();
		AlertDialog d = (AlertDialog) getDialog();
		if (d!=null) {
			mOkButton = d.getButton(DialogInterface.BUTTON_POSITIVE);
			mCancelButton = d.getButton(DialogInterface.BUTTON_NEGATIVE);
		}
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (mOkButton!=null) {
			mOkButton.setOnClickListener(null);
			mOkButton = null;
		}
		if (mOkOnClickListener!=null) {
			mOkOnClickListener.cleanup();
			mOkOnClickListener = null;
		}
		if (mCancelButton!=null) {
			mCancelButton.setOnClickListener(null);
			mCancelButton = null;
		}
		if (mCancelOnClickListener!=null) {
			mCancelOnClickListener = null;
		}

		mFilenameView.removeTextChangedListener(mFilenameTextWatcher);
		mFilenameTextWatcher.cleanup();
		mFilenameTextWatcher = null;
		
		mListView.setOnItemClickListener(null);
		mListOnItemClickListener.cleanup();
		mListOnItemClickListener = null;

		mDialog = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mFileList = null;
	}
	
	
	private class FileListAdapter extends ArrayAdapter<File> {
		public FileListAdapter(File[] fileList) {
			super(getActivity(), android.R.layout.simple_list_item_1, fileList);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView==null) {
				convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_filename, parent, false);
			}
			File item = getItem(position);			
			TextView filenameView = (TextView) convertView.findViewById(R.id.listItem_filename_id);
			filenameView.setText(item.getName());
			return convertView;
		}
		
		@Override
		public int getCount() {
			return mFileList.length;
		}
	}
	
	
	
	private static class ListOnItemClickListener implements OnItemClickListener {
		OpenDialogFragment hostFragment;
		public ListOnItemClickListener(OpenDialogFragment hostFragment) {
			super();
			this.hostFragment = hostFragment;
		}
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
			// Get the file selected by the user
			File fileChosen = (File) hostFragment.mListView.getAdapter().getItem(position);
			
			// Remove the front path, but keep the extension. 
			String rawFilename = fileChosen.toString();
			
			int sep = rawFilename.lastIndexOf("/");
			String filename = rawFilename.substring(sep+1);
			
			if (filename!=null) {					
				hostFragment.mFilenameView.setText(filename);
				if (filename.length()>0) {
					((AlertDialog) hostFragment.mDialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
				}
			}
		}
		public void cleanup() { hostFragment = null; }
	}
	
	
	private static class FilenameTextWatcher implements TextWatcher {
		OpenDialogFragment hostFragment;
		public FilenameTextWatcher(OpenDialogFragment hostFragment) {
			super();
			this.hostFragment = hostFragment;
		}
		@Override
		public void afterTextChanged(Editable arg0) {}
		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
		@Override
		public void onTextChanged(CharSequence c, int start, int before, int count) {
			if (c==null)
				hostFragment.mFilename = "";
			else
				hostFragment.mFilename = c.toString().trim();			
			hostFragment.getArguments().putString(EXTRA_filename, hostFragment.mFilename);
		}
		public void cleanup() { hostFragment = null; }
	}	

	
	static private class OkOnClickListener implements DialogInterface.OnClickListener {
		OpenDialogFragment hostFragment;
		public OkOnClickListener(OpenDialogFragment hostFragment) {
			super();
			this.hostFragment = hostFragment;
		}
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// Normally filename will not be "". But check to be safe.
			if (hostFragment.mFilename == null || hostFragment.mFilename.equals("") ) {
				Toast.makeText(hostFragment.getActivity(), "Invalid file name.", Toast.LENGTH_SHORT).show();
			}
			else {	
				// return filename to main activity.
				((Callbacks) (hostFragment.getActivity())).onFinishOpenDialog(hostFragment.mFilename);
			}
		}
		public void cleanup() { hostFragment = null; }
	}

	static private class CancelOnClickListener implements DialogInterface.OnClickListener {
		public CancelOnClickListener() {
			super();
		}
		@Override
		public void onClick(DialogInterface dialog, int which) {
			Savelog.d(TAG, debug, "User said cancel.");
		}
	}

	
	
	private File[] getExternalPdfFileList() {
		File root = FileLoader.getExternalDirectory();
		Savelog.d(TAG, debug, "path=" + root.getAbsolutePath());

		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				if (filename.toLowerCase(Locale.getDefault()).endsWith(".pdf")) return true;
				else return false;
			}
		};
		File[] list = root.listFiles(filter);
		return list;
	}
	
	
	
	public interface Callbacks {
		void onFinishOpenDialog(String filename);
	}


}
