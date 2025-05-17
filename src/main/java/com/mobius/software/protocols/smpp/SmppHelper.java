package com.mobius.software.protocols.smpp;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.cloudhopper.commons.charset.PackedGSMCharset;
import com.cloudhopper.commons.util.ByteUtil;
import com.mobius.software.protocols.smpp.channel.EsmClass;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

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
public class SmppHelper 
{
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static final short TAG_RECEIPTED_MSG_ID = 0x001E;
	public static final short TAG_MESSAGE_PAYLOAD = 0x0424;
	public static final short TAG_MSG_STATE = 0x0427;
	
	public static final short TAG_MSG_REF_NUM = 0x020C;
    public static final short TAG_LANGUAGE_INDICATOR = 0x020D;
    public static final short TAG_SEGMENT_SEQNUM = 0x020F;
    public static final short TAG_TOTAL_SEGMENTS = 0x020E;
    
	public static final String DELIVERY_ACK_ID = "id:";
	public static final String DELIVERY_ACK_SUB = " sub:";
	public static final String DELIVERY_ACK_DLVRD = " dlvrd:";
	public static final String DELIVERY_ACK_SUBMIT_DATE = " submit date:";
	public static final String DELIVERY_ACK_DONE_DATE = " done date:";
	public static final String DELIVERY_ACK_STAT = " stat:";
	public static final String DELIVERY_ACK_ERR = " err:";
	public static final String DELIVERY_ACK_TEXT = " text:";
	    
	private static PackedGSMCharset gsmCharset=new PackedGSMCharset();
	public static final SimpleDateFormat DELIVERY_ACK_DATE_FORMAT = new SimpleDateFormat("yyMMddHHmm");
	
	public static Date parseSmppDate(String val) throws ParseException 
	{
		if (val == null || val.length() == 0)
			return null;
	
		if (val.length() != 16) 
			throw new ParseException("Absolute or relative time formats must be 16 characters length", 0);
		
		char sign = val.charAt(15);
		Date res;
		if (sign == 'R') 
		{
			String yrS = val.substring(0, 2);
			String mnS = val.substring(2, 4);
			String dyS = val.substring(4, 6);
			String hrS = val.substring(6, 8);
			String miS = val.substring(8, 10);
			String scS = val.substring(10, 12);
			int yr = Integer.parseInt(yrS);
			int mn = Integer.parseInt(mnS);
			int dy = Integer.parseInt(dyS);
			int hr = Integer.parseInt(hrS);
			int mi = Integer.parseInt(miS);
			int sc = Integer.parseInt(scS);
			Calendar c = Calendar.getInstance();
			c.setTime(new Date());
			c.add(Calendar.YEAR, yr);
			c.add(Calendar.MONTH, mn);
			c.add(Calendar.DATE, dy);
			c.add(Calendar.HOUR, hr);
			c.add(Calendar.MINUTE, mi);
			c.add(Calendar.SECOND, sc);
			res = c.getTime();
		} 
		else 
		{
			String s2 = val.substring(12, 13);
			String s3 = val.substring(13, 15);

			String yrS = val.substring(0, 2);
			String mnS = val.substring(2, 4);
			String dyS = val.substring(4, 6);
			String hrS = val.substring(6, 8);
			String miS = val.substring(8, 10);
			String scS = val.substring(10, 12);
			int yr = Integer.parseInt(yrS);
			int mn = Integer.parseInt(mnS);
			int dy = Integer.parseInt(dyS);
			int hr = Integer.parseInt(hrS);
			int mi = Integer.parseInt(miS);
			int sc = Integer.parseInt(scS);
			
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			c.set(Calendar.YEAR, c.get(Calendar.YEAR) - c.get(Calendar.YEAR)%100 + yr);
			c.set(Calendar.MONTH, mn);
			c.set(Calendar.DATE, dy);
			c.set(Calendar.HOUR, hr);
			c.set(Calendar.MINUTE, mi);
			c.set(Calendar.SECOND, sc);
			c.set(Calendar.MILLISECOND,0);
			
			int dSec = Integer.parseInt(s2);
			int tZone = Integer.parseInt(s3);
			switch (sign) {
				case '+':
					res = new Date(c.getTimeInMillis() + dSec * 100 - tZone * 15 * 60 * 1000);
					break;
				case '-':
					res = new Date(c.getTimeInMillis() + dSec * 100 + tZone * 15 * 60 * 1000);
					break;
				default:
					throw new ParseException("16-th character must be '+' or '-' for absolute time format or 'R' for relative time format", 16);
			}
		}			

		return res;
	}
	
