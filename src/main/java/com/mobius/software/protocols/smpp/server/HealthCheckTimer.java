package com.mobius.software.protocols.smpp.server;
/* Copyright 2019(C) Mobius Software LTD - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Yulian Oifa <yulian.oifa@mobius-software.com>
 */
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mobius.software.common.dal.timers.Timer;
import com.mobius.software.protocols.smpp.channel.SmppServerSession;

public class HealthCheckTimer implements Timer 
{
	public static Logger logger=LogManager.getLogger(HealthCheckTimer.class);
	
	private long startTime;
	private AtomicLong timestamp;
	private long timeout;
	private SmppServerSession session;
	
	public HealthCheckTimer(SmppServerSession session,long timeout)
	{
		this.startTime=System.currentTimeMillis();
		this.session=session;
		this.timeout=timeout;
		this.timestamp = new AtomicLong(System.currentTimeMillis() + timeout*3);
	}

	@Override
	public void execute() 
	{
		if(timestamp.get()<Long.MAX_VALUE)
		{
			logger.error("Closing session:" + session.getConfiguration().getName() + " due to inactivity");
			session.passiveClose();
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
	
	public void restart()
	{
		this.timestamp.set(System.currentTimeMillis() + timeout*3);
	}
}
