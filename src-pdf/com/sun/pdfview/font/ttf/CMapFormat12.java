

/*
 * $Id: CMapFormat4.java,v 1.3 2009/02/12 13:53:57 tomoke Exp $
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

import java.util.HashMap;
import java.util.Map;

import net.sf.andpdf.nio.ByteBuffer;
import pdf.main.SavelogPDF;


/* Cmap format12
 * based on: http://www.microsoft.com/typography/otspec/cmap.htm
 * 
 * reference: org.apache.fontbox.ttf.CMAPEncodingEntry
 * http://unicode-table.com/en/sections/cjk-unified-ideographs/
 * 
 * @modified 06.24.2013 by A.Hui for use with DroidSansJapanese.ttf
 */

public class CMapFormat12 extends CMap {
	private static final String TAG = CMapFormat12.class.getSimpleName()+"_class";
	private static final boolean debug = false;

	int language;  // to override superclass' language
	int numGlyphs;
	
	private int[] glyphIdToCharacterCode;
	private Map<Integer, Integer> characterCodeToGlyphId = new HashMap<Integer, Integer>();

    /** Creates a new instance of CMapFormat0 */
    protected CMapFormat12(int language, int numGlyphs) {
        super((short) 12);
        this.language = language;
        this.numGlyphs = numGlyphs;
    }
    

    
    /**
     * Get the length of this table
     */
    @Override
    public short getLength() {
        // start with the size of the fixed header
        short size = Character.SIZE;
        return size;
    }

    /** 
     * Cannot map from a byte
     */
    @Override
    public byte map(byte src) {
        int c = map((char) src);
        if (c < Byte.MIN_VALUE || c > Byte.MAX_VALUE) {
            // out of range
            return 0;
        }
    
        return (byte) c;
    }

    /**
     * Map from char
     */
	@Override
    public int map(char src) {
        SavelogPDF.d(TAG, debug, "cmap(" + src + " ("+ Integer.toHexString(src) + "))");
        
        Integer glyphId = characterCodeToGlyphId.get(Integer.valueOf(((int)src)));
        
        if (glyphId!=null) {
        	SavelogPDF.d(TAG, debug, "char " +  Integer.toHexString((int) src) + " -> glyph " + glyphId  +"(base10)");
        	return glyphId.intValue();
        }
        // shouldn't get here!
    	SavelogPDF.w(TAG, "char " +  Integer.toHexString((int) src) + " -> glyph " + 0);
        return 0;
    }
    
	
	
    /**
     * Get the src code which maps to the given glyphID
     */
	@Override
    public char reverseMap(short glyphID) {
        SavelogPDF.d(TAG, debug, "reverseMap(" + glyphID + ")");
        
        if (glyphID > numGlyphs || glyphID > Integer.MAX_VALUE) {
			//	throw new IOException("CMap contains an invalid glyph index");
			SavelogPDF.e(TAG, "CMap contains an invalid glyph index");
		}
        else {
            return (char) glyphIdToCharacterCode[glyphID];
        }
        // not found!
        return (char) 0;
    }

	
	
    /**
     * Get the data in this map as a ByteBuffer
     */
	@Override
    public void setData(int length, ByteBuffer data) {
        // read the table size values
		int nGroups = data.getInt();
		glyphIdToCharacterCode = new int[numGlyphs];
		
		// Reading nGroups of segments
		for (int group=0; group<nGroups; group++) {
            int startCharCode = data.getInt();
            int endCharCode = data.getInt();
            int startGlyphID = data.getInt();
            
            
			if ( startCharCode < 0 || startCharCode > 0x0010FFFF 
					|| ( startCharCode >= 0x0000D800 && startCharCode <= 0x0000DFFF ) ) {
				//throw new IOException("Invalid Characters codes");
				SavelogPDF.e(TAG, "Invalid Characters codes");
			}

			if ( endCharCode > 0 && (endCharCode < startCharCode || endCharCode > 0x0010FFFF 
					|| ( endCharCode >= 0x0000D800 && endCharCode <= 0x0000DFFF ) ) ) {
				// throw new IOException("Invalid Characters codes");
				SavelogPDF.e(TAG, "Invalid Characters codes");
			}

			for (long j = 0; j <= (endCharCode - startCharCode); ++j) {

				if ( (startCharCode + j) > Integer.MAX_VALUE ) {
					// throw new IOException("Character Code greater than Integer.MAX_VALUE");                 
					SavelogPDF.e(TAG, "Character Code greater than Integer.MAX_VALUE");
				}

				long glyphIndex = (startGlyphID + j);
				
				if (glyphIndex > numGlyphs || glyphIndex > Integer.MAX_VALUE) {
				//	throw new IOException("CMap contains an invalid glyph index");
					SavelogPDF.e(TAG, "CMap contains an invalid glyph index");
				}
				//Savelog.d(TAG, debug, "char=" + Integer.toHexString((int) (startCharCode + j)) + " glyphIndex=" + glyphIndex);
				glyphIdToCharacterCode[(int)glyphIndex] = (int)(startCharCode + j);
				characterCodeToGlyphId.put((int)(startCharCode + j), (int)glyphIndex);
			}


		}
        
    }

	



    /**
     * Get the data in the map as a byte buffer
     */
	@Override
    public ByteBuffer getData() {
        ByteBuffer buf = ByteBuffer.allocate(getLength());

        // write the header
        buf.putShort(getFormat());
        buf.putShort((short) getLength());
        buf.putShort(getLanguage());

        // reset the data pointer
        buf.flip();

        return buf;
    }

	
	
	

}
