/*
 * $Id: CMap.java,v 1.4 2009/03/15 20:47:38 tomoke Exp $
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
 *
 * @author  jkaplan
 */
public abstract class CMap {
	private static final String TAG = CMap.class.getSimpleName()+"_class";
	private static final boolean debug = false;

    /**
     * The format of this map
     */
    private short format;

    /**
     * The language of this map, or 0 for language-independent
     */
    private short language=0;

    /** Creates a new instance of CMap 
     * Don't use this directly, use <code>CMap.createMap()</code>
     */
    protected CMap (short format, short language) {
        this.format = format;
        this.language = language;
    }
    
    protected CMap (short format) {
        this.format = format;
    }

    /**
     * Create a map for the given format and language

     * <p>The Macintosh standard character to glyph mapping is supported
     * by format 0.</p>
     *
     * <p>Format 2 supports a mixed 8/16 bit mapping useful for Japanese,
     * Chinese and Korean. </p>
     *
     * <p>Format 4 is used for 16 bit mappings.</p>
     *
     * <p>Format 6 is used for dense 16 bit mappings.</p>
     *
     * <p>Formats 8, 10, and 12 (properly 8.0, 10.0, and 12.0) are used
     * for mixed 16/32-bit and pure 32-bit mappings.<br>
     * This supports text encoded with surrogates in Unicode 2.0 and later.</p>
     *
     * <p>Reference:<br>
     * http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6cmap.html </p>
     */
    public static CMap createMap (short format, short language) {
        CMap outMap = null;
        SavelogPDF.d(TAG, debug, "createMap with format=" + format + " language=" + language);

        switch (format) {
            case 0: // CMap format 0 - single byte codes
                outMap = new CMapFormat0 (language);
                break;
            case 4: // CMap format 4 - two byte encoding
                outMap = new CMapFormat4 (language);
                break;
            case 6: // CMap format 6 - 16-bit, two byte encoding
                outMap = new CMapFormat6 (language);
                break;
            default:
                SavelogPDF.e(TAG, "Unsupport CMap format: " + format);
                return null;
        }

        return outMap;
    }

    /**
     * Get a map from the given data
     *
     * This method reads the format, data and length variables of
     * the map.
     */
    public static CMap getMap (ByteBuffer data, int numGlyphs) {
    	// First ushort is format
        short format = data.getShort ();
        
        if (format<8) {
        	short lengthShort = data.getShort ();
            int length = 0xFFFF & (int) lengthShort;
            SavelogPDF.d(TAG, debug, "getMap() format: " + format + ", length: " + lengthShort);

            // make sure our slice of the data only contains up to the length
            // of this table
            data.limit (Math.min (length, data.limit ()));

            short language = data.getShort ();

            CMap outMap = createMap (format, language);
            if (outMap == null) {
                return null;
            }

            outMap.setData (data.limit (), data);

            return outMap;
        }
        else {
        	// for format 8 onward:
        	

            if (format==12) {
            	// ushort format (already read)
            	// ushort reservedShort
            	// ulong length
            	// ulong language
                short reservedShort = data.getShort ();
                int length = data.getInt();
                int language = data.getInt ();
            	
                SavelogPDF.d(TAG, debug, "getMap() format: " + format + ", length: " + length + " reserve: " + reservedShort);
                // make sure our slice of the data only contains up to the length
                // of this table
                data.limit (Math.min (length, data.limit ()));
                
                CMap outMap = new CMapFormat12 (language, numGlyphs);

                outMap.setData(data.limit(), data);
                
                return outMap;
            }
            
            return null;
        }
    }

    /**
     * Get the format of this map
     */
    public short getFormat () {
        return format;
    }

    /**
     * Get the language of this map
     */
    public short getLanguage () {
        return language;
    }

    /**
     * Set the data for this map
     */
    public abstract void setData (int length, ByteBuffer data);

    /**
     * Get the data in this map as a byte buffer
     */
    public abstract ByteBuffer getData ();

    /**
     * Get the length of this map
     */
    public abstract short getLength ();

    /**
     * Map an 8 bit value to another 8 bit value
     */
    public abstract byte map (byte src);

    /**
     * Map a 16 bit value to another 16 bit value
     */
    public abstract int map (char src);

    /**
     * Get the src code which maps to the given glyphID
     */
    public abstract char reverseMap (short glyphID);

    /** Print a pretty string */
    @Override
    public String toString () {
        String indent = "        ";

        return indent + " format: " + getFormat () + " length: " +
                getLength () + " language: " + getLanguage () + "\n";
    }
}
