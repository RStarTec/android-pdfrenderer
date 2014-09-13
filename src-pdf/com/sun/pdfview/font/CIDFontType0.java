package com.sun.pdfview.font;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import pdf.main.SavelogPDF;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.font.cid.ToUnicodeMap;

/*****************************************************************************
 * At the moment this is not fully supported to parse CID based fonts
 * As a hack we try to use a built in font as substitution and use a
 * toUnicode map to translate the characters if available.
 * 
 *
 * @version $Id: CIDFontType0.java,v 1.1 2011-08-03 15:48:56 bros Exp $ 
 * @author  Bernd Rosstauscher
 * @since 03.08.2011
 * @modified 06.24.2013 by A.Hui
 ****************************************************************************/

public class CIDFontType0 extends TTFFont {
	private static final String TAG = CIDFontType0.class.getSimpleName()+"_class";
	private static final boolean debug = false;

    /**
     * The width of each glyph from the DW and W arrays
     */
    private Map<Character, Float> widths = null;
    /**
     * The vertical width of each glyph from the DW2 and W2 arrays
     */
    private Map<Character, Float> widthsVertical = null;

    /*
     * the default width
     */
    private int defaultWidth = 1000;
    /*
     * the default vertical width
     */
    private int defaultWidthVertical = 1000;

	
    private ToUnicodeMap GidMap = null;
    private String registry = "";
    private String ordering = "";
    private int supplement = 0;
    
	/*************************************************************************
	 * Constructor
	 * @param baseFont
	 * @param fontObj
	 * @param descriptor
	 * @throws IOException
	 ************************************************************************/
	
	public CIDFontType0(String baseFont, PDFObject fontObj,
			PDFFontDescriptor descriptor) throws IOException {
		super(baseFont, fontObj, descriptor);
		
        parseWidths(fontObj);


        try {
            // read the CIDSystemInfo dictionary (required)
            PDFObject systemInfoObj = fontObj.getDictRef("CIDSystemInfo");
           	SavelogPDF.d(TAG, debug, "Checking dictionary in CIDFontType0 object:");
           	SavelogPDF.d(TAG, debug, "CIDSystemInfo" + systemInfoObj!=null? systemInfoObj.toString() : "(null)");
            
            PDFObject registryObj = systemInfoObj.getDictRef("Registry");
            if (registryObj!=null) registry = registryObj.getStringValue();
           	SavelogPDF.d(TAG, debug, "registry=" + registry);
            
            PDFObject orderingObj = systemInfoObj.getDictRef("Ordering");
            if (orderingObj!=null) ordering = orderingObj.getStringValue();
            
           	SavelogPDF.d(TAG, debug, "ordering=" + ordering);
            
            PDFObject supplementObj = systemInfoObj.getDictRef("Supplement");
            if (supplementObj!=null) supplement = supplementObj.getIntValue();
            
           	SavelogPDF.d(TAG, debug, "supplement=" + supplement);
        }
        catch (Exception e) {
        	SavelogPDF.e(TAG, "cannot understand fontObj!");
        }
        catch (Throwable t) {
        	SavelogPDF.e(TAG, "cannot understand fontObj!");
        }
        
	}
	
	
	
	
    /** Parse the Widths array and DW object */
    private void parseWidths(PDFObject fontObj)
            throws IOException {
        // read the default width (otpional)
        PDFObject defaultWidthObj = fontObj.getDictRef("DW");
        if (defaultWidthObj != null) {
            defaultWidth = defaultWidthObj.getIntValue();
        }

        int entryIdx = 0;
        int first = 0;
        int last = 0;
        PDFObject[] widthArray;

        // read the widths table 
        PDFObject widthObj = fontObj.getDictRef("W");
        if (widthObj != null) {

            // initialize the widths array
            widths = new HashMap<Character, Float>();

            // parse the width array
            widthArray = widthObj.getArray();

            /* an entry can be in one of two forms:
             *   <startIndex> <endIndex> <value> or
             *   <startIndex> [ array of values ]
             * we use the entryIdx to differentitate between them
             */
            for (int i = 0; i < widthArray.length; i++) {
                if (entryIdx == 0) {
                    // first value in an entry.  Just store it
                    first = widthArray[i].getIntValue();
                } else if (entryIdx == 1) {
                    // second value -- is it an int or array?
                    if (widthArray[i].getType() == PDFObject.ARRAY) {
                        // add all the entries in the array to the width array
                        PDFObject[] entries = widthArray[i].getArray();
                        for (int c = 0; c < entries.length; c++) {
                            Character key = Character.valueOf((char) (c + first));

                            // value is width / default width
                            float value = entries[c].getIntValue();
                            widths.put(key, Float.valueOf(value));
                        }
                        // all done
                        entryIdx = -1;
                    } else {
                        last = widthArray[i].getIntValue();
                    }
                } else {
                    // third value.  Set a range
                    int value = widthArray[i].getIntValue();

                    // set the range
                    for (int c = first; c <= last; c++) {
                        widths.put(Character.valueOf((char) c), Float.valueOf(value));
                    }

                    // all done
                    entryIdx = -1;
                }

                entryIdx++;
            }
        }

        // read the optional vertical default width
        defaultWidthObj = fontObj.getDictRef("DW2");
        if (defaultWidthObj != null) {
            defaultWidthVertical = defaultWidthObj.getIntValue();
        }

        // read the vertical widths table
        widthObj = fontObj.getDictRef("W2");
        if (widthObj != null) {

            // initialize the widths array
            widthsVertical = new HashMap<Character, Float>();

            // parse the width2 array
            widthArray = widthObj.getArray();

            /* an entry can be in one of two forms:
             *   <startIndex> <endIndex> <value> or
             *   <startIndex> [ array of values ]
             * we use the entryIdx to differentitate between them
             */
            entryIdx = 0;
            first = 0;
            last = 0;

            for (int i = 0; i < widthArray.length; i++) {
                if (entryIdx == 0) {
                    // first value in an entry.  Just store it
                    first = widthArray[i].getIntValue();
                } else if (entryIdx == 1) {
                    // second value -- is it an int or array?
                    if (widthArray[i].getType() == PDFObject.ARRAY) {
                        // add all the entries in the array to the width array
                        PDFObject[] entries = widthArray[i].getArray();
                        for (int c = 0; c < entries.length; c++) {
                            Character key = Character.valueOf((char) (c + first));

                            // value is width / default width
                            float value = entries[c].getIntValue();
                            widthsVertical.put(key, Float.valueOf(value));
                        }
                        // all done
                        entryIdx = -1;
                    } else {
                        last = widthArray[i].getIntValue();
                    }
                } else {
                    // third value.  Set a range
                    int value = widthArray[i].getIntValue();

                    // set the range
                    for (int c = first; c <= last; c++) {
                        widthsVertical.put(Character.valueOf((char) c), Float.valueOf(value));
                    }

                    // all done
                    entryIdx = -1;
                }

                entryIdx++;
            }
        }
    }

