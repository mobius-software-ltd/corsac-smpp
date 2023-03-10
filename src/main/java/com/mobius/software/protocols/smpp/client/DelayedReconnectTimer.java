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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mobius.software.common.dal.timers.Timer;
import com.mobius.software.protocols.smpp.channel.SmppConnectionConfiguration;

public class DelayedReconnectTimer implements Timer
{
	public static Logger logger=LogManager.getLogger(ClientChannelConnectListener.class);
	
	private Long realTimestamp=System.currentTimeMillis();
	private SmppConnectionConfiguration configuration;
	private SmppClient smppClient;
	
	public DelayedReconnectTimer(SmppClient smppClient,SmppConnectionConfiguration configuration)
	{
		this.smppClient=smppClient;
		this.configuration=configuration;
	}
	
	@Override
	public void execute() 
	{
		logger.error("Retrying connection to remote address:" + configuration.getHost() + ":" + configuration.getPort());		
		smppClient.startChannel(null);  
	}

	@Override
	public long getStartTime() 
	{			
		return realTimestamp;
	}

	@Override
	public Long getRealTimestamp() 
	{
		return realTimestamp + configuration.getConnectTimeout();
	}

	@Override
	public void stop() 
	{
		//NOT USED
	}		   
}