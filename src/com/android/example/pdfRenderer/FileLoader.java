package com.android.example.pdfRenderer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import pdf.main.Renderer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

public class FileLoader {
	private static final String TAG = FileLoader.class.getSimpleName()+"_class";
	private static final boolean debug = true;
	
	// always save rendered image file in png 100% quality
	private static final Bitmap.CompressFormat ImageFormat = Bitmap.CompressFormat.PNG;
	private static final int ImageQuality = 100;
	private static final String ImageExtension = ".png";
	
	private static final Object fileLoaderLock = new Object();

	private String filename = "";
	private int numberOfPages = 0;
	private String pageNames[] = null;
	private File pdfFilePath = null;
	
	public FileLoader(String filename) {
		if (filename!=null) this.filename = filename;
	}
	
	// Need asyntask for peeking
	public void setup() {
		if (filename==null || filename.length()==0) return;
		
		// Now check if pdf file exists
		pdfFilePath = getExternalFileIfExists(filename);
		if (pdfFilePath==null) return;

		// May download file here if not already found on device.
		
		Renderer renderer = new Renderer();
		numberOfPages = renderer.peekNumberOfPages(pdfFilePath);
		renderer.cleanup();
		
		pageNames = new String[numberOfPages];

		// Create the rendered pagename for each page
		for (int pageNumber=1; pageNumber<=numberOfPages; pageNumber++) {
			int index = pageNumber-1;
			pageNames[index] = getRenderedName(pdfFilePath.getName(), pageNumber);
		}
	}
	
	
	
	// Need asyncTask for rendering
	/* This function performs all the tasks necessary to make a pdf file
	 * available as bitmap files to be stored in disk cache.
	 * 
	 * 1. The pdf is assumed to be available. 
	 *	Check if every page is already rendered and cached.
	 * 2. For those pages that are not already rendered and cached, it renders
	 *	them and store them in cache.
	 * 3. When all the pages are available in cache, it returns an array
	 *	containing the names of each page in cache. Each of these bitmaps
	 *	can then be retrieved from the cache using the names.
	 */
	private String[] renderAndCacheSelected(Context appContext, int maxWidth, boolean need[]) {
		String[] renderedFilenames = null;
		boolean hit[];
		boolean isComplete = true;
		
		if (pdfFilePath==null || pageNames==null || numberOfPages<=0) return null;
		if (maxWidth<=0 || need==null || need.length!=numberOfPages) return null;
		
		synchronized(fileLoaderLock) {
			
			Savelog.d(TAG, debug, "trying to load from file " + pdfFilePath.getName() + " with " + numberOfPages + " pages");

			// First, check the disk cache to see if all pages are available
			{
				// Check if the pages have already been rendered
				hit = areAllPagesFound(appContext, pageNames, need);
				
				if (hit!=null) {
					isComplete = true;
					for (int pageNumber=1; pageNumber<=numberOfPages; pageNumber++) {
						int index = pageNumber-1;
						if (!hit[index]) isComplete = false;
					}
				}
				else {
					// do nothing
				}
			}

			// Second, try rendering from pdf files in internal storage
			
			if (hit!=null && !isComplete) { // no point rendering if cache isn't working.
				
				try {
					Renderer pdfRenderer;
					pdfRenderer = new Renderer();

					Savelog.d(TAG, debug, "Trying to render from pdf file");

					pdfRenderer.open(pdfFilePath);

					for (int index=0; index<need.length; index++) {
						int pageNumber = index+1;
						if (need[index]) {
							Bitmap bitmap = pdfRenderer.get(maxWidth, pageNumber);
							if (bitmap!=null) {
								saveBitmapToFile(appContext, pageNames[index], bitmap);
							}
						}
					}
					
					// Since we force the use of hard-references, we must clear those links
					pdfRenderer.cleanup();
					
				} catch (Exception e) {
					Savelog.e(TAG, "cannot render file " + pdfFilePath.getName());
				}
			}
			

			// Finally, double check the disk cache to see if all pages are available
			renderedFilenames = new String[numberOfPages];
			{	
				isComplete = true;
				hit = areAllPagesFound(appContext, pageNames, need);

				if (hit!=null) {
					for (int pageNumber=1; pageNumber<=numberOfPages; pageNumber++) {
						int index = pageNumber-1;
						if (need[index]) {
							if (hit[index]) {
								renderedFilenames[index] = pageNames[index];
							}
							else {
								isComplete = false;
							}
						}
					}
				}
				if (!isComplete) {
					Savelog.e(TAG, "Unable to obtain all rendered pages for this pdf");
				}
				else {
					Savelog.d(TAG, debug, "All pages found in cache");
				}
			}
			fileLoaderLock.notifyAll();
		}
		return renderedFilenames;
	}
	
	
	public Bitmap load(Context appContext, int maxWidth, int pageNumberNeeded, int range) {
		String pngFilename;
		Bitmap bitmap;
		// Encapsulate the cache in the load, so that whoever uses the renderer also uses the cache. 
		// This is to prevent deadlock.
		
		if (pdfFilePath==null || pageNames==null || numberOfPages<=0 || maxWidth<=0) return null;
		if (pageNumberNeeded<1 || pageNumberNeeded>numberOfPages || range<0) return null;
		
		pngFilename = pageNames[pageNumberNeeded-1];
		
		synchronized(fileLoaderLock) {
			bitmap = loadBitmapFromFile(appContext, pngFilename);
			
			// Not successful in getting the bitmap. Try to render it. Then get it again.
			if (bitmap==null) {

				// Check the window is well defined. 
				if (pageNumberNeeded>=1 && pageNumberNeeded<=numberOfPages && range>=0) {

					Savelog.d(TAG, debug, "Cannot find " + pngFilename + " in cache. Try rendering now.");
					boolean need[] = new boolean[numberOfPages];
					for (int index=0; index<numberOfPages; index++) {
						int pageNumber=index+1;
						if (pageNumber>=pageNumberNeeded-range && pageNumber<=pageNumberNeeded+range) {
							need[index]=true;
						}
						else {
							need[index]=false;
						}
					}
					renderAndCacheSelected(appContext, maxWidth, need);
					
					bitmap = loadBitmapFromFile(appContext, pngFilename);
				}
				else {
					Savelog.w(TAG, "Render window ill-defined.");
				}
			}
			
			fileLoaderLock.notifyAll();
		}
		return bitmap;
	}
	


