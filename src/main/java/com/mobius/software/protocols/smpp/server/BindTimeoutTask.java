package com.mobius.software.protocols.smpp.server;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mobius.software.common.dal.timers.Timer;

/* Copyright 2019(C) Mobius Software LTD - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Yulian Oifa <yulian.oifa@mobius-software.com>
 */
import io.netty.channel.Channel;

public class BindTimeoutTask implements Timer
{
	public static Logger logger=LogManager.getLogger(BindTimeoutTask.class);
	
	private long startTime;
	private AtomicLong timestamp;
	private Channel channel;
	private Long timeout;
	private String channelName;
	private String taskName;
	
	public BindTimeoutTask(Channel channel,String channelName, long timeout, String taskName)
	{
		this.channel=channel;
		this.channelName=channelName;
		this.timeout=timeout;
		this.startTime=System.currentTimeMillis();
		this.timestamp = new AtomicLong(System.currentTimeMillis() + timeout);
		this.taskName=taskName;
	}
	
	@Override
	public void execute() 
	{
		if(timestamp.get()<Long.MAX_VALUE)
		{
			logger.warn("Channel not bound within [" + timeout + "] ms, closing connection [{" + channelName + "}]");
			channel.close();
		}
	}

	@Override
	public long getStartTime() 
	{
		return startTime;
	}

	@Override
	public Long getRealTimestamp() 
	{
		return timestamp.get();
	}

	@Override
	public void stop() 
	{
		timestamp.set(Long.MAX_VALUE);
	}

	@Override
	public String printTaskDetails()
	{
		return "Task name: " + taskName;
	}	
}
