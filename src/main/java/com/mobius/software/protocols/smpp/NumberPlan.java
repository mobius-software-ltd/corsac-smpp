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

public enum NumberPlan 
{
	UNKNOWN(0),E164(1),ISDN(2),X121(3),TELEX(4),LAND_MOBILE(6),NATIONAL(8),PRIVATE(9),ERMES(10),INTERNET(14),WAP_CLIENT_ID(18);

	private static final Map<Integer, NumberPlan> intToTypeMap = new HashMap<Integer, NumberPlan>();
	static 
	{
	    for (NumberPlan type : NumberPlan.values()) 
	    {
	        intToTypeMap.put(type.value, type);
	    }
	}

	public static NumberPlan fromInt(int i) 
	{
		NumberPlan type = intToTypeMap.get(Integer.valueOf(i));
	    if (type == null) 
	        return NumberPlan.UNKNOWN;
	    
	    return type;
	}
	
	private int value;
	
	private NumberPlan(int value)
	{
		this.value=value;
	}
	
	public int getValue()
	{
		return value;
	}
}