	public static String printSmppAbsoluteDate(Long date, int timezoneOffset) 
	{
		StringBuilder sb = new StringBuilder();

		Calendar calendar=GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(date);
		
		int year = calendar.get(Calendar.YEAR)%100;
		int month = calendar.get(Calendar.MONTH) + 1;
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int min = calendar.get(Calendar.MINUTE);
		int sec = calendar.get(Calendar.SECOND);
		int tSec = calendar.get(Calendar.MILLISECOND)/100;

		addDateToStringBuilder(sb, year, month, day, hour, min, sec);

		if (tSec > 9)
			tSec = 9;
		
		if (tSec < 0)
			tSec = 0;
		
		sb.append(tSec);

		int tz = timezoneOffset / 15;
		char sign;
		if (tz < 0) 
		{
			sign = '-';
			tz = -tz;
		} 
		else 
			sign = '+';
		
		if (tz < 10)
			sb.append("0");
		
		sb.append(tz);
		sb.append(sign);		
		return sb.toString();
	}
	
	public static String parseShortMessageText(DeliverSm event) throws UnsupportedEncodingException 
	{
        byte[] data = event.getShortMessage();
        byte[] udh = null;
        if (event.getShortMessageLength() == 0) 
        {
            Tlv messagePaylod = event.getOptionalParameter(TAG_MESSAGE_PAYLOAD);
            if (messagePaylod != null) 
                data = messagePaylod.getValue();            
        }
        
        if (data == null) 
            data = new byte[0];
        
        boolean udhPresent = (event.getEsmClass() & EsmClass.UDHI.getValue()) != 0;
        int start=0;
        int length=data.length;
        if (udhPresent && data.length > 2) 
        {
        	int udhLen = (data[0] & 0xFF) + 1;
            if (udhLen <= data.length) 
            {
            	start=udhLen;
            	length=length-start;                               
            }
        }
        
        if(start!=0)
		{
			byte[] temp=new byte[length];
			System.arraycopy(data, start, temp, 0, length);
			udh = getShortMessageUserDataHeader(data);
			data=temp;			
		}
        
        return translateMessage(Encoding.fromInt(event.getDataCoding()), data, udh);        
    }		
	
	public static String createDeliveryReport(String messageId, Date submitDate, Date deliveryDate, int errorCode, String messageText, DeliveryStatus deliveryStatus)
	{
		StringBuilder sb=new StringBuilder();
		sb.append(DELIVERY_ACK_ID);
		sb.append(messageId);
		sb.append(DELIVERY_ACK_SUB);
        sb.append("001");
        sb.append(DELIVERY_ACK_DLVRD);
        if (deliveryStatus==DeliveryStatus.DELIVERED) 
            sb.append("001");
        else 
            sb.append("000");
        
        sb.append(DELIVERY_ACK_SUBMIT_DATE);
        sb.append(DELIVERY_ACK_DATE_FORMAT.format(submitDate));
        sb.append(DELIVERY_ACK_DONE_DATE);
        sb.append(DELIVERY_ACK_DATE_FORMAT.format(deliveryDate));
        sb.append(DELIVERY_ACK_STAT);        
        
        switch(deliveryStatus)
        {
			case DELETED:
				sb.append("DELETED");
				break;
			case DELIVERED:
				sb.append("DELIVRD");
				break;
			case ENROUTE:
				sb.append("ENROUTE");
				break;
			case EXPIRED:
				sb.append("EXPIRED");
				break;
			case REJECTED:
				sb.append("REJECTD");
				break;
			case SCHEDULED:
				sb.append("SCHEDLD");
				break;
			case SKIPPED:
				sb.append("SKIPPED");
				break;
			case UNDELIVERABLE:
				sb.append("UNDELIV");
				break;
			case ACCEPTED:
			case UNKNOWN:
			default:
				sb.append("ACCEPTD");
				break;        
        }
        
        sb.append(DELIVERY_ACK_ERR);
        sb.append(String.format("%03d", errorCode));
        sb.append(DELIVERY_ACK_TEXT);
        sb.append(messageText);
        
        return sb.toString();
	}
	
