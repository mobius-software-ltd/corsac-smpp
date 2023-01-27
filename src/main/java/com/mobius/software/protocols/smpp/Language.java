package com.mobius.software.protocols.smpp;
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
import java.util.HashMap;
import java.util.Map;

import com.mobius.software.protocols.smpp.charsets.ExtentionEncodings;

public enum Language 
{
	DEFAULT(0),TURKISH(1),SPANISH(2),PORTUGUESE(3),BENGALI(4),GURAJATI(5),HINDI(6),KANNADA(7),MALAYALAM(8),ORIYA(9),PUNJABI(10),TAMIL(11),TELUGU(12),URDU(13);

	private static final Map<Integer, Language> intToTypeMap = new HashMap<Integer, Language>();
	static 
	{
	    for (Language type : Language.values()) 
	    {
	        intToTypeMap.put(type.value, type);
	    }
	}

	public static Language fromInt(int i) 
	{
		Language type = intToTypeMap.get(Integer.valueOf(i));
	    if (type == null) 
	        return Language.DEFAULT;
	    
	    return type;
	}
	
	private int value;
	
	private Language(int value)
	{
		this.value=value;
	}
	
	public int getValue()
	{
		return value;
	}
	
	public static Charset getCharset(Language language)
	{
		switch (language) 
		{
			case BENGALI:
				return ExtentionEncodings.gsm7CharsetBengali;				
			case GURAJATI:
				return ExtentionEncodings.gsm7CharsetGujarati;				
			case HINDI:
				return ExtentionEncodings.gsm7CharsetHindi;
			case KANNADA:
				return ExtentionEncodings.gsm7CharsetKannada;
			case MALAYALAM:
				return ExtentionEncodings.gsm7CharsetMalayalam;
			case ORIYA:
				return ExtentionEncodings.gsm7CharsetOriya;
			case PORTUGUESE:
				return ExtentionEncodings.gsm7CharsetPortugese;
			case PUNJABI:
				return ExtentionEncodings.gsm7CharsetPunjabi;
			case SPANISH:
				return ExtentionEncodings.gsm7CharsetSpanish;
			case TAMIL:
				return ExtentionEncodings.gsm7CharsetTamil;
			case TELUGU:
				return ExtentionEncodings.gsm7CharsetTelugu;
			case TURKISH:
				return ExtentionEncodings.gsm7CharsetTurkish;
			case URDU:
				return ExtentionEncodings.gsm7CharsetUrdu;
			default:
			case DEFAULT:
				return ExtentionEncodings.gsm7CharsetBasic;
		}
	}
}