    /** Get the default width in text space */
    @Override
    public int getDefaultWidth() {
        return defaultWidth;
    }

    /** Get the width of a given character */
    @Override
    public float getWidth(char code, String name) {
        if (widths == null) {
            return 1f;
        }
        Float w = widths.get(Character.valueOf(code));
        if (w == null) {
            return 1f;
        }

        return w.floatValue() / getDefaultWidth();
    }

    /** Get the default vertical width in text space */
    public int getDefaultWidthVertical() {
        return defaultWidthVertical;
    }

    /** Get the vertical width of a given character */
    public float getWidthVertical(char code, String name) {
        if (widthsVertical == null) {
            return 1f;
        }
        Float w = widthsVertical.get(Character.valueOf(code));
        if (w == null) {
            return 1f;
        }

        return w.floatValue() / getDefaultWidth();
    }


    
    // Override getGlyph in OutlineFont
    @Override 
    protected PDFGlyph getGlyph(char cid, String name) {
    	SavelogPDF.d(TAG, debug, "enter getGlyph(cid="+cid+","+name+")" );
    	char unicode = 0;
    	
       	try {
       		// The map may not be available for this specific cid
       		if (GidMap==null  || !GidMap.mapAvailable(registry, ordering, supplement, cid)) {
    			GidMap = new ToUnicodeMap(registry, ordering, supplement, cid);
       		}
       		
	        // check if there is a cidToGidMap
	        if (GidMap != null) {
	            // read the map
	            unicode = GidMap.getChar(cid);
	        }
		} catch (IOException e) {
			SavelogPDF.e(TAG, "Cannot get ToUnicodeMap" + SavelogPDF.getStack(e));
		}
       	
    	SavelogPDF.d(TAG, debug, "leave getGlyph(unicode="+unicode+","+name+")" );

        return super.getGlyph(unicode, name);
    }

}