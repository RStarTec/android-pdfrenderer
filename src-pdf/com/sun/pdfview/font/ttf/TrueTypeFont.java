/*
 * $Id: TrueTypeFont.java,v 1.6 2009/03/15 20:47:39 tomoke Exp $
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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.andpdf.nio.ByteBuffer;
import pdf.main.SavelogPDF;

/**
 *
 * @author  jkaplan
 * @author Ferenc Hechler (ferenc@hechler.de)
 * @author Joerg Jahnke (joergjahnke@users.sourceforge.net)
 */
public class TrueTypeFont {
	private static final String TAG = TrueTypeFont.class.getSimpleName()+"_class";
	private static final boolean debug = false;

    private int type;
    // could be a ByteBuffer or a TrueTypeTable
    private SortedMap<String, Object> tables;

    /** Creates a new instance of TrueTypeParser */
    public TrueTypeFont(int type) {
        this.type = type;

        tables = Collections.synchronizedSortedMap(
                new TreeMap<String, Object>());
    }

    /**
     * Parses a TrueType font from a byte array
     */
    public static TrueTypeFont parseFont(byte[] orig) {
        ByteBuffer inBuf = ByteBuffer.wrap(orig);
        // Savelog.d(TAG, debug, "wrapping byte[] into ByteBuffer of size=" + inBuf.limit());
        
        return parseFont(inBuf);
    }

    /**
     * Parses a TrueType font from a byte buffer
     */
    public static TrueTypeFont parseFont(ByteBuffer inBuf) {
        int type = inBuf.getInt();
        short numTables = inBuf.getShort();
        short searchRange = inBuf.getShort();
        short entrySelector = inBuf.getShort();
        short rangeShift = inBuf.getShort();
        
        SavelogPDF.d(TAG, debug, "parseFont() type=" + type + " numTables=" + numTables + " searchRange=" + searchRange
        		+ " entrySelector=" + entrySelector + " rangeShift=" + rangeShift);

        TrueTypeFont font = new TrueTypeFont(type);
        parseDirectories(inBuf, numTables, font);

        return font;
    }

    /**
     * Get the type of this font
     */
    public int getType() {
        return type;
    }

    /**
     * Add a table to the font
     *
     * @param tagString the name of this table, as a 4 character string
     *        (i.e. cmap or head)
     * @param data the data for this table, as a byte buffer
     */
    public void addTable(String tagString, ByteBuffer data) {
        tables.put(tagString, data);
    }

    /**
     * Add a table to the font
     *
     * @param tagString the name of this table, as a 4 character string
     *        (i.e. cmap or head)
     * @param table the table
     */
    public void addTable(String tagString, TrueTypeTable table) {
        tables.put(tagString, table);
    }

    /**
     * Get a table by name.  This command causes the table in question
     * to be parsed, if it has not already been parsed.
     *
     * @param tagString the name of this table, as a 4 character string
     *        (i.e. cmap or head)
     */
    public TrueTypeTable getTable(String tagString) {
    	// Savelog.d(TAG, debug, "Getting table " + tagString);
    	
        Object tableObj = tables.get(tagString);

        TrueTypeTable table = null;

        if (tableObj instanceof ByteBuffer) {
        	//Savelog.d(TAG, debug, "parsing table " + tagString);
            
        	// the table has not yet been parsed.  Parse it, and add the
            // parsed version to the map of tables.
            ByteBuffer data = (ByteBuffer) tableObj;

            table = TrueTypeTable.createTable(this, tagString, data);
            addTable(tagString, table);
        } else {
            table = (TrueTypeTable) tableObj;
        }

        return table;
    }

    /**
     * Remove a table by name
     *
     * @param tagString the name of this table, as a 4 character string
     *        (i.e. cmap or head)
     */
    public void removeTable(String tagString) {
        tables.remove(tagString);
    }

    /**
     * Get the number of tables
     */
    public short getNumTables() {
        return (short) tables.size();
    }

    /**
     * Get the search range
     */
    public short getSearchRange() {
        double pow2 = Math.floor(Math.log(getNumTables()) / Math.log(2));
        double maxPower = Math.pow(2, pow2);

        return (short) (16 * maxPower);
    }

    /**
     * Get the entry selector
     */
    public short getEntrySelector() {
        double pow2 = Math.floor(Math.log(getNumTables()) / Math.log(2));
        double maxPower = Math.pow(2, pow2);

        return (short) (Math.log(maxPower) / Math.log(2));
    }

    /**
     * Get the range shift
     */
    public short getRangeShift() {
        double pow2 = Math.floor(Math.log(getNumTables()) / Math.log(2));
        double maxPower = Math.pow(2, pow2);

        return (short) ((maxPower * 16) - getSearchRange());
    }

