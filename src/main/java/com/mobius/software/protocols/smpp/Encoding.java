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
import java.util.HashMap;
import java.util.Map;

public enum Encoding 
{
	DEFAULT(0), IA5(1),OCTET_UNSPECIFIED_1(2),ISO_8859_1(3),OCTET_UNSPECIFIED_2(4),JIS(5),CYRLLIC(6),LATIN_HEBREW(7),UTF_16(8),PICTOGRAM(9),ISO2022JP(10),RESERVED_1(11),RESERVED_2(12),JISX(13),KS_C(14);
    
	private static final Map<Integer, Encoding> intToTypeMap = new HashMap<Integer, Encoding>();
	static 
	{
	    for (Encoding type : Encoding.values()) 
	    {
	        intToTypeMap.put(type.value, type);
	    }
	}

	public static Encoding fromInt(int i) 
	{
		Encoding type = intToTypeMap.get(Integer.valueOf(i));
	    if (type == null) 
	        return Encoding.DEFAULT;
	    
	    return type;
	}
	
	private int value;
	
	private Encoding(int value)
	{
		this.value=value;
	}
	
	public int getValue()
	{
		return value;
	}
}
