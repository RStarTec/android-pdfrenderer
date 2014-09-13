/*
 * $Id: HexDump.java,v 1.3 2009/01/16 16:26:12 tomoke Exp $
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
package com.sun.pdfview;

import java.io.IOException;
import java.io.RandomAccessFile;

import pdf.main.SavelogPDF;

public class HexDump {
	private static final String TAG = HexDump.class.getSimpleName()+"_class";

    public static void printData(byte[] data) {
    	String logData = "";
        char[] parts = new char[17];
        int partsloc = 0;
        for (int i = 0; i < data.length; i++) {
            int d = ((int) data[i]) & 0xff;
            if (d == 0) {
                parts[partsloc++] = '.';
            } else if (d < 32 || d >= 127) {
                parts[partsloc++] = '?';
            } else {
                parts[partsloc++] = (char) d;
            }
            if (i % 16 == 0) {
                int start = Integer.toHexString(data.length).length();
                int end = Integer.toHexString(i).length();

                for (int j = start; j > end; j--) {
                    logData += "0";
                }
                logData += Integer.toHexString(i) + ": ";
            }
            if (d < 16) {
            	logData += "0" + Integer.toHexString(d);
            } else {
            	logData += Integer.toHexString(d);
            }
            if ((i & 15) == 15 || i == data.length - 1) {
            	logData += "      " + new String(parts) + "\n";
                partsloc = 0;
            } else if ((i & 7) == 7) {
            	logData +="  ";
                parts[partsloc++] = ' ';
            } else if ((i & 1) == 1) {
            	logData +=" ";
            }
        }
        SavelogPDF.i(TAG, logData);
    }

    public static void main(String args[]) {
        if (args.length != 1) {
            SavelogPDF.i(TAG, "Usage: ");
            SavelogPDF.i(TAG, "    HexDump <filename>");
            System.exit(-1);
        }

        try {
            RandomAccessFile raf = new RandomAccessFile(args[0], "r");

            int size = (int) raf.length();
            byte[] data = new byte[size];

            raf.readFully(data);
            printData(data);
        } catch (IOException ioe) {
        	SavelogPDF.e(TAG, ioe.getMessage());
        }
    }
}
