package com.sun.pdfview.font.cid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pdf.main.SavelogPDF;

import com.sun.pdfview.font.PDFCMap;

/*****************************************************************************
 * Parses a CMAP and builds a lookup table to map CMAP based codes to unicode.
 * This is not a fully functional CMAP parser but a stripped down parser
 * that should be able to parse some limited variants of CMAPs that are
 * used for the ToUnicode mapping found for some Type0 fonts.
 *
 * @author  Bernd Rosstauscher
 * @since 03.08.2011
 * @modified 06.24.2013 by A.Hui
 ****************************************************************************/

public class ToUnicodeMap extends PDFCMap {
	private static final String TAG = ToUnicodeMap.class.getSimpleName()+"_class";
	private static final String resourceFolder = "/com/sun/pdfview/font/cid/res/";
	private static final boolean debug = false;

	private String resourceName = null;
	
	/*****************************************************************************
	 * Small helper class to define a code range.
	 ****************************************************************************/

	// TODO: complete this when time permits. 
	private static class CodeRangeMapping {
		char srcStart;
		char srcEnd;
		
		CodeRangeMapping(char srcStart, char srcEnd) {
			this.srcStart = srcStart;
			this.srcEnd = srcEnd;
		}
		
		boolean contains(char c) {
			return this.srcStart <= c 
								&& c <= this.srcEnd;
		}
		
	}
	
	/*****************************************************************************
	 * Small helper class to define a char range.
	 ****************************************************************************/

	private static class CharRangeMapping {
		char srcStart;
		char srcEnd;
		char destStart;
		
		CharRangeMapping(char srcStart, char srcEnd, char destStart) {
			this.srcStart = srcStart;
			this.srcEnd = srcEnd;
			this.destStart = destStart;
		}
		
		boolean contains(char c) {
			return this.srcStart <= c 
								&& c <= this.srcEnd;
		}
		
		char map(char src) {
			return (char) (this.destStart + (src-this.srcStart));
		}
		
	}
	
	private final Map<Character, Character> singleCharMappings;
	private final List<CharRangeMapping> charRangeMappings;
	private final List<CodeRangeMapping> codeRangeMappings;

	/*************************************************************************
	 * Constructor
	 * @param map 
	 * @throws IOException 
	 ************************************************************************/
	
	public ToUnicodeMap(String registry, String ordering, int supplement, char cid) throws IOException {
		super();
		this.singleCharMappings = new HashMap<Character, Character>();
		this.charRangeMappings = new ArrayList<CharRangeMapping>();
		this.codeRangeMappings = new ArrayList<CodeRangeMapping>();

		resourceName = getResourceNameForCID(registry, ordering, supplement, cid);
    	SavelogPDF.d(TAG, debug, "Try to load " + resourceName + " for cid=" + Integer.toHexString(cid));
    	
    	// Note: file may not exist.
    	InputStream in = getClass().getResourceAsStream(resourceName);
    	byte[] cmapData = new byte[in.available()];
    	in.read(cmapData);
    	in.close();
    	parseBareMappings(cmapData);
	}
	
	/*************************************************************************
	 * @param map
	 * @throws IOException 
	 ************************************************************************/
		
	
	private String getResourceNameForCID(String registry, String ordering, int supplement, char cid) {
		String filename = "";
		String cidString = "";
		String extension = ".txt";

		// TODO: Only Japan1 has been tested. Others are not tested yet.
		if (registry.contains("Adobe")) {
			if (ordering.contains("CNS1")) {
				filename = "CNS1ToUnicode";
			}
			else if (ordering.contains("GB1")) {
				filename = "GB1ToUnicode";
			}
			else if (ordering.contains("Japan1")) {
				filename = "JapanToUnicode";
			}
			else if (ordering.contains("Korea1")) {
				filename = "KoreaToUnicode";
			}
			cidString = CIDToFileNumber(cid);
		}
		String resourcePath = resourceFolder + filename + cidString + extension;
		return resourcePath;
	}
	
	private String CIDToFileNumber(char cid) {
		String cidString = Integer.toHexString((int) (cid >> 8));
		if (cidString.length()==1) {  // add a preceding 0
			cidString = "0" + cidString;
		}
		return cidString;
	}

	public boolean mapAvailable(String registry, String ordering, int supplement, char cid) {
		if (resourceName==null) return false;
		String resourceNameForCID = getResourceNameForCID(registry, ordering, supplement, cid);
		return resourceName.equals(resourceNameForCID);
	}
	
	private void parseBareMappings(byte[] data) throws IOException {
		String delimiter = " ";
		int count=0;
		try {
			StringReader reader = new StringReader(new String(data, "ASCII"));
			BufferedReader bf = new BufferedReader(reader);
			String line = bf.readLine();
			while (line != null) {
				String[] mapping = line.split(delimiter);
				if (mapping.length == 2) {
					this.singleCharMappings.put(parseChar(mapping[0]), parseChar(mapping[1]));
				}
				else if (mapping.length == 3) {
					Character srcStart = parseChar(mapping[0]);
					Character srcEnd = parseChar(mapping[1]);
					Character destStart = parseChar(mapping[2]);
					this.charRangeMappings.add(new CharRangeMapping(srcStart, srcEnd, destStart));
				}
				line = bf.readLine();
				count++;
			}
		} catch (UnsupportedEncodingException e) {
			throw new IOException(e);
		} 
		SavelogPDF.d(TAG, debug, "parseBareMappings() read " + count + " lines."); 
	}

	

	/*************************************************************************
	 * Parse a string of the format <0F3A> to a char.
	 * @param charDef
	 * @return
	 ************************************************************************/
	
	private Character parseChar(String charDef) {
		if (charDef.startsWith("<")) {
			charDef = charDef.substring(1);
		}
		if (charDef.endsWith(">")) {
			charDef = charDef.substring(0, charDef.length()-1);
		}
		try {
			int result = Integer.decode("0x" + charDef);
			return (char) result;
		} catch (NumberFormatException e) {
			return (char) ' ';
		}
	}

	/*************************************************************************
	 * map
	 * @see com.sun.pdfview.font.cid.PDFCMap#map(char)
	 ************************************************************************/
	@Override
	public char map(char src) {
		Character mappedChar = null;
		for (CodeRangeMapping codeRange : this.codeRangeMappings) {
			if(codeRange.contains(src)) {
				mappedChar = this.singleCharMappings.get(src);
				if (mappedChar == null) {
					mappedChar = lookupInRanges(src);
				}
				break;
			}
		}
		if (mappedChar == null) {
			// TODO XOND 27.03.2012: PDF Spec. "9.7.6.3Handling Undefined Characters"
			mappedChar = 0;
		}
		return mappedChar;
	}

	/*************************************************************************
	 * @param src
	 * @return
	 ************************************************************************/
	
	private Character lookupInRanges(char src) {
		Character mappedChar = null;
		for (CharRangeMapping rangeMapping : this.charRangeMappings) {
			if (rangeMapping.contains(src)) {
				mappedChar = rangeMapping.map(src);
				break;
			}
		}
		return mappedChar;
	}

	public char getChar(char cid) {
		Character unicode = 0;
		unicode = this.singleCharMappings.get(Character.valueOf(cid));
		if (unicode!=null) return unicode;
		else {
			for (CharRangeMapping m : this.charRangeMappings) {
				if (m.contains(cid)) {
					unicode = m.map(cid);
				}
			}
		}
		return unicode;
	}


}