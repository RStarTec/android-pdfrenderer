package com.android.example.pdfRenderer;


import java.lang.ref.WeakReference;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView.RecyclerListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ViewerFragment extends Fragment {
	private static final String TAG = ViewerFragment.class.getSimpleName()+"_class";
	private static final boolean debug = true;
	
	private static final String EXTRA_filename = ViewerFragment.class.getSimpleName()+".filename";
	private static final int pageRange = 2;  // use 0 for older machines. 

		
	private String mFilename;
	private ListView mListView;
	private BitmapListAdapter mBitmapListAdapter = null;
	private Bitmap mPlaceHolderBitmap;
	private int maxWidth;
	
	private ProgressBar mLoadingView;
		
	// keep a reference to the AsyncTask object
	// Allow the user to cancel the task upon quitting the fragment
	private PdfFetcherAsyncTask mFetcherTask;
	public FileLoader mFileLoader;

	// Supply the filename as an argument to the newly created fragment.
	public static ViewerFragment newInstance(String filename) {
		Bundle args = new Bundle();
		args.putString(EXTRA_filename, filename);
		ViewerFragment fragment = new ViewerFragment();
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		

		mFilename = getArguments().getString(EXTRA_filename);
		Savelog.d(TAG, debug, "onCreate() entered for " + mFilename);

		mFileLoader = null;
		
		/* Create a bitmap that is tall enough to cover the screen. 
		 * This step is very important as the pages will be loaded asynchronously.
		 * If the placeholder is too small, the listview will attempt to load all
		 * the pages even if just 1 is to be displayed at any time. This will cause 
		 * oom err
		 */
		mPlaceHolderBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_help);
		
		mFetcherTask = new PdfFetcherAsyncTask(this, mFilename);
		mFetcherTask.execute();
		
		maxWidth = getScreenMaxDimension(this.getActivity());

		setRetainInstance(true);
	} // end to implementing onCreate()
	
	
	
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View v;
		Savelog.d(TAG, debug, "onCreateView() entered");
		
		v = inflater.inflate(R.layout.fragment_viewer, parent, false);

		mLoadingView = (ProgressBar) v.findViewById(R.id.fragmentPng_loading_id);
		
		mListView = (ListView) v.findViewById(R.id.fragmentPng_list_id);
		
		/* There are three scenarios when we reach this point:
		 * 1. Asyntask is completed and data is available for display
		 * 2. Asyntask is completed and data is NOT available
		 * 3. Asyntask is NOT completed
		 */
		if (mFileLoader==null) {
			Savelog.d(TAG, debug, "bitmap array is null");
			if (mFetcherTask==null || mFetcherTask.getStatus()==AsyncTask.Status.FINISHED) {
				Savelog.d(TAG, debug, "Rendering done");
				onFetchingCompleted();
			}
			else {
				Savelog.d(TAG, debug, "Rendering in progress");
				mListView.setAlpha(0f);
				mListView.setVisibility(View.VISIBLE);
			}
		}
		else {
			Savelog.d(TAG, debug, "bitmap array is not null");
			onFetchingCompleted();
		}
		return v;
	} // end to implementing onCreateView() 
	

	
	public void onFetchingCompleted() {
		mLoadingView.setVisibility(View.GONE);

		mListView.setVisibility(View.VISIBLE);
		mListView.setAlpha(1f);

		if (mFileLoader!=null) {
			Savelog.d(TAG, debug, "bitmap page length  = " + mFileLoader.getNumberOfPages());
			if (mBitmapListAdapter==null) { // Don't reset adapter if already exists
				mBitmapListAdapter = new BitmapListAdapter(this, mFileLoader.getPageNames());
			}
			mListView.setAdapter(mBitmapListAdapter);
			mListView.setRecyclerListener(new RecyclerListener() {
				@Override
				public void onMovedToScrapHeap(View view) {
					final ImageView imageView = (ImageView) view.findViewById(R.id.listItem_bitmap_id);
					Savelog.d(TAG, debug, "Called recycler.");
					imageView.setImageBitmap(null);
					imageView.setTag(null);
				}
			});
 
		}
	}

	
	
	
	
	static class PdfFetcherAsyncTask extends AsyncTask<Object, Void, FileLoader> {
		private String filename;
		private final WeakReference<ViewerFragment> hostFragmentReference;
		private Exception err=null;
		
		public PdfFetcherAsyncTask(ViewerFragment hostFragment, String filename)  {
			super();
			this.filename = filename;
			hostFragmentReference = new WeakReference<ViewerFragment>(hostFragment);
		}
		
		@Override
		protected FileLoader doInBackground(Object... arg0) {
			FileLoader fileLoader = new FileLoader(filename);
			try {
				fileLoader.setup();
			}
			catch (Exception e) {
				err = e;
				Savelog.e(TAG, "Asyntask rendering unsuccessful." + "\n" + Savelog.getStack(e));
			}
			return fileLoader; 
		}

		// once complete, see if ImageView is still around and set up bitmap
		@Override
		protected void onPostExecute(FileLoader fileLoader) {
			super.onPostExecute(fileLoader);
			
			if (isCancelled()) {
				fileLoader = null;
			}
			if (hostFragmentReference != null) {
				final ViewerFragment hostFragment = hostFragmentReference.get();
				if (hostFragment != null) {
					if (fileLoader != null) {
						hostFragment.mFileLoader = fileLoader;
						Toast.makeText(hostFragment.getActivity(), "Found " + fileLoader.getNumberOfPages() + " pages", Toast.LENGTH_SHORT).show();
					}
					else {
						hostFragment.mFileLoader = null;
						if (err!=null) {
							Savelog.e(TAG, "Cannot fetch pdf" + "\n" + Savelog.getStack(err));
						}
						Toast.makeText(hostFragment.getActivity(), "Data unavailable", Toast.LENGTH_SHORT).show();
					}
					// Callback upon finished fetching.
					hostFragment.onFetchingCompleted();
					// Detach host fragment from this task.
					hostFragment.mFetcherTask = null;
				}
			}
			Savelog.d(TAG, debug, "AsyncTask completed.");
		}

		
		@Override
		protected void onCancelled() {
			super.onCancelled();
			Savelog.d(TAG, debug, "AsyncTask canceled.");
		}

	}

	
	
	// Allow user to cancel asynctask by moving to a different fragment
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mFetcherTask!=null) {
			mFetcherTask.cancel(true);
			mFetcherTask = null;
		}

		MainActivity.clearInternalFiles(getActivity());
	}
	
	
	private static class BitmapListAdapter extends BaseAdapter {
		ViewerFragment hostFragment;
		String[] renderedKey;

		public BitmapListAdapter(ViewerFragment hostFragment, String[] renderedKey) {
			super();
			this.hostFragment = hostFragment;
			this.renderedKey = renderedKey;
		}
		@Override
		public int getCount() {
			return renderedKey.length;
		}
		@Override
		public Object getItem(int position) {
			return renderedKey[position];
		}
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;
			
			if (convertView==null) {
				convertView = hostFragment.getActivity().getLayoutInflater()
				.inflate(R.layout.list_item_bitmap, parent, false);
			}
			Savelog.d(TAG, debug, "png listview getView " + position);
			imageView = (ImageView) convertView.findViewById(R.id.listItem_bitmap_id);
			
			String key = (String)getItem(position);
			imageView.setTag(key);
			
			loadBitmap(hostFragment, key, position, imageView);

			return convertView;
		}

	}
	
	
		
	
	public static void loadBitmap(ViewerFragment hostFragment, String key, int position, ImageView imageView) {
		if (cancelPotentialWork(key, imageView)) {
			final BitmapWorkerTask task = new BitmapWorkerTask(hostFragment, imageView);
			final AsyncDrawable asyncDrawable = new AsyncDrawable(hostFragment.getResources(), hostFragment.mPlaceHolderBitmap, task);
			imageView.setImageDrawable(asyncDrawable);
			task.execute(key, position);
		}
		
	}
	
	
	
	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap placeHolderBitmap, BitmapWorkerTask bitmapWorkerTask) {
			super(res, placeHolderBitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
		}
		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}
	
	
	public static boolean cancelPotentialWork(String key, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
		if (bitmapWorkerTask != null) {
			final String bitmapKey = bitmapWorkerTask.key;
			if (bitmapKey != key) {
				// Cancel previous task
				Savelog.d(TAG, debug, "Canceling task for key:" + key);
				bitmapWorkerTask.cancel(true);
			} else {
				// The same work is already in progress
				return false;
			}
		}

		// No task associated with the ImageView, or an existing task was cancelled
		return true;
	}
	
	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}
	
	
	
	private static class BitmapWorkerTask extends AsyncTask<Object, Void, Bitmap> {
		WeakReference<ImageView> imageViewReference;
		private final WeakReference<ViewerFragment> hostFragmentReference;
		private Context appContext;
		private FileLoader fileLoader;
		private int maxWidth;
		private String key = null;
		
		public BitmapWorkerTask(ViewerFragment hostFragment, ImageView imageView) {
			imageViewReference = new WeakReference<ImageView>(imageView);
			this.appContext = hostFragment.getActivity().getApplicationContext();
			this.hostFragmentReference = new WeakReference<ViewerFragment>(hostFragment);
			this.fileLoader = hostFragment.mFileLoader;
			this.maxWidth = hostFragment.maxWidth;
		}
		
		@Override
		protected Bitmap doInBackground(Object... params) {
			Bitmap bitmap = null;
			key = (String) params[0];
			int position = (Integer) params[1];
			Savelog.d(TAG, debug, "AsyncTask trying to load bitmap from cache");

			if (fileLoader==null) {
				Savelog.e(TAG, "file loader is null.");
			}
			else {
				int pageNumberNeeded = position+1;
				bitmap = fileLoader.load(appContext, maxWidth, pageNumberNeeded, pageRange);
			}
			// bitmap could be null if oom error or cache flushed out.
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (isCancelled()) {
				bitmap = null;
			}
			if (imageViewReference != null && bitmap != null) {
				final ImageView imageView = imageViewReference.get();
				final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
				if (this == bitmapWorkerTask && imageView != null) {
					imageView.setImageBitmap(bitmap);
				}
			}
			else {
				Savelog.e(TAG, "Unable to load " + key);
				if (hostFragmentReference!=null) {
					ViewerFragment hostFragment = hostFragmentReference.get();
					Activity activity = hostFragment.getActivity();
					if (activity!=null) {
						if (bitmap==null) {
							Savelog.e(TAG, "bitmap is null. Possibly due to OOM error");
							Toast.makeText(hostFragment.getActivity(), "Try to refresh this page by scrolling slowly.", Toast.LENGTH_SHORT).show();
						}
						else {
							Savelog.d(TAG, debug, "bitmap is not null for key " + key);
						}
					}
	
				}
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	public static int getScreenMaxDimension(Context appContext) {
		// Get maximum width to be rendered (the width of the screen)
		WindowManager wm = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		int maxDimen;
		if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB_MR2) {
			Point size = new Point();
			display.getSize(size);
			maxDimen = size.x > size.y ? size.x : size.y;
		}
		else {
			maxDimen = (display.getWidth() > display.getHeight()) ? display.getWidth() : display.getHeight();
		}
		return maxDimen;
	}
}
