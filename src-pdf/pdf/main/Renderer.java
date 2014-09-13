package pdf.main;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import net.sf.andpdf.nio.ByteBuffer;
import net.sf.andpdf.refs.HardReference;
import android.graphics.Bitmap;
import android.graphics.RectF;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFImage;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.font.PDFFont;

public class Renderer {

	private static final String TAG = Renderer.class.getSimpleName()+"_class";
	private static final boolean debug = false;
		
	public static final boolean DEFAULTSHOWIMAGES = true;
	public static final boolean DEFAULTANTIALIAS = true;
	public static final boolean DEFAULTUSEFONTSUBSTITUTION = false;
	public static final boolean DEFAULTKEEPCACHES = true;
	
    
    private PDFPage mPdfPage; 
	private PDFFile mPdfFile;
	private int numberOfPages;

    public Renderer()  {
    	// Set up parameters
        PDFImage.sShowImages = DEFAULTSHOWIMAGES;
        PDFPaint.s_doAntiAlias = DEFAULTANTIALIAS;
        PDFFont.sUseFontSubstitution = DEFAULTUSEFONTSUBSTITUTION;
        HardReference.sKeepCaches = DEFAULTKEEPCACHES;
        // Cleanup any pre-existing references
        cleanup();
    }
    
    public int peekNumberOfPages(File filePath) {
    	int numberOfPages = 0;
    	PDFFile pdfFile;
		try {
			pdfFile = openFile(filePath);
		    if (pdfFile != null) {
		        numberOfPages = pdfFile.getNumPages();
		    }
		} catch (IOException e) {
			SavelogPDF.e(TAG, "Error peeking pdf for number of pages: " + e.getMessage());
		}
		cleanup();
	    return numberOfPages;
    }
    
    
    public boolean open(File filePath) {
        try {
        	mPdfFile = openFile(filePath);
	        numberOfPages = mPdfFile.getNumPages();
	        return true;
        }
    	catch (Exception e) {
    		SavelogPDF.e(TAG, "Cannot load file " + filePath + "\n" + SavelogPDF.getStack(e));
    		return false;
    	}
    }
    
    public Bitmap get(int maxWidth, int pageNumber) {
    	Bitmap bitmap = null;
    	
        if (mPdfFile!=null && numberOfPages>0 && pageNumber>=1 && pageNumber<=numberOfPages) {
            try {

	            SavelogPDF.d(TAG, debug, "Rendering page " + pageNumber);
	            if (mPdfPage == null || mPdfPage.getPageNumber() != pageNumber) {
	            	mPdfPage = mPdfFile.getPage(pageNumber, true);
	            }
	            float width = mPdfPage.getWidth();
	            float height = mPdfPage.getHeight();
	            RectF clip = null;
	            SavelogPDF.d(TAG, debug, "pdf default dimension=W"+ width + "*H" + height);
	            
	        	float zoom;
	            zoom = maxWidth / width;
	    	    bitmap = mPdfPage.getImage((int)(width*zoom), (int)(height*zoom), clip, true, true);
	    	    SavelogPDF.d(TAG, debug, "pdf rendered dimension=W"+ (int)(width*zoom) + "*H" + (int)(height*zoom));
    		} catch (Throwable e) {
    			SavelogPDF.e(TAG, "Cannot render page" + pageNumber + "\n" + SavelogPDF.getStack(e));
    		}
        }
        return bitmap;
    }

	
	private static PDFFile openFile(File filePath) throws IOException {
		PDFFile pdfFile = null;
		
		if (filePath!=null && filePath.exists() && filePath.length()>0) {
			long len = filePath.length();
			SavelogPDF.i(TAG, "file '" + filePath + "' has " + len + " bytes");
	    	
	
			// first open the file for random access
	        RandomAccessFile raf = new RandomAccessFile(filePath, "r");
	
	        SavelogPDF.d(TAG, debug, "trying to open file as random access");
	        
	        // extract a file channel
	        FileChannel channel = raf.getChannel();
	
		    // Note: Mapping the file locks the file until the PDFFile is closed.
	         ByteBuffer bb = ByteBuffer.NEW(channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()));
	        
	        /* alternative approach is to load file into memory
	        ByteBuffer bb = ByteBuffer.allocate((int)channel.size());
	        int bytesRead = channel.read(bb);
	        SaveLog.i(TAG, "read " + bytesRead + " from channel");
	        */
	        
	         // create a PDFFile from the data
	        pdfFile = new PDFFile(bb);
	        raf.close();
		}
		
		return pdfFile;
	}
	
	
	public void cleanup() {
		if (HardReference.sKeepCaches) {
			HardReference.cleanup();
		}
	}
}