	private static void addDateToStringBuilder(StringBuilder sb, int year, int month, int day, int hour, int min, int sec) 
	{
		if (year < 10)
			sb.append("0");
		sb.append(year);
		
		if (month < 10)
			sb.append("0");
		sb.append(month);
		
		if (day < 10)
			sb.append("0");
		sb.append(day);
		
		if (hour < 10)
			sb.append("0");
		sb.append(hour);
		
		if (min < 10)
			sb.append("0");
		sb.append(min);
		
		if (sec < 10)
			sb.append("0");
		sb.append(sec);
	}
	
	static public byte[] getShortMessageUserData(byte[] shortMessage) throws IllegalArgumentException {
		if (shortMessage == null) {
            return null;
        }

        if (shortMessage.length == 0) {
            return shortMessage;
        }

        // the entire length of UDH is the first byte + the length
        int userDataHeaderLength = ByteUtil.decodeUnsigned(shortMessage[0]) + 1;

        // is there enough data?
        if (userDataHeaderLength > shortMessage.length) {
            throw new IllegalArgumentException("User data header length exceeds short message length [shortMessageLength=" + shortMessage.length + ", userDataHeaderLength=" + userDataHeaderLength + "]");
        }

        // create a new message with the header removed
        int newShortMessageLength = shortMessage.length - userDataHeaderLength;
        byte[] newShortMessage = new byte[newShortMessageLength];

        System.arraycopy(shortMessage, userDataHeaderLength, newShortMessage, 0, newShortMessageLength);

        return newShortMessage;
    }
	
	static public byte[] getShortMessageUserDataHeader(byte[] shortMessage) throws IllegalArgumentException {
        if (shortMessage == null) {
            return null;
        }

        if (shortMessage.length == 0) {
            return shortMessage;
        }

        // the entire length of UDH is the first byte + the length
        int userDataHeaderLength = ByteUtil.decodeUnsigned(shortMessage[0]);

        // is there enough data?
        if (userDataHeaderLength+1 > shortMessage.length) {
            throw new IllegalArgumentException("User data header length exceeds short message length [shortMessageLength=" + shortMessage.length + ", userDataHeaderLength=" + userDataHeaderLength + "]");
        }

        // create a new message with just the header
        byte[] userDataHeader = new byte[userDataHeaderLength];
        System.arraycopy(shortMessage, 1, userDataHeader, 0, userDataHeaderLength);

        return userDataHeader;
    }
	
	public static boolean isUserDataHeaderIndicatorEnabled(byte esmClass) 
	{
        return ((esmClass & EsmClass.UDHI.getValue()) == EsmClass.UDHI.getValue());
    }
	
	public static boolean isMessageTypeAnyDeliveryReceipt(byte esmClass) 
	{
        return ((esmClass & EsmClass.MT_MASK) > 0);
    }
	
	public static ShiftType getShiftType(byte[] udh)
	{
		ShiftType shiftType=null;
		if(udh!=null && udh.length>0)
		{
			ByteBuf udhBuffer=Unpooled.wrappedBuffer(udh);
			while(udhBuffer.readableBytes()>0 && (shiftType==null || shiftType==ShiftType.NONE))
			{
				shiftType=ShiftType.fromInt(udhBuffer.readUnsignedByte());
				Integer length=udhBuffer.readByte() & 0x0FF;
				udhBuffer.skipBytes(length);
			}
		}
		
		return shiftType;
	}
	
	public static Language getLanguage(byte[] udh)
	{
		Language language=null;
		ShiftType shiftType=null;
		if(udh!=null && udh.length>0)
		{
			ByteBuf udhBuffer=Unpooled.wrappedBuffer(udh);
			while(udhBuffer.readableBytes()>0 && (shiftType==null || shiftType==ShiftType.NONE))
			{
				shiftType=ShiftType.fromInt(udhBuffer.readUnsignedByte());
				Integer length=udhBuffer.readByte() & 0x0FF;
				if(shiftType!=null && shiftType!=ShiftType.NONE)
				{
					if(length==1)
						language=Language.fromInt(udhBuffer.readUnsignedByte());
					else
						udhBuffer.skipBytes(length);
				}
				else
					udhBuffer.skipBytes(length);
			}
		}
		
		return language;
	}
	
