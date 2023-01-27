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

public enum EsmClass 
{
	MM_DEFAULT(0x00),MM_DATAGRAM(0x01),MM_TRANSACTION(0x02),MM_STORE_FORWARD(0x03),MT_SMSC_DELIVERY_RECEIPT(0x04),MT_ESME_DELIVERY_RECEIPT(0x08),MT_MANUAL_USER_ACK(0x10),INTERMEDIATE_DELIVERY_RECEIPT_FLAG(0x20),UDHI(0x40),REPLY_PATH(0x80);

	
	public static final short MT_MASK = 0x1C;
	
	private static final Map<Integer, EsmClass> intToTypeMap = new HashMap<Integer, EsmClass>();
	static 
	{
	    for (EsmClass type : EsmClass.values()) 
	    {
	        intToTypeMap.put(type.value, type);
	    }
	}

	public static EsmClass fromInt(int i) 
	{
		EsmClass type = intToTypeMap.get(Integer.valueOf(i));
	    if (type == null) 
	        return EsmClass.MM_DEFAULT;
	    
	    return type;
	}
	
	private int value;
	
	private EsmClass(int value)
	{
		this.value=value;
	}
	
	public int getValue()
	{
		return value;
	}
}
