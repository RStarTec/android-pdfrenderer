/*
 * $Id: TTFFont.java,v 1.10 2009/02/23 15:29:19 tomoke Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.sun.pdfview.font;

import java.io.IOException;
import java.io.InputStream;

import net.sf.andpdf.utils.Utils;
import pdf.main.SavelogPDF;
import android.graphics.Matrix;
import android.graphics.Path;

import com.sun.pdfview.PDFObject;
import com.sun.pdfview.font.ttf.AdobeGlyphList;
import com.sun.pdfview.font.ttf.CMap;
import com.sun.pdfview.font.ttf.CmapTable;
import com.sun.pdfview.font.ttf.Glyf;
import com.sun.pdfview.font.ttf.GlyfCompound;
import com.sun.pdfview.font.ttf.GlyfSimple;
import com.sun.pdfview.font.ttf.GlyfTable;
import com.sun.pdfview.font.ttf.HeadTable;
import com.sun.pdfview.font.ttf.HmtxTable;
import com.sun.pdfview.font.ttf.PostTable;
import com.sun.pdfview.font.ttf.TrueTypeFont;


/**
 * A true-type font
 * @modified 06.24.2013 by A.Hui
 */
public class TTFFont extends OutlineFont {
	private static final String TAG = TTFFont.class.getSimpleName()+"_class";
	private static final boolean debug = false;

    /** the truetype font itself */
    private TrueTypeFont font = null;
    /** the number of units per em in the font */
    private float unitsPerEm;

    private String fontName = "";

    /**
     * create a new TrueTypeFont object based on a description of the
     * font from the PDF file.  If the description happens to contain
     * an in-line true-type font file (under key "FontFile2"), use the
     * true type font.  Otherwise, parse the description for key information
     * and use that to generate an appropriate font.
     */
    public TTFFont(String baseFont, PDFObject fontObj,
            PDFFontDescriptor descriptor)
            throws IOException {
        super(baseFont, fontObj, descriptor);

        fontName = descriptor.getFontName();
        PDFObject ttfObj = descriptor.getFontFile2();

        // Dump the embedded stream if it exists.
        /*
        if (debug) {
            try {
                byte[] fontData = ttfObj.getStream();
                File tempOutput = new File(IO.getDefaultExternalPath().getAbsolutePath(), fontName + ".ttf");
                Savelog.d(TAG, debug, "Saving embedded font data to " + tempOutput.getAbsolutePath());
                FileOutputStream fis = new FileOutputStream(tempOutput);
                fis.write(fontData);
                fis.flush();
                fis.close();
            } catch (Exception ex) {
                Savelog.e(TAG, "Cannot output ttf " + ex.getMessage());
            }
        	
        }
        */
        
        if (ttfObj != null) {
        	SavelogPDF.d(TAG, debug, "TTFFont constructor(): getFontFile2()!=null, fontName=" + fontName);
        	font = TrueTypeFont.parseFont(ttfObj.getStreamBuffer());
        }
        
        // If things don't work out, use a default font.
        if (font==null){
        	// TODO: Here, find out how to determine which default font to use
        	font=getDefaultJapan1Font();
        }
        
        if (font!=null) {
        	// read the units per em from the head table
   	        HeadTable head = (HeadTable) font.getTable("head");
   	        unitsPerEm = head.getUnitsPerEm();
        }
    }
    
    
    /* In case the specific true font is not embedded 
     * in the pdf file, resort to a default true-type 
     * font package. Since android has limited memory, 
     * pick a package that is small enough and generic 
     * enough to cover many languages. 
     * The DroidSansJapanese.ttf is the older version 
     * of DroidSansFallback and is chosen for this reason. 
     */
    
    private TrueTypeFont getDefaultJapan1Font() {
    	TrueTypeFont defaultFont = null;
    	// Android limits block size to <100K per file
    	int segments = 18; // use 18 blocks for Japanese font
    	
    	String resourceName = "/com/sun/pdfview/font/ttf/resource/DroidSansJapanese.ttf";
    	InputStream in;
    	
    	try {
        	int size[] = new int[segments];
        	int total = 0;

        	for (int seg=0; seg<segments; seg++) {
        		String resourceSegmentName = resourceName+ seg;
            	in = getClass().getResourceAsStream(resourceSegmentName);
            	size[seg] = in.available();
            	in.close();
            	total += size[seg];
        	}

        	byte[] fontdata = new byte[total];

        	total = 0;
        	for (int seg=0; seg<segments; seg++) {
        		String resourceSegmentName = resourceName+ seg;
            	in = getClass().getResourceAsStream(resourceSegmentName);
            	in.read(fontdata, total, in.available());
            	in.close();
            	total += size[seg];
        	}


        	SavelogPDF.w(TAG, "TTFFont constructor(): resort to Backup"+ " size=" + total);
        	
        	if (debug) {
                // For testing the DroidSansJapanese.ttf file
                // Display a sample of the glyph data
                if (fontdata.length==1173140) {
                	int block = (int) Math.pow(2, 16);
                	for (int offset=0; offset<fontdata.length; offset+=block) {
                    	int startByte = offset;
                    	int endByte = offset + 32;
                    	listTTFData(fontdata, startByte, endByte);
                	}
                }
        	}

        	defaultFont = TrueTypeFont.parseFont(fontdata);
    	}
    	catch (Exception e) {
    		
    	}
    	return defaultFont;

    }

    
    

