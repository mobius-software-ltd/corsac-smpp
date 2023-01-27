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

public enum Priority 
{
	NON_PRIORITY(0),NORMAL(1),HIGH(2),URGENT(3);

	private static final Map<Integer, Priority> intToTypeMap = new HashMap<Integer, Priority>();
	static 
	{
	    for (Priority type : Priority.values()) 
	    {
	        intToTypeMap.put(type.value, type);
	    }
	}

	public static Priority fromInt(int i) 
	{
		Priority type = intToTypeMap.get(Integer.valueOf(i));
	    if (type == null) 
	        return Priority.NON_PRIORITY;
	    
	    return type;
	}
	
	private int value;
	
	private Priority(int value)
	{
		this.value=value;
	}
	
	public int getValue()
	{
		return value;
	}
}
