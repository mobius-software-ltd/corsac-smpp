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

public enum CommandType 
{
	CMD_ID_BIND_RECEIVER(0x00000001),CMD_ID_BIND_TRANSMITTER(0x00000002),CMD_ID_QUERY_SM(0x00000003),
    CMD_ID_SUBMIT_SM(0x00000004),CMD_ID_DELIVER_SM(0x00000005),CMD_ID_UNBIND(0x00000006),CMD_ID_REPLACE_SM(0x00000007),
    CMD_ID_CANCEL_SM(0x00000008),CMD_ID_BIND_TRANSCEIVER(0x00000009),CMD_ID_OUTBIND(0x0000000B),CMD_ID_ENQUIRE_LINK(0x00000015),
    CMD_ID_SUBMIT_MULTI(0x00000021),CMD_ID_ALERT_NOTIFICATION(0x00000102),CMD_ID_DATA_SM(0x00000103),
    CMD_ID_BROADCAST_SM(0x00000111),CMD_ID_QUERY_BROADCAST_SM(0x00000112),CMD_ID_CANCEL_BROADCAST_SM(0x00000113),
    CMD_ID_GENERIC_NACK(0x80000000),CMD_ID_BIND_RECEIVER_RESP(0x80000001),CMD_ID_BIND_TRANSMITTER_RESP(0x80000002),
    CMD_ID_QUERY_SM_RESP(0x80000003),CMD_ID_SUBMIT_SM_RESP(0x80000004),CMD_ID_DELIVER_SM_RESP(0x80000005),
    CMD_ID_UNBIND_RESP(0x80000006),CMD_ID_REPLACE_SM_RESP(0x80000007),CMD_ID_CANCEL_SM_RESP(0x80000008),
    CMD_ID_BIND_TRANSCEIVER_RESP(0x80000009),CMD_ID_ENQUIRE_LINK_RESP(0x80000015),CMD_ID_SUBMIT_MULTI_RESP(0x80000021),
    CMD_ID_DATA_SM_RESP(0x80000103),CMD_ID_BROADCAST_SM_RESP(0x80000111),CMD_ID_QUERY_BROADCAST_SM_RESP(0x80000112),
    CMD_ID_CANCEL_BROADCAST_SM_RESP(0x80000113),CMD_ID_UNKNOWN(0xFFFFFFFF);
    
	private static final Map<Integer, CommandType> intToTypeMap = new HashMap<Integer, CommandType>();
	static 
	{
	    for (CommandType type : CommandType.values()) 
	    {
	        intToTypeMap.put(type.value, type);
	    }
	}

	public static CommandType fromInt(int i) 
	{
		CommandType type = intToTypeMap.get(Integer.valueOf(i));
	    if (type == null) 
	        return CommandType.CMD_ID_UNKNOWN;
	    
	    return type;
	}
	
	private int value;
	
	private CommandType(int value)
	{
		this.value=value;
	}
	
	public int getValue()
	{
		return value;
	}
}
