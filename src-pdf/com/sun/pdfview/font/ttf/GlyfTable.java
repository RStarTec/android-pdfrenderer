/*
 * $Id: GlyfTable.java,v 1.2 2007/12/20 18:33:30 rbair Exp $
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

package com.sun.pdfview.font.ttf;

import net.sf.andpdf.nio.ByteBuffer;
import pdf.main.SavelogPDF;

/**
 * Model the TrueType Glyf table
 */
public class GlyfTable extends TrueTypeTable {
	private static final String TAG = GlyfTable.class.getSimpleName()+"_class";
	private static final boolean debug = false;
    /** 
     * the glyph data, as either a byte buffer (unparsed) or a 
     * glyph object (parsed)
     */
    private Object[] glyphs;
    
    /**
     * The glyph location table
     */
    private LocaTable loca;
    
    /** Creates a new instance of HmtxTable */
    protected GlyfTable(TrueTypeFont ttf) {
        super (TrueTypeTable.GLYF_TABLE);
    
        loca = (LocaTable) ttf.getTable("loca");
        
        MaxpTable maxp = (MaxpTable) ttf.getTable("maxp");
        int numGlyphs = maxp.getNumGlyphs();
        
        SavelogPDF.d(TAG, debug, "Glyph table has " + numGlyphs + " entries.");
        
        glyphs = new Object[numGlyphs]; 
    }
  
    /**
     * Get the glyph at a given index, parsing it as needed
     */
    public Glyf getGlyph(int index) {
    	// Need to return an emtpy glyph if one does not exist!!
    	
    	SavelogPDF.d(TAG, debug, "getGlyph " + index);
    	
    	Object o;

		/* NOTE: Although TTFFont requires this function to return a valid glyph,
		 * when such a glyph is returned, the renderer creates a box around the
		 * empty character. This is ugly. So we decide to return null instead of
		 * glyph 0 whenever a glyph is not found.
		 */
    	if (index<0 || index>glyphs.length) {
    		SavelogPDF.e(TAG, "glyph index out of range" );
    		// o = glyphs[0];  
    		return null;
    	}
    	else {
    		// normal
    		o = glyphs[index];
    	}
    	
        if (o == null) {
        	// no data. Use the 0th glyph
        	SavelogPDF.w(TAG, "glyph " + index + " not found: \n");
        	// o = glyphs[0];
        	return null;
        }
        
        if (o instanceof ByteBuffer) {
        	// if there is no glyph for the index, glyph is the substitute
        	SavelogPDF.d(TAG, debug, "getGlyf from bytebuffer of size=" + ((ByteBuffer)o).limit() );
            Glyf g = Glyf.getGlyf((ByteBuffer) o);
            glyphs[index] = g;
            
            return g;
        } else {
        	SavelogPDF.d(TAG, debug, "glyf ready");
            return (Glyf) o;
        }
    }
  
    /** get the data in this map as a ByteBuffer */
    public ByteBuffer getData() {
        int size = getLength();
        
        ByteBuffer buf = ByteBuffer.allocate(size);
        
        // write the offsets
        for (int i = 0; i < glyphs.length; i++) {
            Object o = glyphs[i];
            if (o == null) {
            	continue;
            }

            ByteBuffer glyfData = null;
            if (o instanceof ByteBuffer) {
                glyfData = (ByteBuffer) o;
            } else {
                glyfData = ((Glyf) o).getData();
            }
            
            glyfData.rewind();
            buf.put(glyfData);
            glyfData.flip();
        }
        
        // reset the start pointer
        buf.flip();
        
        return buf;
    }
    
    /** Initialize this structure from a ByteBuffer */
    @Override
    public void setData(ByteBuffer data) {
    	SavelogPDF.d(TAG, debug, "setData() data size=" + data.limit());
        for (int i = 0; i < glyphs.length; i++) {
            int location = loca.getOffset(i);
            int length = loca.getSize(i);
            
            boolean bug = false;
            if (location<0 || location>data.limit()) {
            	SavelogPDF.e(TAG, "glyph "+ i + " location is off");
            	bug = true;
            }
            if (length<0 || length>data.limit()-location) {
            	SavelogPDF.e(TAG, "glyph "+ i + " length is off! " + length + " (offset=" + location + ")" );
            	bug = true;
            }
            if (bug) {
            	continue;
            }
            
            if (length == 0) {
                // undefined glyph
            	//Savelog.e(TAG, "glyph "+ i + " is undefined!!" );
                continue;
            }
            //Savelog.d(TAG, debug, "glyph "+ i + " location=" + location + " length=" + length);
            	
            data.position(location);
            ByteBuffer glyfData = data.slice();
            glyfData.limit(length);
            
            glyphs[i] = glyfData;
            

        }
    }
    
    /**
     * Get the length of this table
     */
    public int getLength() {
        int length = 0;
        
        for (int i = 0; i < glyphs.length; i++) {
            Object o = glyphs[i];
            if (o == null) {
                continue;
            }
            
            if (o instanceof ByteBuffer) {
                length += ((ByteBuffer) o).remaining();
            } else {
                length += ((Glyf) o).getLength();
            }
        }
        
        return length;
    }
    
    /**
     * Create a pretty String
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        String indent = "    ";
     
        buf.append(indent + "Glyf Table: (" + glyphs.length + " glyphs)\n");

        for (int i = 0; i < glyphs.length; i++) {
            Object o = glyphs[i];
            if (o == null) {
                continue;
            }
            int glyphSize;
            String description;
            if (o instanceof ByteBuffer) {
                glyphSize = ((ByteBuffer) o).remaining();
                description = "glyph "+ i + " length=" + glyphSize;
                // interpret this glyph
            } else {
                glyphSize = ((Glyf) o).getLength();
                description = "glyph "+ i + " length=" + glyphSize;
                // obtain the fields of this glyph
            }
            buf.append(indent + description + "\n");
        }

        return buf.toString();
    }
}
