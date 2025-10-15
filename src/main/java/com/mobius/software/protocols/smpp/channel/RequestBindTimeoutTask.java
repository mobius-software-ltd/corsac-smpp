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
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mobius.software.common.dal.timers.TaskCallback;
import com.mobius.software.protocols.smpp.PduRequest;

public class RequestBindTimeoutTask implements RequestTimeoutInterface
{
	public static final Logger logger = LogManager.getLogger(RequestBindTimeoutTask.class);

	private long startTime;
	private AtomicLong timestamp;
	private SmppSessionImpl session;
	@SuppressWarnings("rawtypes")
	private PduRequest bindRequest;
	private String taskName;

	@SuppressWarnings("rawtypes")
	public RequestBindTimeoutTask(SmppSessionImpl session, PduRequest bindRequest, long timeout, String taskName)
	{
		this.session = session;
		this.bindRequest = bindRequest;
		this.startTime = System.currentTimeMillis();
		this.timestamp = new AtomicLong(System.currentTimeMillis() + timeout);
		this.taskName = taskName;
	}

	@SuppressWarnings("rawtypes")
	public PduRequest getRequest()
	{
		return this.bindRequest;
	}

	@Override
	public void execute()
	{
		if (timestamp.get() < Long.MAX_VALUE)
		{
			session.expired(bindRequest);

			session.sendRequest(bindRequest, timestamp.get() - startTime, session.getId(), new TaskCallback<Exception>()
			{
				@Override
				public void onSuccess()
				{
				}

				@Override
				public void onError(Exception exception)
				{
					logger.warn("An exception occured while sending bind request: " + exception);
				}
			});
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
