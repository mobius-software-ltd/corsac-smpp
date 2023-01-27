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

public enum MessageStatus 
{
	OK(0),INVMSGLEN(1),INVCMDLEN(2),INVCMDID(3),INVBNDSTS(4),ALYBND(5),INVPRTFLG(6),INVREGDLVFLG(7),SYSERR(8),INVSRCADR(10),INVDSTADR(11),INVMSGID(12),BINDFAIL(13),INVPASWD(14),INVSYSID(16),CANCELFAIL(17),REPLACEFAIL(19),MSGQFUL(20),INVSERTYP(21),RESERVED(30),INVNUMDESTS(51),INVDLNAME(52),INVDESTFLAG(64),INVSUBREP(66),INVESMCLASS(67),CNTSUBDL(68),SUBMITFAIL(69),INVSRCTON(72),INVSRCNPI(73),INVDSTTON(80),INVDSTNPI(81),INVSYSTYP(83),INVREPFLAG(84),INVNUMMSGS(85),THROTTLED(88),INVSCHED(97),INVEXPIRY(98),INVDFTMSGID(99),X_T_APPN(100),X_P_APPN(101),X_R_APPN(102),QUERYFAIL(103),INVOPTPARSTREAM(192),OPTPARNOTALLWD(193),INVPARLEN(194),MISSINGOPTPARAM(195),INVOPTPARAMVAL(196),DELIVERYFAILURE(254),UNKNOWNERR(255),SERTYPUNAUTH(256),PROHIBITED(257),SERTYPUNAVAIL(258),SERTYPDENIED(259),INVDCS(260),INVSRCADDRSUBUNIT(261),INVDSTADDRSUBUNIT(262),INVBCASTFREQINT(263),INVBCASTALIAS_NAME(264),INVBCASTAREAFMT(265),INVNUMBCAST_AREAS(266),INVBCASTCNTTYPE(267),INVBCASTMSGCLASS(268);
    
	private static final Map<Integer, MessageStatus> intToTypeMap = new HashMap<Integer, MessageStatus>();
	static 
	{
	    for (MessageStatus type : MessageStatus.values()) 
	    {
	        intToTypeMap.put(type.value, type);
	    }
	}

	public static MessageStatus fromInt(int i) 
	{
		MessageStatus type = intToTypeMap.get(Integer.valueOf(i));
	    if (type == null) 
	        return MessageStatus.OK;
	    
	    return type;
	}
	
	private int value;
	
	private MessageStatus(int value)
	{
		this.value=value;
	}
	
	public int getValue()
	{
		return value;
	}
}
