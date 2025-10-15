package com.mobius.software.protocols.smpp.client;
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
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mobius.software.common.dal.timers.Timer;
import com.mobius.software.protocols.smpp.channel.SmppSession;

public class HealthCheckTimer implements Timer 
{
	public static Logger logger=LogManager.getLogger(HealthCheckTimer.class);
	
	private long startTime;
	private AtomicLong timestamp;
	private long timeout;
	private SmppSession session;
	private String taskName;
	
	public HealthCheckTimer(SmppSession session,long timeout, String taskName)
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

	@Override
	public String printTaskDetails()
	{
		return "Task name: " + taskName;
	}
}
