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

import com.mobius.software.common.dal.timers.PeriodicQueuedTasks;
import com.mobius.software.common.dal.timers.Timer;
import com.mobius.software.protocols.smpp.channel.SmppSessionConfiguration;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class ClientChannelConnectListener implements ChannelFutureListener 
{  
	public static Logger logger=LogManager.getLogger(ClientChannelConnectListener.class);
	
	private SmppClient smppClient;  
	private SmppSessionConfiguration configuration;
	
	private PeriodicQueuedTasks<Timer> timersQueue;
	   
	public ClientChannelConnectListener(PeriodicQueuedTasks<Timer> timersQueue,SmppClient client,SmppSessionConfiguration configuration) 
	{  
		this.timersQueue=timersQueue;
		this.smppClient = client;  
		this.configuration=configuration;
	}
	   
	@Override
	public void operationComplete(ChannelFuture channelFuture) throws Exception 
	{  
		if (!channelFuture.isSuccess()) 
		{  
			logger.error("An error occured while connecting to remote address:" + configuration.getHost() + ":" + configuration.getPort());
			ReconnectionTimer timer=new ReconnectionTimer(channelFuture.channel(), "SmppConnectListenerReconectionTimer");
			timersQueue.store(timer.getRealTimestamp(), timer);			 
		}   
	}
	   
	public class ReconnectionTimer implements Timer
	{
		private Channel channel;
		private Long realTimestamp=System.currentTimeMillis();
		private String taskName;
		
		public ReconnectionTimer(Channel channel, String taskName)
		{
			this.channel=channel;
			this.taskName=taskName;
		}
		
		@Override
		public void execute() 
		{
			if(channel!=null && channel.isOpen())
				channel.close();
			
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

		@Override
		public String printTaskDetails()
		{
			return "Task name: " + taskName;
		}		   
	}
}