	public static String translateMessage(Encoding encoding,byte[] data,byte[] udh)
	{
		Language language=null;
		ShiftType shiftType=null;
		if(udh!=null && udh.length>0)
		{
			ByteBuf udhBuffer=Unpooled.wrappedBuffer(udh);
			while(udhBuffer.readableBytes()>0 && (shiftType==null || shiftType==ShiftType.NONE))
			{
				shiftType=ShiftType.fromInt(udhBuffer.readUnsignedByte());
				Integer length=udhBuffer.readByte() & 0x0FF;
				if(shiftType!=null && shiftType!=ShiftType.NONE)
				{
					if(length==1)
						language=Language.fromInt(udhBuffer.readUnsignedByte());
					else
						udhBuffer.skipBytes(length);
				}
				else
					udhBuffer.skipBytes(length);
			}
		}
		
		if(shiftType!=null && shiftType!=ShiftType.NONE && language!=null)
		{
			Charset currCharset=Language.getCharset(language);
			return currCharset.decode(ByteBuffer.wrap(data)).toString();
		}
		
		String result=new String(data);		
		try
		{
			switch(encoding)
			{
				case CYRLLIC:
					result=new String(data,"ISO-8859-5");
					break;
				case ISO2022JP:				
					result=new String(data,"ISO-2022-JP");
					break;
				case ISO_8859_1:
					result=new String(data,"ISO-8859-1");
					break;
				case IA5:
					result=new String(data,"US-ASCII");
					break;
				case JIS:
					result=new String(data,"JIS_X0201");
					break;
				case JISX:
					result=new String(data,"JIS_X0212-1990");
					break;
				case KS_C:
					result=new String(data,"x-Johab");
					break;
				case LATIN_HEBREW:
					result=new String(data,"ISO-8859-8");
					break;
				case UTF_16:
					result=new String(data,"UTF-16BE");				
					break;
				case DEFAULT:
					result=gsmCharset.decode(data);
					break;
				default:					
				case OCTET_UNSPECIFIED_1:				
				case OCTET_UNSPECIFIED_2:
				case RESERVED_1:				
				case RESERVED_2:
				case PICTOGRAM:
					break;			
			}
		}
		catch(Exception ex)
		{
			
		}
		
		return result;
	}
	
	public static byte[] translateMessage(Encoding encoding,String data,ShiftType shiftType,Language language)
	{
		if(shiftType!=null && shiftType!=ShiftType.NONE && language!=null)
		{
			Charset currCharset=Language.getCharset(language);
			ByteBuffer encodedData=currCharset.encode(data);
			byte[] result=new byte[encodedData.remaining()];
			encodedData.get(result);
			return result;
		}	
		
		byte[] result=data.getBytes();
		try
		{
			switch(encoding)
			{
				case CYRLLIC:
					result=data.getBytes("ISO-8859-5");
					break;
				case ISO2022JP:				
					result=data.getBytes("ISO-2022-JP");
					break;
				case ISO_8859_1:
					result=data.getBytes("ISO-8859-1");
					break;
				case IA5:
					result=data.getBytes("US-ASCII");
					break;
				case JIS:
					result=data.getBytes("JIS_X0201");
					break;
				case JISX:
					result=data.getBytes("JIS_X0212-1990");
					break;
				case KS_C:
					result=data.getBytes("x-Johab");
					break;
				case LATIN_HEBREW:
					result=data.getBytes("ISO-8859-8");
					break;
				case PICTOGRAM:
					break;
				case DEFAULT:
					result=gsmCharset.encode(data);					
				case OCTET_UNSPECIFIED_1:				
				case OCTET_UNSPECIFIED_2:
				case RESERVED_1:				
				case RESERVED_2:
					break;
				case UTF_16:
					result=data.getBytes("UTF-16BE");				
					break;
				default:
					break;			
			}
		}
		catch(Exception ex)
		{
			
		}
		
		return result;
	}

	public static String bytesToHex(byte[] bytes)
	{
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++)
		{
			int v = bytes[j] & 0xFF;
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
			hexChars[j * 2] = hexArray[v >>> 4];
		}

		return new String(hexChars);
	}
}
