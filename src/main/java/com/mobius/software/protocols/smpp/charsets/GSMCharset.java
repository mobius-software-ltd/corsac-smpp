package com.mobius.software.protocols.smpp.charsets;
/*
 * Mobius Software LTD
 * Copyright 2019 - 2023, Mobius Software LTD and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class GSMCharset extends Charset 
{

    public static final String GSM_CANONICAL_NAME = "GSM";

    protected static final float averageCharsPerByte = 8 / 7f;
    protected static final float maxCharsPerByte = 2f;

    protected static final float averageBytesPerChar = 2f;
    protected static final float maxBytesPerChar = 2f;

    protected static final int BUFFER_SIZE = 256;

    public static final byte ESCAPE = 0x1B;

    protected int[] mainTable;
    protected int[] extensionTable;
    protected ConcurrentHashMap<Integer,Integer> reverseMainTable;
    protected ConcurrentHashMap<Integer,Integer> reverseExtensionTable;
    
    public GSMCharset(String canonicalName, String[] aliases) {
        this(canonicalName, aliases, ExtentionEncodings.basicMap, ExtentionEncodings.basicExtentionMap);
    }

    public GSMCharset(String canonicalName, String[] aliases, int[] mainTable, int[] extentionTable) {
        super(canonicalName, aliases);

        this.mainTable = mainTable;
        this.extensionTable = extentionTable;
        
        reverseMainTable=new ConcurrentHashMap<Integer,Integer>();
        reverseExtensionTable=new ConcurrentHashMap<Integer,Integer>();
        
        if(this.mainTable!=null)
        {
	        for(int i=0;i<mainTable.length;i++)
	        {
	        	if(mainTable[i]!=0x0000)
	        		reverseMainTable.put(mainTable[i], i);
	        }
        }
        
        if(this.extensionTable!=null)
        {
	        for(int i=0;i<extentionTable.length;i++)
	        {
	        	if(extentionTable[i]!=0x0000)
	        		reverseExtensionTable.put(extentionTable[i], i);
	        }
        }
    }

    @Override
    public boolean contains(Charset cs) {
        return this.getClass().isInstance(cs);
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new GSMCharsetDecoder(this, averageCharsPerByte, maxCharsPerByte);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new GSMCharsetEncoder(this, averageBytesPerChar, maxBytesPerChar);
    }

    /**
     * Returns true if all characters in data String is included in main and extension encoding tables of the GSM7 charset
     *
     * @param data
     * @return
     */
    public boolean checkAllCharsCanBeEncoded(String data) {
        return checkAllCharsCanBeEncoded(data, this.mainTable, this.extensionTable);
    }

    /**
     * Returns true if all characters in data String is included in main and extension encoding tables of the GSM7 charset
     *
     * @param data
     * @return
     */
    public static boolean checkAllCharsCanBeEncoded(String data, int[] mainTable, int[] extentionTable) {
        if (data == null)
            return true;

        if (mainTable == null)
            return false;

        for (int i1 = 0; i1 < data.length(); i1++) {
            char c = data.charAt(i1);

            boolean found = false;
            for (int i = 0; i < mainTable.length; i++) {
                if (mainTable[i] == c) {
                    found = true;
                    break;
                }
            }
            if (!found && extentionTable != null) {
                for (int i = 0; i < extentionTable.length; i++) {
                    if (extentionTable[i] == c) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found)
                return false;
        }

        return true;
    }

    /**
     * Returns a count in characters / septets of the data String after which the String will be GSM7 style encoded. For all
     * characters from the extension character table two bytes will be reserved. For all characters from the main character
     * table or which are not present in main or extension character tables one byte will be reserved.
     *
     * @param data
     * @return
     */
    public int checkEncodedDataLengthInChars(String data) {
        return checkEncodedDataLengthInChars(data, this.mainTable, this.extensionTable);
    }

    /**
     * Returns a count in characters / septets of the data String after which the String will be GSM7 style encoded. For all
     * characters from the extension character table two bytes will be reserved. For all characters from the main character
     * table or which are not present in main or extension character tables one byte will be reserved.
     *
     * @param data
     * @return
     */
    public static int checkEncodedDataLengthInChars(String data, int[] mainTable, int[] extentionTable) {
        if (data == null)
            return 0;

        if (mainTable == null)
            return 0;

        int cnt = 0;
        for (int i1 = 0; i1 < data.length(); i1++) {
            char c = data.charAt(i1);

            boolean found = false;
            for (int i = 0; i < mainTable.length; i++) {
                if (mainTable[i] == c) {
                    found = true;
                    cnt++;
                    break;
                }
            }
            if (!found && extentionTable != null) {
                for (int i = 0; i < extentionTable.length; i++) {
                    if (extentionTable[i] == c) {
                        found = true;
                        cnt += 2;
                        break;
                    }
                }
            }
            if (!found)
                cnt++;
        }

        return cnt;
    }

    /**
     * Calculates how many octets encapsulate the provides septets count.
     *
     * @param data
     * @return
     */
    public static int septetsToOctets(int septCnt) {
        int byteCnt = (septCnt + 1) * 7 / 8;
        return byteCnt;
    }

    /**
     * Calculates how many septets are encapsulated in the provides octets count.
     *
     * @param data
     * @return
     */
    public static int octetsToSeptets(int byteCnt) {
        int septCnt = (byteCnt * 8 - 1) / 7 + 1;
        return septCnt;
    }

    /**
     * Slicing of a data String into substrings that fits to characters / septets count in charCount parameter.
     *
     * @param data
     * @return
     */
    public String[] sliceString(String data, int charCount) {
        return sliceString(data, charCount, this.mainTable, this.extensionTable);
    }

    /**
     * Slicing of a data String into substrings that fits to characters / septets count in charCount parameter.
     *
     * @param data
     * @return
     */
    public static String[] sliceString(String data, int charCount, int[] mainTable, int[] extentionTable) {
        if (data == null)
            return null;

        if (mainTable == null)
            return null;

        ArrayList<String> res = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        int chCnt = 0;
        for (int i1 = 0; i1 < data.length(); i1++) {
            char c = data.charAt(i1);

            boolean found = false;
            for (int i = 0; i < mainTable.length; i++) {
                if (mainTable[i] == c) {
                    found = true;
                    chCnt++;
                    if (chCnt > charCount) {
                        chCnt = 1;
                        res.add(sb.toString());
                        sb = new StringBuilder();
                    }
                    sb.append(c);
                    break;
                }
            }
            if (!found && extentionTable != null) {
                for (int i = 0; i < extentionTable.length; i++) {
                    if (extentionTable[i] == c) {
                        found = true;
                        chCnt += 2;
                        if (chCnt > charCount) {
                            chCnt = 2;
                            res.add(sb.toString());
                            sb = new StringBuilder();
                        }
                        sb.append(c);
                        break;
                    }
                }
            }
            if (!found) {
                chCnt++;
                if (chCnt > charCount) {
                    chCnt = 1;
                    res.add(sb.toString());
                    sb = new StringBuilder();
                }
                sb.append(c);
            }
        }

        res.add(sb.toString());
        String[] arr = new String[res.size()];
        res.toArray(arr);
        return arr;
    }   
}
