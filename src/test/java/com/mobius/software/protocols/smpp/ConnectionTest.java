package com.mobius.software.protocols.smpp;
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
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.bson.types.ObjectId;
import org.junit.Test;

import com.mobius.software.protocols.smpp.exceptions.SmppChannelException;

public class ConnectionTest extends TestsBase 
{
	@Test
	public void testConnection() throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		ConcurrentHashMap<String, Semaphore> semaphores=new ConcurrentHashMap<String, Semaphore>();
		String connectionID=(new ObjectId()).toHexString();
		String username="username1";
		String password="01020304";
		semaphores.put(connectionID,new Semaphore(0));
		LocalConnectionListener connectionListener=new LocalConnectionListener(semaphores, semaphores, null, null, null, null, null);
		setConnectionListener(connectionListener);
		startClient(connectionID, false, false, true, 2, 2, username, password);
		
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		assertEquals(getUsedClients(connectionID),new Integer(2));
		
		//waiting for bind completed on client side
		try
		{
			Thread.sleep(1000);
		}
		catch(Exception ex)
		{
			
		}
		
		stopClient(connectionID);
				
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		assertEquals(getUsedClients(connectionID),new Integer(0));		
		startClient(connectionID, false, false, true, 2, 3, username, password);
		
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		//lets wait to validate that third channel not established
		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		assertEquals(getUsedClients(connectionID),new Integer(2));
		
		stopServers();
		
		//waiting for everything to stop
		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException ex)
		{
					
		}
				
		assertEquals(getUsedClients(connectionID),new Integer(0));		
		
		semaphores.get(connectionID).drainPermits();
		startServer();
		
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)((bindTimeout+connectTimeout)*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		//lets wait to validate that third channel not established
		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException ex)
		{
					
		}
				
		assertEquals(getUsedClients(connectionID),new Integer(2));			
		resetConnectionListener();
		stopClient(connectionID);
	}		
	
	@Test
	public void testEnquiryLink() throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		ConcurrentHashMap<String, Semaphore> semaphores=new ConcurrentHashMap<String, Semaphore>();
		ConcurrentHashMap<String, Semaphore> heartbeatSemaphores=new ConcurrentHashMap<String, Semaphore>();
		String connectionID=(new ObjectId()).toHexString();
		String username="username1";
		String password="01020304";
		semaphores.put(connectionID,new Semaphore(0));
		heartbeatSemaphores.put(connectionID, new Semaphore(0));
		
		LocalConnectionListener connectionListener=new LocalConnectionListener(semaphores, semaphores, heartbeatSemaphores, null, null, null, null);
		setConnectionListener(connectionListener);
		startClient(connectionID, false, false, true, 2, 2, username, password);
		
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		assertEquals(getUsedClients(connectionID),new Integer(2));
		
		//waiting for bind completed on client side
		try
		{
			Thread.sleep(1000);
		}
		catch(Exception ex)
		{
			
		}
		
		try
		{
			heartbeatSemaphores.get(connectionID).tryAcquire(10, (long)(enquiryLinkTimeout*5.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		assertEquals(new Integer(10), getHeartbeats(connectionID));
		resetConnectionListener();
		stopClient(connectionID);
	}
	
	@Test
	public void testServerEnquiryHandling() throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		ConcurrentHashMap<String, Semaphore> semaphores=new ConcurrentHashMap<String, Semaphore>();
		String connectionID=(new ObjectId()).toHexString();
		String username="username1";
		String password="01020304";
		semaphores.put(connectionID,new Semaphore(0));
		LocalConnectionListener connectionListener=new LocalConnectionListener(semaphores, semaphores, null, null, null, null, null);
		setConnectionListener(connectionListener);
		startClient(connectionID, false, false, false, 2, 2, username, password);
		
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		assertEquals(getUsedClients(connectionID),new Integer(2));
		
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)(enquiryLinkTimeout*3+1000), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		assertEquals(getUsedClients(connectionID),new Integer(0));
		
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		assertEquals(getUsedClients(connectionID),new Integer(2));
		resetConnectionListener();
		stopClient(connectionID);
	}
	
	@Test
	public void testTLSConnection() throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		ConcurrentHashMap<String, Semaphore> semaphores=new ConcurrentHashMap<String, Semaphore>();
		String connectionID=(new ObjectId()).toHexString();
		String username="username1";
		String password="01020304";
		semaphores.put(connectionID,new Semaphore(0));
		LocalConnectionListener connectionListener=new LocalConnectionListener(semaphores, semaphores, null, null, null, null, null);
		setConnectionListener(connectionListener);
		startClient(connectionID, true, false, true, 2, 2, username, password);
		
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		assertEquals(getUsedClients(connectionID),new Integer(2));
		
		//waiting for bind completed on client side
		try
		{
			Thread.sleep(1000);
		}
		catch(Exception ex)
		{
			
		}
		
		stopClient(connectionID);
				
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		assertEquals(getUsedClients(connectionID),new Integer(0));		
		startClient(connectionID, true, false, true, 2, 3, username, password);
		
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		//lets wait to validate that third channel not established
		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		assertEquals(getUsedClients(connectionID),new Integer(2));
		
		stopServers();
		
		//waiting for everything to stop
		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException ex)
		{
					
		}
				
		assertEquals(getUsedClients(connectionID),new Integer(0));		
		
		semaphores.get(connectionID).drainPermits();
		startServer();
		
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)((bindTimeout+connectTimeout)*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		//lets wait to validate that third channel not established
		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException ex)
		{
					
		}
				
		assertEquals(getUsedClients(connectionID),new Integer(2));			
		resetConnectionListener();
		stopClient(connectionID);
	}
	
	@Test
	public void testTLSConnectionWithKeystore() throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		ConcurrentHashMap<String, Semaphore> semaphores=new ConcurrentHashMap<String, Semaphore>();
		String connectionID=(new ObjectId()).toHexString();
		String username="username1";
		String password="01020304";
		semaphores.put(connectionID,new Semaphore(0));
		LocalConnectionListener connectionListener=new LocalConnectionListener(semaphores, semaphores, null, null, null, null, null);
		setConnectionListener(connectionListener);
		startClient(connectionID, true, true, true, 2, 2, username, password);
		
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		assertEquals(getUsedClients(connectionID),new Integer(2));
		
		//waiting for bind completed on client side
		try
		{
			Thread.sleep(1000);
		}
		catch(Exception ex)
		{
			
		}
		
		stopClient(connectionID);
				
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		assertEquals(getUsedClients(connectionID),new Integer(0));		
		startClient(connectionID, true, true, true, 2, 3, username, password);
		
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		//lets wait to validate that third channel not established
		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		assertEquals(getUsedClients(connectionID),new Integer(2));
		
		stopServers();
		
		//waiting for everything to stop
		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException ex)
		{
					
		}
				
		assertEquals(getUsedClients(connectionID),new Integer(0));		
		
		semaphores.get(connectionID).drainPermits();
		startServer();
		
		try
		{
			semaphores.get(connectionID).tryAcquire(2, (long)((bindTimeout+connectTimeout)*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		//lets wait to validate that third channel not established
		try
		{
			Thread.sleep(1000);
		}
		catch(InterruptedException ex)
		{
					
		}
				
		assertEquals(getUsedClients(connectionID),new Integer(2));			
		resetConnectionListener();
		stopClient(connectionID);
	}
}