    /**
     * Write a font given the type and an array of Table Directory Entries
     */
    public byte[] writeFont() {
        // allocate a buffer to hold the font
        ByteBuffer buf = ByteBuffer.allocate(getLength());

        // write the font header
        buf.putInt(getType());
        buf.putShort(getNumTables());
        buf.putShort(getSearchRange());
        buf.putShort(getEntrySelector());
        buf.putShort(getRangeShift());

        // first offset is the end of the table directory entries
        int curOffset = 12 + (getNumTables() * 16);

        // write the tables
        for (Iterator i = tables.keySet().iterator(); i.hasNext();) {
            String tagString = (String) i.next();
            int tag = TrueTypeTable.stringToTag(tagString);

            ByteBuffer data = null;

            Object tableObj = tables.get(tagString);
            if (tableObj instanceof TrueTypeTable) {
                data = ((TrueTypeTable) tableObj).getData();
            } else {
                data = (ByteBuffer) tableObj;
            }

            int dataLen = data.remaining();

            // write the table directory entry
            buf.putInt(tag);
            buf.putInt((int)(calculateChecksum(tagString, data)*0xffffffff));
            buf.putInt(curOffset);
            buf.putInt(dataLen);

            // save the current position
            buf.mark();

            // move to the current offset and write the data
            buf.position(curOffset);
            buf.put(data);

            // reset the data start pointer
            data.flip();

            // return to the table directory entry
            buf.reset();

            // udate the offset
            curOffset += dataLen;

            // don't forget the padding
            while ((curOffset % 4) > 0) {
                curOffset++;
            }
        }

        buf.position(curOffset);
        buf.flip();

        // adjust the checksum
        updateChecksumAdj(buf);

        return buf.array();
    }

    /**
     * Calculate the checksum for a given table
     * 
     * @param tagString the name of the data
     * @param data the data in the table
     */
    private static int calculateChecksum(final String tagString, final ByteBuffer data) {
        int sum = 0;
        
        
        data.mark();

        // special adjustment for head table
        if (tagString.equals("head")) {
            data.putInt(8, 0);
        }

        for (int i = 0, nlongs = (data.remaining() + 3) / 4; i < nlongs; ++i) {
            switch (data.remaining()) {
                case 3:
                    sum += ((data.getShort() << 16) + (data.get() << 8));
                    break;
                case 2:
                    sum += (data.getShort() << 16);
                    break;
                case 1:
                    sum += ((data.get() & 0xff) << 24);
                    break;
                default:
                    sum += data.getInt();
            }
        }

        data.reset();

        return sum;
    }

    
    
    
    /**
     * Get directory entries from a font
     */
    private static void parseDirectories(ByteBuffer data, int numTables,
            TrueTypeFont ttf) {
        for (int i = 0; i < numTables; i++) {
            int tag = data.getInt();
            String tagString = TrueTypeTable.tagToString(tag);
            SavelogPDF.d(TAG, debug, "parseDirectories: " + tagString);
            int checksum = data.getInt();
            int offset = data.getInt();
            int length = data.getInt();

            SavelogPDF.d(TAG, debug, "checksum: " + checksum  + "(" + Integer.toHexString(checksum)+")" );
            SavelogPDF.d(TAG, debug, "offset: " + offset  + "(" + Integer.toHexString(offset)+")");
            SavelogPDF.d(TAG, debug, "length: " + length + "(" + Integer.toHexString(length)  +")");

            // read the data
            data.mark();
            data.position(offset);

            ByteBuffer tableData = data.slice();
            tableData.limit(length);

            int calcChecksum = calculateChecksum(tagString, tableData);

            if (calcChecksum == checksum) {
                ttf.addTable(tagString, tableData);
            } else {
            	// Mismatched checksums are very common. Can be ignored under normal circumstances
                SavelogPDF.d(TAG, debug, "Mismatched checksums on table " + 
                tagString + ": " + calcChecksum + " != " +
                checksum);

                ttf.addTable(tagString, tableData);

            }
            data.reset();
        }
    }

    /**
     * Get the length of the font
     *
     * @return the length of the entire font, in bytes
     */
    private int getLength() {
        // the size of all the table directory entries
        int length = 12 + (getNumTables() * 16);

        // for each directory entry, get the size,
        // and don't forget the padding!
        for (Iterator i = tables.values().iterator(); i.hasNext();) {
            Object tableObj = i.next();

            // add the length of the entry
            if (tableObj instanceof TrueTypeTable) {
                length += ((TrueTypeTable) tableObj).getLength();
            } else {
                length += ((ByteBuffer) tableObj).remaining();
            }

            // pad
            if ((length % 4) != 0) {
                length += (4 - (length % 4));
            }
        }

        return length;
    }

    /**
     * Update the checksumAdj field in the head table
     */
    private void updateChecksumAdj(ByteBuffer fontData) {
        int checksum = calculateChecksum("", fontData);
        int checksumAdj = 0xb1b0afba - checksum;

        // find the head table
        int offset = 12 + (getNumTables() * 16);

        // find the head table
        for (Iterator i = tables.keySet().iterator(); i.hasNext();) {
            String tagString = (String) i.next();

            // adjust the checksum
            if (tagString.equals("head")) {
                fontData.putInt(offset + 8, (int)(checksumAdj * 0xffffffff));
                return;
            }

            // add the length of the entry 
            Object tableObj = tables.get(tagString);
            if (tableObj instanceof TrueTypeTable) {
                offset += ((TrueTypeTable) tableObj).getLength();
            } else {
                offset += ((ByteBuffer) tableObj).remaining();
            }

            // pad
            if ((offset % 4) != 0) {
                offset += (4 - (offset % 4));
            }
        }
    }

    /**
     * Write the font to a pretty string
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();

        System.out.println("Type         : " + getType());
        System.out.println("NumTables    : " + getNumTables());
        System.out.println("SearchRange  : " + getSearchRange());
        System.out.println("EntrySelector: " + getEntrySelector());
        System.out.println("RangeShift   : " + getRangeShift());

        for (Iterator i = tables.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();

            TrueTypeTable table = null;
            if (e.getValue() instanceof ByteBuffer) {
                table = getTable((String) e.getKey());
            } else {
                table = (TrueTypeTable) e.getValue();
            }

            System.out.println(table);
        }

        return buf.toString();
    }
}