    /**
     * Get the outline of a character given the character code
     */
    protected synchronized Path getOutline(char src, float width) {
    	SavelogPDF.d(TAG, debug, "getOutline(char) on " + src );
        // find the cmaps
        CmapTable cmap = (CmapTable) font.getTable("cmap");

        // if there are no cmaps, this is (hopefully) a cid-mapped font,
        // so just trust the value we were given for src
        if (cmap == null) {
            return getOutline((int) src, width);
        }

        CMap[] maps = cmap.getCMaps();

        SavelogPDF.d(TAG, debug, "Obtained " + maps.length + " cmaps");
        
        // try the maps in order
        for (int i = 0; i < maps.length; i++) {
            int idx = maps[i].map(src);
            if (idx != 0) {
                return getOutline(idx, width);
            }
        }

        // not found, return the empty glyph
        return getOutline(0, width);
    }

    /**
     * lookup the outline using the CMAPs, as specified in 32000-1:2008,
     * 9.6.6.4, when an Encoding is specified.
     * 
     * @param val
     * @param width
     * @return GeneralPath
     */
    protected synchronized Path getOutlineFromCMaps(char val, float width) {
    	SavelogPDF.d(TAG, debug, "getOutlineFromCMaps(char) on " + val );

        // find the cmaps
        CmapTable cmap = (CmapTable) font.getTable("cmap");

        if (cmap == null) {
            return null;
        }

        // try maps in required order of (3, 1), (1, 0)
        CMap map = cmap.getCMap((short) 3, (short) 1);
        if (map == null) {
            map = cmap.getCMap((short) 1, (short) 0);
        }
        int idx = map.map(val);
        if (idx != 0) {
            return getOutline(idx, width);
        }

        return null;
    }

    /**
     * Get the outline of a character given the character name
     */
    protected synchronized Path getOutline(String name, float width) {
    	SavelogPDF.d(TAG, debug, "getOutline(String) on " + name );

        int idx;
        PostTable post = (PostTable) font.getTable("post");
        if (post != null) {
        	idx = post.getGlyphNameIndex(name);
        	if (idx != 0) {
        		return getOutline(idx, width);
        	}
        	return null;
        }
        
        Integer res = AdobeGlyphList.getGlyphNameIndex(name);
        if(res != null) {
        	idx = res;
        	return getOutlineFromCMaps((char)idx, width);
        }        		        
        return null;
    }

    /**
     * Get the outline of a character given the glyph id
     */
    protected synchronized Path getOutline(int glyphId, float width) {
    	SavelogPDF.d(TAG, debug, "getOutline(int) on " + glyphId +"(base10)" );
    	
        // find the glyph itself
        GlyfTable glyf = (GlyfTable) font.getTable("glyf");
        Glyf g = glyf.getGlyph(glyphId);

        Path gp = null;
        if (g instanceof GlyfSimple) {
        	SavelogPDF.d(TAG, debug, "going to render simpleGlyph");
            gp = renderSimpleGlyph((GlyfSimple) g);
        } else if (g instanceof GlyfCompound) {
        	SavelogPDF.d(TAG, debug, "going to render compoundGlyph");
            gp = renderCompoundGlyph(glyf, (GlyfCompound) g);
        } else {
        	SavelogPDF.d(TAG, debug, "going to render new empty path");
            gp = new Path();
        }

        // calculate the advance
        HmtxTable hmtx = (HmtxTable) font.getTable("hmtx");
        float advance = (float) hmtx.getAdvance(glyphId) / (float) unitsPerEm;

        // scale the glyph to match the desired advance
        float widthfactor = width / advance;

        // the base transform scales the glyph to 1x1
        Matrix at = new Matrix();
        at.setScale(1 / unitsPerEm, 1 / unitsPerEm);
        Matrix tmp = new Matrix();
        tmp.setScale(widthfactor, 1);
        at.preConcat(tmp);

        gp.transform(at);

        return gp;
    }

