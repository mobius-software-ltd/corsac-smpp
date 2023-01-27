package com.mobius.software.protocols.smpp;
import java.util.List;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class LocalConnectionListener implements ConnectionListener
{
	private ConcurrentHashMap<String,Semaphore> connectSemaphores;
	private ConcurrentHashMap<String,Semaphore> disconnectSemaphores;
	private ConcurrentHashMap<String,Semaphore> heartbeatsSemaphores;
	private ConcurrentHashMap<String,Semaphore> messagesSemaphores;
	private ConcurrentHashMap<String,Semaphore> statusesSemaphores;
	private ConcurrentHashMap<String,Semaphore> responsesSemaphores;
	private ConcurrentHashMap<String,Semaphore> timeoutsSemaphores;
	
	public LocalConnectionListener(ConcurrentHashMap<String,Semaphore> connectSemaphores,ConcurrentHashMap<String,Semaphore> disconnectSemaphores,ConcurrentHashMap<String,Semaphore> heartbeatSemaphores,ConcurrentHashMap<String,Semaphore> messagesSemaphores,ConcurrentHashMap<String,Semaphore> statusesSemaphores,ConcurrentHashMap<String,Semaphore> responsesSemaphores,ConcurrentHashMap<String,Semaphore> timeoutsSemaphores)
	{
		this.connectSemaphores=connectSemaphores;
		this.disconnectSemaphores=disconnectSemaphores;
		this.heartbeatsSemaphores=heartbeatSemaphores;
		this.messagesSemaphores=messagesSemaphores;
		this.statusesSemaphores=statusesSemaphores;
		this.responsesSemaphores=responsesSemaphores;
		this.timeoutsSemaphores=timeoutsSemaphores;
	}
	
	@Override
	public void connected(String uniqueID) 
	{
		if(connectSemaphores!=null)
		{
			Semaphore current=connectSemaphores.get(uniqueID);
			if(current!=null)
				current.release();
		}
	}

	@Override
	public void disconnected(String uniqueID) 
	{
		if(disconnectSemaphores!=null)
		{
			Semaphore current=disconnectSemaphores.get(uniqueID);
			if(current!=null)
				current.release();
		}
	}

	@Override
	public void heartbeatReceived(String uniqueID) 
	{
		if(heartbeatsSemaphores!=null)
		{
			Semaphore current=heartbeatsSemaphores.get(uniqueID);
			if(current!=null)
				current.release();
		}
	}

	@Override
	public void messageReceived(String uniqueID,List<String> to, byte[] data, byte[] udh, ReportData reportData, AsyncCallback<RequestProcessingResult> callback) 
	{
		if(messagesSemaphores!=null)
		{
			Semaphore current=messagesSemaphores.get(uniqueID);
			if(current!=null)
				current.release();
		}				
	}

	@Override
	public void statusReceived(String uniqueID, String messageID, AsyncCallback<DeliveryProcessingResult> callback) 
	{
		if(statusesSemaphores!=null)
		{
			Semaphore current=statusesSemaphores.get(uniqueID);
			if(current!=null)
				current.release();
		}
	}

	@Override
	public void responseReceived(String uniqueID, String messageID, String remoteMessageID, MessageStatus status) 
	{
		if(responsesSemaphores!=null)
		{
			Semaphore current=responsesSemaphores.get(uniqueID);
			if(current!=null)
				current.release();
		}
	}

	@Override
	public void timeoutReceived(String uniqueID, String messageID) 
	{
		if(timeoutsSemaphores!=null)
		{
			Semaphore current=timeoutsSemaphores.get(uniqueID);
			if(current!=null)
				current.release();
		}
	}
}