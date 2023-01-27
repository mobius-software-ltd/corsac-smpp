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

public enum TypeOfNetwork 
{
	UNKNOWN(0),INTERNATIONAL(1),NATIONAL(2),NETWORK(3),SUBSCRIBER(4),ALPHANUMERIC(5),ABBREVIATED(6),RESERVED_EXTN(7);

	private static final Map<Integer, TypeOfNetwork> intToTypeMap = new HashMap<Integer, TypeOfNetwork>();
	static 
	{
	    for (TypeOfNetwork type : TypeOfNetwork.values()) 
	    {
	        intToTypeMap.put(type.value, type);
	    }
	}

	public static TypeOfNetwork fromInt(int i) 
	{
		TypeOfNetwork type = intToTypeMap.get(Integer.valueOf(i));
	    if (type == null) 
	        return TypeOfNetwork.UNKNOWN;
	    
	    return type;
	}
	
	private int value;
	
	private TypeOfNetwork(int value)
	{
		this.value=value;
	}
	
	public int getValue()
	{
		return value;
	}
}