	private boolean[] areAllPagesFound(Context appContext, String renderedFilename[], boolean need[]) {
		int numberOfPages = renderedFilename.length;
		boolean hit[] = new boolean[numberOfPages];
		
		for (int index=0; index<numberOfPages; index++) {
			if (need[index]) {
				File pngfile = getInternalFileIfExists(appContext, renderedFilename[index]); 
				if (pngfile!=null) hit[index] = true;
				else hit[index] = false;
			}
		}
		return hit;
	}
	
	private static void saveBitmapToFile(Context context, String pngFilename, Bitmap bitmap) {
		try {
			FileOutputStream fout = new FileOutputStream(context.getFileStreamPath(pngFilename));
			BufferedOutputStream bout = new BufferedOutputStream(fout);
			bitmap.compress(ImageFormat, ImageQuality, bout);
			bout.close();
			fout.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}



	
	private static Bitmap loadBitmapFromFile(Context context, String pngFilename) {
		Bitmap bitmap = null;
		// provide a safety machanism to save from crash if bitmap cannot be loaded.
		try {
			File file = context.getFileStreamPath(pngFilename);
			if (file.exists())
				bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
		}
		catch (Throwable t) {
			Savelog.w(TAG, "Failed to load bitmap. " + t.getMessage());
		}
		return bitmap;
	}

	private String getRenderedName(String filename, int pageNumber) {
		return filename + "-p" + pageNumber + ImageExtension;
	}
	
	private File getInternalFileIfExists(Context context, String filename) {
		Savelog.d(TAG, debug, "Trying to get file " + filename);
		File f = context.getFileStreamPath(filename);
		if (f!=null && f.exists()) return f;
		else return null;
	}
	
	
	public static File getExternalDirectory() {
		File root = android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		return root;
	}
	
	// Get external file from Download directory
	private File getExternalFileIfExists(String filename) {
		File root = getExternalDirectory();
		File f = new File(root, filename);
		if (f!=null && f.exists()) return f;
		else return null;
	}

	public int getNumberOfPages() {
		if (pageNames==null) return 0;
		else return pageNames.length;
	}

	public String[] getPageNames() {
		return pageNames;
	}
}
