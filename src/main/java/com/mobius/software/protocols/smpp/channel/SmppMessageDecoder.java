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
import java.util.List;

import com.mobius.software.protocols.smpp.Pdu;
import com.mobius.software.protocols.smpp.PduTranscoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class SmppMessageDecoder extends ByteToMessageDecoder
{
	public static final String NAME = "smppSessionPduDecoder";
    
	private final PduTranscoder transcoder;

    public SmppMessageDecoder(PduTranscoder transcoder)
    {
    	this.transcoder=transcoder;
    }
    
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,List<Object> out) throws Exception 
	{
		Pdu pdu=null;
		do
		{
			pdu=transcoder.decode(in);
			if(pdu!=null)
				out.add(pdu);
		}
		while(pdu!=null);
	}
}