    /**
     * Render a simple glyf
     */
    protected Path renderSimpleGlyph(GlyfSimple g) {
        // the current contour
        int curContour = 0;

        // the render state
        RenderState rs = new RenderState();
        rs.gp = new Path();

        for (int i = 0; i < g.getNumPoints(); i++) {
            PointRec rec = new PointRec(g, i);

            if (rec.onCurve) {
                addOnCurvePoint(rec, rs);
            } else {
                addOffCurvePoint(rec, rs);
            }

            // see if we just ended a contour
            if (i == g.getContourEndPoint(curContour)) {
                curContour++;

                if (rs.firstOff != null) {
                    addOffCurvePoint(rs.firstOff, rs);
                }

                if (rs.firstOn != null) {
                    addOnCurvePoint(rs.firstOn, rs);
                }

                rs.firstOn = null;
                rs.firstOff = null;
                rs.prevOff = null;
            }
        }

        return rs.gp;
    }
    

    
    /**
     * Render a compound glyf
     */
    protected Path renderCompoundGlyph(GlyfTable glyf, GlyfCompound g) {
        Path gp = new Path();

        for (int i = 0; i < g.getNumComponents(); i++) {
            // find and render the component glyf
            Glyf gl = glyf.getGlyph (g.getGlyphIndex (i));
            Path path = null;
            if (gl instanceof GlyfSimple) {
                path = renderSimpleGlyph ((GlyfSimple) gl);
            } else if (gl instanceof GlyfCompound) {
                path = renderCompoundGlyph (glyf, (GlyfCompound) gl);
            } else {
                throw new RuntimeException ("Unsupported glyph type " + gl.getClass ().getCanonicalName ());
            }

            // multiply the translations by units per em
            float[] matrix = g.getTransform(i);

            // transform and add path to the global path
            Matrix mat = new Matrix();
            Utils.setMatValues(mat, matrix);
            gp.addPath(path, mat);
        }

        return gp;
    }

    /** add a point on the curve */
    private void addOnCurvePoint(PointRec rec, RenderState rs) {
        // if the point is on the curve, either move to it,
        // or draw a line from the previous point
        if (rs.firstOn == null) {
            rs.firstOn = rec;
            rs.gp.moveTo(rec.x, rec.y);
        } else if (rs.prevOff != null) {
            rs.gp.quadTo(rs.prevOff.x, rs.prevOff.y, rec.x, rec.y);
            rs.prevOff = null;
        } else {
            rs.gp.lineTo(rec.x, rec.y);
        }
    }

    /** add a point off the curve */
    private void addOffCurvePoint(PointRec rec, RenderState rs) {
        if (rs.prevOff != null) {
            PointRec oc = new PointRec((rec.x + rs.prevOff.x) / 2,
                    (rec.y + rs.prevOff.y) / 2,
                    true);
            addOnCurvePoint(oc, rs);
        } else if (rs.firstOn == null) {
            rs.firstOff = rec;
        }
        rs.prevOff = rec;
    }

    class RenderState {
        // the shape itself
        Path gp;
        // the first off and on-curve points in the current segment
        PointRec firstOn;
        PointRec firstOff;
        // the previous off and on-curve points in the current segment
        PointRec prevOff;
    }

    /** a point on the stack of points */
    class PointRec {

        int x;
        int y;
        boolean onCurve;

        public PointRec(int x, int y, boolean onCurve) {
            this.x = x;
            this.y = y;
            this.onCurve = onCurve;
        }

        public PointRec(GlyfSimple g, int idx) {
            x = g.getXCoord(idx);
            y = g.getYCoord(idx);
            onCurve = g.onCurve(idx);
        }
    }
    

    
    
	private void listTTFData(byte[] rawdata, int startByte, int endByte) {
		SavelogPDF.i(TAG, "Listing data in range (" + startByte + "(base10)," + endByte + "(base10))");
		SavelogPDF.i(TAG, "                range (" + Integer.toHexString(startByte) + "(base16)," + Integer.toHexString(endByte) + "(base16))");
		if (rawdata==null) return;

		if (startByte<0 || rawdata.length<=endByte) return;

		int offset = startByte % 16;
		if (offset!=0) {
			offset = 16 - offset;
		}

		String firstLine = "";

		for (int pos=0; pos<offset; pos++) {
			firstLine += " 00";
		}

		SavelogPDF.i(TAG, "firstline: " + firstLine);
		    
		int pos = startByte; 
		String line = "";
		while (pos<endByte) {

			// Start a new line
			if (pos==startByte || (pos%16==0)) {
				int startAddress = (pos / 16) * 16;
				int lineNum = ((pos-startByte)/16) * 16;
				line = "" + Integer.toHexString(startAddress) + "(" + Integer.toHexString(lineNum) + "): ";
			}

			// If at the beginning, add offset
			if (pos==startByte) {
				line += firstLine; 
			}

			String byteValue = Integer.toHexString(rawdata[pos]); 
			if (byteValue.length()==1) byteValue = "0" + byteValue;
			if (byteValue.length()>2) byteValue = byteValue.substring(byteValue.length()-2);

			line += " " + byteValue;
			if (pos%16==15 || pos==endByte-1) {
				SavelogPDF.i(TAG, line);
			}

			pos++;
		}
	}

}
