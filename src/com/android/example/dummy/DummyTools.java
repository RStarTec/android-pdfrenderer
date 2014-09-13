package com.android.example.dummy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.android.example.pdfRenderer.Savelog;
import com.android.example.pdfRenderer.ViewerFragment;

public class DummyTools {
	private static final String TAG = DummyTools.class.getSimpleName()+"_class";
	private static final boolean debug = true;


	
	static Bitmap getDummyPage(Context context, int position) {
		Bitmap bitmap;
		int width = 50;
		int height = ViewerFragment.getScreenMaxDimension(context);
		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		bitmap = Bitmap.createBitmap(width, height, config);
		String colorName[] = {"red", "magenta", "yellow", "green", "cyan", "blue", "black", "darkGray"};
		int color[] = {Color.RED, Color.MAGENTA, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.BLACK, Color.DKGRAY};
		int index = position % color.length;
		
		Savelog.d(TAG, debug, "Creating dummy bitmap for position=" + position + " color=" + colorName[index]);
		Savelog.d(TAG, debug, "dummy bitmap size=" + bitmap.getWidth() + "x" + bitmap.getHeight());
		bitmap.eraseColor(color[index]);
		return bitmap;
	}
}
