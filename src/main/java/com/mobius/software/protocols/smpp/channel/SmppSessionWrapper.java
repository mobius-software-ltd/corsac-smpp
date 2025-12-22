package com.mobius.software.protocols.smpp.channel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import com.mobius.software.protocols.smpp.Pdu;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class SmppSessionWrapper extends SimpleChannelInboundHandler<Pdu> 
{
	public static final String NAME = "smppSessionWrapper";
	public static final Logger logger = LogManager.getLogger(SmppSessionWrapper.class);
    
	private SmppSessionChannelListener listener;

	public SmppSessionWrapper(SmppSessionChannelListener listener)
	{
		this.listener = listener;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Pdu msg) throws Exception 
	{
		this.listener.firePduReceived(msg);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception 
	{
		this.listener.fireExceptionThrown(cause);
	}
	 
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception 
	{
		logger.error("Received channel inactive in session wrapper");
		this.listener.fireChannelClosed();
	}
}