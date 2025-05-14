package com.mobius.software.protocols.smpp.server;
/* Copyright 2019(C) Mobius Software LTD - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Yulian Oifa <yulian.oifa@mobius-software.com>
 */
import javax.net.ssl.SSLEngine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mobius.software.common.dal.timers.CountableQueue;
import com.mobius.software.common.dal.timers.PeriodicQueuedTasks;
import com.mobius.software.common.dal.timers.Task;
import com.mobius.software.common.dal.timers.Timer;
import com.mobius.software.protocols.smpp.channel.ChannelUtil;
import com.mobius.software.protocols.smpp.channel.SmppMessageDecoder;
import com.mobius.software.protocols.smpp.channel.SmppSessionWrapper;
import com.mobius.software.protocols.smpp.channel.SslConfiguration;
import com.mobius.software.protocols.smpp.channel.SslContextFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;

@Sharable
public class SmppServerConnector extends ChannelInboundHandlerAdapter
{
	public static Logger logger=LogManager.getLogger(SmppServerConnector.class);
	
	private SmppServer server;
	private CountableQueue<Task> mainQueue;
	private PeriodicQueuedTasks<Timer> timersQueue;
	
	public SmppServerConnector(SmppServer server, CountableQueue<Task> mainQueue, PeriodicQueuedTasks<Timer> timersQueue)
	{
		this.server = server;
		this.mainQueue = mainQueue;
		this.timersQueue = timersQueue;
	}
	
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception 
    {
		Channel channel=ctx.channel();
		logger.info("Channel connected from:" + channel.remoteAddress());
		
		if (server.getConfiguration().isUseSsl()) 
		{
		    SslConfiguration sslConfig = server.getConfiguration().getSslConfiguration();
		    if (sslConfig == null) throw new IllegalStateException("sslConfiguration must be set");
		    SslContextFactory factory = new SslContextFactory(sslConfig);
		    SSLEngine sslEngine = factory.newSslEngine();
		    sslEngine.setUseClientMode(false);
		    channel.pipeline().addLast("SSL", new SslHandler(sslEngine));
		}

		channel.pipeline().addLast(SmppMessageDecoder.NAME, new SmppMessageDecoder(server.getTranscoder()));

		String channelName = ChannelUtil.createChannelName(channel);
        UnboundSmppSession session = new UnboundSmppSession(channelName, channel, server, timersQueue);
		channel.pipeline().addLast(SmppSessionWrapper.NAME, new SmppSessionWrapper(session, this.mainQueue));
	}		
}
