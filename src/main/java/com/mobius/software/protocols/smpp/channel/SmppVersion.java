package com.mobius.software.protocols.smpp.channel;
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

public enum SmppVersion 
{
	VERSION_3_3(0x33),VERSION_3_4(0x34),VERSION_5_0(0x50);

	public static final short TAG_SC_INTERFACE_VERSION = 0x0210;
	
	private static final Map<Integer, SmppVersion> intToTypeMap = new HashMap<Integer, SmppVersion>();
	static 
	{
	    for (SmppVersion type : SmppVersion.values()) 
	    {
	        intToTypeMap.put(type.value, type);
	    }
	}

	public static SmppVersion fromInt(int i) 
	{
		SmppVersion type = intToTypeMap.get(Integer.valueOf(i));
	    if (type == null) 
	        return SmppVersion.VERSION_3_4;
	    
	    return type;
	}
	
	private int value;
	
	private SmppVersion(int value)
	{
		this.value=value;
	}
	
	public int getValue()
	{
		return value;
	}
}
