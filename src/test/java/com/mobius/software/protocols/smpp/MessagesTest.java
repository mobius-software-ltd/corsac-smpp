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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.bson.types.ObjectId;
import org.junit.Test;

import com.mobius.software.protocols.smpp.channel.EsmClass;
import com.mobius.software.protocols.smpp.exceptions.SmppChannelException;
import com.mobius.software.protocols.smpp.exceptions.SmppInvalidArgumentException;

public class MessagesTest extends TestsBase 
{
	@Test
	public void testNonTlsMessaging() throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		ConcurrentHashMap<String, Semaphore> connectSemaphores=new ConcurrentHashMap<String, Semaphore>();
		ConcurrentHashMap<String, Semaphore> messagesSemaphores=new ConcurrentHashMap<String, Semaphore>();
		
		String connectionID1=(new ObjectId()).toHexString();
		String username1="username1";
		String password1="01020304";
		connectSemaphores.put(connectionID1,new Semaphore(0));
		messagesSemaphores.put(connectionID1,new Semaphore(0));
		String connectionID2=(new ObjectId()).toHexString();
		String username2="username2";
		String password2="01020305";
		connectSemaphores.put(connectionID2,new Semaphore(0));
		messagesSemaphores.put(connectionID2,new Semaphore(0));
		
		LocalConnectionListener connectionListener=new LocalConnectionListener(connectSemaphores, connectSemaphores, null, messagesSemaphores, messagesSemaphores, messagesSemaphores, null);
		setConnectionListener(connectionListener);
		
		startClient(connectionID1, false, false, true, 2, 2, username1, password1);
		startClient(connectionID2, false, false, true, 2, 2, username2, password2);
		
		try
		{
			connectSemaphores.get(connectionID1).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		assertEquals(getUsedClients(connectionID1),new Integer(2));
		
		try
		{
			connectSemaphores.get(connectionID2).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		assertEquals(getUsedClients(connectionID2),new Integer(2));
		Long expirationDate=System.currentTimeMillis()+24*60*60*1000;
		expirationDate=expirationDate-expirationDate%100;
		
		List<String> messageIDs=new ArrayList<String>();
		List<byte[]> data=new ArrayList<byte[]>();
		String[] remoteMessageIDs=new String[10];
		ReportData[] reportData=new ReportData[10];
		for(int i=0;i<10;i++)
		{
			messageIDs.add(new ObjectId().toHexString());
			data.add(("hello world " + i).getBytes());
		}
		
		SubmitSm message1=generateSubmitSm(connectionID1, messageIDs.get(0), data.get(0), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message2=generateSubmitSm(connectionID2, messageIDs.get(1), data.get(1), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.INTERNET, TypeOfNetwork.NATIONAL, "010205", NumberPlan.INTERNET, TypeOfNetwork.NATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message3=generateSubmitSm(connectionID1, messageIDs.get(2), data.get(2), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.UNKNOWN, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message4=generateSubmitSm(connectionID2, messageIDs.get(3), data.get(3), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.E164, TypeOfNetwork.UNKNOWN, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message5=generateSubmitSm(connectionID1, messageIDs.get(4), data.get(4), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.UNKNOWN, TypeOfNetwork.UNKNOWN, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message6=generateSubmitSm(connectionID2, messageIDs.get(5), data.get(5), null, Encoding.CYRLLIC, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message7=generateSubmitSm(connectionID1, messageIDs.get(6), data.get(6), null, Encoding.DEFAULT, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message8=generateSubmitSm(connectionID2, messageIDs.get(7), data.get(7), null, Encoding.LATIN_HEBREW, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message9=generateSubmitSm(connectionID1, messageIDs.get(8), data.get(8), null, Encoding.UTF_16, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message10=generateSubmitSm(connectionID2, messageIDs.get(9), data.get(9), new byte[] { 0x02 , 0x01}, Encoding.ISO_8859_1, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
				
		List<SubmitSm> allMessages=Arrays.asList(new SubmitSm[] { message1,message2,message3,message4,message5,message6,message7,message8,message9,message10});
		List<MessageStatus> statuses=Arrays.asList(new MessageStatus[] { MessageStatus.OK, MessageStatus.INVCMDID, MessageStatus.CNTSUBDL, MessageStatus.OK, MessageStatus.INVEXPIRY, MessageStatus.OK, MessageStatus.OK, MessageStatus.INVDSTTON, MessageStatus.INVDCS, MessageStatus.INVNUMDESTS});
		List<DeliveryStatus> delivery=Arrays.asList(new DeliveryStatus[] { DeliveryStatus.DELIVERED, DeliveryStatus.ENROUTE, DeliveryStatus.DELETED, DeliveryStatus.DELIVERED, DeliveryStatus.REJECTED, DeliveryStatus.DELIVERED, DeliveryStatus.DELIVERED, DeliveryStatus.EXPIRED, DeliveryStatus.DELIVERED, DeliveryStatus.DELIVERED});
		List<Integer> errorCodes=Arrays.asList(new Integer[] { 0,0, 25, 0,0, 12, 1,2, 0,0});
		
		//waiting for bind completed on client side
		try
		{
			Thread.sleep(1000);
		}
		catch(Exception ex)
		{
					
		}
				
		for(int i=0;i<5;i++)
		{
			sendMessage(connectionID1, messageIDs.get(i*2), allMessages.get(i*2));
			
			try
			{
				messagesSemaphores.get(connectionID1).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			ConcurrentLinkedQueue<MessageRequestWrapper> requests=getRequests(connectionID1);
			MessageRequestWrapper currRequest=requests.poll();
			assertNotNull(currRequest);
			remoteMessageIDs[i*2]=new ObjectId().toHexString();
			assertArrayEquals(data.get(i*2),currRequest.getData());
			RequestProcessingResult result=new RequestProcessingResult(Arrays.asList(new String[] { remoteMessageIDs[i*2] }), statuses.get(i*2));
			currRequest.getResponse().onResult(result, null);
			
			try
			{
				messagesSemaphores.get(connectionID1).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			ConcurrentLinkedQueue<ResponseWrapper> responses=getResponses(connectionID1);
			ResponseWrapper currResponseWrapper=responses.poll();
			assertNotNull(currResponseWrapper);
			assertEquals(currResponseWrapper.getOriginalMessageID(), messageIDs.get(i*2));
			assertEquals(currResponseWrapper.getRemoteMessageID(), remoteMessageIDs[i*2]);
			assertEquals(currResponseWrapper.getStatus(), statuses.get(i*2));
			
			sendMessage(connectionID2, messageIDs.get(i*2+1), allMessages.get(i*2+1));
			
			try
			{
				messagesSemaphores.get(connectionID2).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			requests=getRequests(connectionID2);
			currRequest=requests.poll();
			assertNotNull(currRequest);
			assertArrayEquals(data.get(i*2+1),currRequest.getData());
			remoteMessageIDs[i*2+1]=new ObjectId().toHexString();
			result=new RequestProcessingResult(Arrays.asList(new String[] { remoteMessageIDs[i*2+1] }), statuses.get(i*2+1));
			currRequest.getResponse().onResult(result, null);
			
			try
			{
				messagesSemaphores.get(connectionID2).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			responses=getResponses(connectionID2);
			currResponseWrapper=responses.poll();
			assertNotNull(currResponseWrapper);
			assertEquals(currResponseWrapper.getOriginalMessageID(), messageIDs.get(i*2+1));
			assertEquals(currResponseWrapper.getRemoteMessageID(), remoteMessageIDs[i*2+1]);
			assertEquals(currResponseWrapper.getStatus(), statuses.get(i*2+1));
						
			//send delivery to each client for all messages , handle delivery on server side + send response on delivery , validate response on client
			Date date=new Date();
			date.setTime(System.currentTimeMillis());
			String deliveryText=SmppHelper.createDeliveryReport(remoteMessageIDs[i*2], date, date, errorCodes.get(i*2), "Message Delivered", delivery.get(i*2));
			reportData[i*2]=new ReportData(remoteMessageIDs[i*2], errorCodes.get(i*2), deliveryText.getBytes(), Encoding.OCTET_UNSPECIFIED_2, delivery.get(i*2));
			
			sendDelivery(connectionID1, remoteMessageIDs[i*2], generateDeliverSm(connectionID1, remoteMessageIDs[i*2], "Message Delivered".getBytes(),null, errorCodes.get(i*2), delivery.get(i*2), Encoding.OCTET_UNSPECIFIED_2, allMessages.get(i*2).getSourceAddress().getAddress(), NumberPlan.fromInt(allMessages.get(i*2).getSourceAddress().getNpi()),TypeOfNetwork.fromInt(allMessages.get(i*2).getSourceAddress().getTon()), allMessages.get(i*2).getDestAddress().getAddress(), NumberPlan.fromInt(allMessages.get(i*2).getDestAddress().getNpi()), TypeOfNetwork.fromInt(allMessages.get(i*2).getDestAddress().getTon()) , connectionListener));
			
			try
			{
				messagesSemaphores.get(connectionID1).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			requests=getRequests(connectionID1);
			currRequest=requests.poll();
			assertNotNull(currRequest.getReportData());
			assertEquals(currRequest.getReportData().getMessageID(),remoteMessageIDs[i*2]);
			assertEquals(currRequest.getReportData().getErrorCode(),errorCodes.get(i*2));
			assertTrue(Arrays.equals(currRequest.getReportData().getData(),"Message Delivered".getBytes()));
			assertEquals(currRequest.getReportData().getEncoding(),Encoding.OCTET_UNSPECIFIED_2);
			assertEquals(currRequest.getReportData().getDeliveryStatus(),delivery.get(i*2));
			
			result=new RequestProcessingResult(Arrays.asList(new String[] { remoteMessageIDs[i*2] }), statuses.get(i*2));
			currRequest.getResponse().onResult(result, null);
			
			try
			{
				messagesSemaphores.get(connectionID1).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{				
			}
			
			responses=getResponses(connectionID1);
			currResponseWrapper=responses.poll();
			assertNotNull(currResponseWrapper);
			assertEquals(currResponseWrapper.getOriginalMessageID(), remoteMessageIDs[i*2]);
			assertNull(currResponseWrapper.getRemoteMessageID());
			assertEquals(currResponseWrapper.getStatus(), statuses.get(i*2));
			
			date=new Date();
			date.setTime(System.currentTimeMillis());
			deliveryText=SmppHelper.createDeliveryReport(remoteMessageIDs[i*2+1], date, date, errorCodes.get(i*2+1), "Message Delivered", delivery.get(i*2+1));
			reportData[i*2+1]=new ReportData(remoteMessageIDs[i*2+1], errorCodes.get(i*2+1), deliveryText.getBytes(), Encoding.OCTET_UNSPECIFIED_2, delivery.get(i*2+1));
			sendDelivery(connectionID2, remoteMessageIDs[i*2+1], generateDeliverSm(connectionID2, remoteMessageIDs[i*2+1], "Message Delivered".getBytes(),null, errorCodes.get(i*2+1), delivery.get(i*2+1), Encoding.OCTET_UNSPECIFIED_2, allMessages.get(i*2+1).getSourceAddress().getAddress(), NumberPlan.fromInt(allMessages.get(i*2+1).getSourceAddress().getNpi()),TypeOfNetwork.fromInt(allMessages.get(i*2+1).getSourceAddress().getTon()), allMessages.get(i*2+1).getDestAddress().getAddress(), NumberPlan.fromInt(allMessages.get(i*2+1).getDestAddress().getNpi()), TypeOfNetwork.fromInt(allMessages.get(i*2+1).getDestAddress().getTon()) , connectionListener));
			
			try
			{
				messagesSemaphores.get(connectionID2).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			requests=getRequests(connectionID2);
			currRequest=requests.poll();
			assertNotNull(currRequest.getReportData());
			assertEquals(currRequest.getReportData().getMessageID(),remoteMessageIDs[i*2+1]);
			assertEquals(currRequest.getReportData().getErrorCode(),errorCodes.get(i*2+1));
			assertTrue(Arrays.equals(currRequest.getReportData().getData(),"Message Delivered".getBytes()));
			assertEquals(currRequest.getReportData().getEncoding(),Encoding.OCTET_UNSPECIFIED_2);
			assertEquals(currRequest.getReportData().getDeliveryStatus(),delivery.get(i*2+1));
			
			result=new RequestProcessingResult(Arrays.asList(new String[] { remoteMessageIDs[i*2+1] }), statuses.get(i*2+1));
			currRequest.getResponse().onResult(result, null);
			
			try
			{
				messagesSemaphores.get(connectionID2).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			responses=getResponses(connectionID2);
			currResponseWrapper=responses.poll();
			assertNotNull(currResponseWrapper);
			assertEquals(currResponseWrapper.getOriginalMessageID(), remoteMessageIDs[i*2+1]);
			assertNull(currResponseWrapper.getRemoteMessageID());
			assertEquals(currResponseWrapper.getStatus(), statuses.get(i*2+1));			
		}
		
		resetConnectionListener();
		stopClient(connectionID1);
		stopClient(connectionID2);
	}
	
	@Test
	public void testTlsMessaging() throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		ConcurrentHashMap<String, Semaphore> connectSemaphores=new ConcurrentHashMap<String, Semaphore>();
		ConcurrentHashMap<String, Semaphore> messagesSemaphores=new ConcurrentHashMap<String, Semaphore>();
		
		String connectionID1=(new ObjectId()).toHexString();
		String username1="username1";
		String password1="01020304";
		connectSemaphores.put(connectionID1,new Semaphore(0));
		messagesSemaphores.put(connectionID1,new Semaphore(0));
		String connectionID2=(new ObjectId()).toHexString();
		String username2="username2";
		String password2="01020305";
		connectSemaphores.put(connectionID2,new Semaphore(0));
		messagesSemaphores.put(connectionID2,new Semaphore(0));
		
		LocalConnectionListener connectionListener=new LocalConnectionListener(connectSemaphores, connectSemaphores, null, messagesSemaphores, messagesSemaphores, messagesSemaphores, null);
		setConnectionListener(connectionListener);
		
		startClient(connectionID1, true, true, true, 2, 2, username1, password1);
		startClient(connectionID2, true, true, true, 2, 2, username2, password2);
		
		try
		{
			connectSemaphores.get(connectionID1).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		assertEquals(getUsedClients(connectionID1),new Integer(2));
		
		try
		{
			connectSemaphores.get(connectionID2).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		assertEquals(getUsedClients(connectionID2),new Integer(2));
		Long expirationDate=System.currentTimeMillis()+24*60*60*1000;
		expirationDate=expirationDate-expirationDate%100;
		
		List<String> messageIDs=new ArrayList<String>();
		List<byte[]> data=new ArrayList<byte[]>();
		String[] remoteMessageIDs=new String[10];
		ReportData[] reportData=new ReportData[10];
		for(int i=0;i<10;i++)
		{
			messageIDs.add(new ObjectId().toHexString());
			data.add(("hello world " + i).getBytes());
		}
		
		SubmitSm message1=generateSubmitSm(connectionID1, messageIDs.get(0), data.get(0), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message2=generateSubmitSm(connectionID2, messageIDs.get(1), data.get(1), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.INTERNET, TypeOfNetwork.NATIONAL, "010205", NumberPlan.INTERNET, TypeOfNetwork.NATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message3=generateSubmitSm(connectionID1, messageIDs.get(2), data.get(2), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.UNKNOWN, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message4=generateSubmitSm(connectionID2, messageIDs.get(3), data.get(3), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.E164, TypeOfNetwork.UNKNOWN, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message5=generateSubmitSm(connectionID1, messageIDs.get(4), data.get(4), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.UNKNOWN, TypeOfNetwork.UNKNOWN, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message6=generateSubmitSm(connectionID2, messageIDs.get(5), data.get(5), null, Encoding.CYRLLIC, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message7=generateSubmitSm(connectionID1, messageIDs.get(6), data.get(6), null, Encoding.DEFAULT, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message8=generateSubmitSm(connectionID2, messageIDs.get(7), data.get(7), null, Encoding.LATIN_HEBREW, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message9=generateSubmitSm(connectionID1, messageIDs.get(8), data.get(8), null, Encoding.UTF_16, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		SubmitSm message10=generateSubmitSm(connectionID2, messageIDs.get(9), data.get(9), new byte[] { 0x02 , 0x01}, Encoding.ISO_8859_1, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
				
		List<SubmitSm> allMessages=Arrays.asList(new SubmitSm[] { message1,message2,message3,message4,message5,message6,message7,message8,message9,message10});
		List<MessageStatus> statuses=Arrays.asList(new MessageStatus[] { MessageStatus.OK, MessageStatus.INVCMDID, MessageStatus.CNTSUBDL, MessageStatus.OK, MessageStatus.INVEXPIRY, MessageStatus.OK, MessageStatus.OK, MessageStatus.INVDSTTON, MessageStatus.INVDCS, MessageStatus.INVNUMDESTS});
		List<DeliveryStatus> delivery=Arrays.asList(new DeliveryStatus[] { DeliveryStatus.DELIVERED, DeliveryStatus.ENROUTE, DeliveryStatus.DELETED, DeliveryStatus.DELIVERED, DeliveryStatus.REJECTED, DeliveryStatus.DELIVERED, DeliveryStatus.DELIVERED, DeliveryStatus.EXPIRED, DeliveryStatus.DELIVERED, DeliveryStatus.DELIVERED});
		List<Integer> errorCodes=Arrays.asList(new Integer[] { 0,0, 25, 0,0, 12, 1,2, 0,0});
		
		//waiting for bind completed on client side
		try
		{
			Thread.sleep(1000);
		}
		catch(Exception ex)
		{
					
		}
				
		for(int i=0;i<5;i++)
		{
			sendMessage(connectionID1, messageIDs.get(i*2), allMessages.get(i*2));
			
			try
			{
				messagesSemaphores.get(connectionID1).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			ConcurrentLinkedQueue<MessageRequestWrapper> requests=getRequests(connectionID1);
			MessageRequestWrapper currRequest=requests.poll();
			assertNotNull(currRequest);
			remoteMessageIDs[i*2]=new ObjectId().toHexString();
			assertArrayEquals(data.get(i*2),currRequest.getData());
			RequestProcessingResult result=new RequestProcessingResult(Arrays.asList(new String[] { remoteMessageIDs[i*2] }), statuses.get(i*2));
			currRequest.getResponse().onResult(result, null);
			
			try
			{
				messagesSemaphores.get(connectionID1).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			ConcurrentLinkedQueue<ResponseWrapper> responses=getResponses(connectionID1);
			ResponseWrapper currResponseWrapper=responses.poll();
			assertNotNull(currResponseWrapper);
			assertEquals(currResponseWrapper.getOriginalMessageID(), messageIDs.get(i*2));
			assertEquals(currResponseWrapper.getRemoteMessageID(), remoteMessageIDs[i*2]);
			assertEquals(currResponseWrapper.getStatus(), statuses.get(i*2));
			
			sendMessage(connectionID2, messageIDs.get(i*2+1), allMessages.get(i*2+1));
			
			try
			{
				messagesSemaphores.get(connectionID2).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			requests=getRequests(connectionID2);
			currRequest=requests.poll();
			assertNotNull(currRequest);
			assertArrayEquals(data.get(i*2+1),currRequest.getData());
			remoteMessageIDs[i*2+1]=new ObjectId().toHexString();
			result=new RequestProcessingResult(Arrays.asList(new String[] { remoteMessageIDs[i*2+1] }), statuses.get(i*2+1));
			currRequest.getResponse().onResult(result, null);
			
			try
			{
				messagesSemaphores.get(connectionID2).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			responses=getResponses(connectionID2);
			currResponseWrapper=responses.poll();
			assertNotNull(currResponseWrapper);
			assertEquals(currResponseWrapper.getOriginalMessageID(), messageIDs.get(i*2+1));
			assertEquals(currResponseWrapper.getRemoteMessageID(), remoteMessageIDs[i*2+1]);
			assertEquals(currResponseWrapper.getStatus(), statuses.get(i*2+1));
						
			//send delivery to each client for all messages , handle delivery on server side + send response on delivery , validate response on client
			Date date=new Date();
			date.setTime(System.currentTimeMillis());
			String deliveryText=SmppHelper.createDeliveryReport(remoteMessageIDs[i*2], date, date, errorCodes.get(i*2), "Message Delivered", delivery.get(i*2));
			reportData[i*2]=new ReportData(remoteMessageIDs[i*2], errorCodes.get(i*2), deliveryText.getBytes(), Encoding.OCTET_UNSPECIFIED_2, delivery.get(i*2));
			
			sendDelivery(connectionID1, remoteMessageIDs[i*2], generateDeliverSm(connectionID1, remoteMessageIDs[i*2], "Message Delivered".getBytes(),null, errorCodes.get(i*2), delivery.get(i*2), Encoding.OCTET_UNSPECIFIED_2, allMessages.get(i*2).getSourceAddress().getAddress(), NumberPlan.fromInt(allMessages.get(i*2).getSourceAddress().getNpi()),TypeOfNetwork.fromInt(allMessages.get(i*2).getSourceAddress().getTon()), allMessages.get(i*2).getDestAddress().getAddress(), NumberPlan.fromInt(allMessages.get(i*2).getDestAddress().getNpi()), TypeOfNetwork.fromInt(allMessages.get(i*2).getDestAddress().getTon()) , connectionListener));
			
			try
			{
				messagesSemaphores.get(connectionID1).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			requests=getRequests(connectionID1);
			currRequest=requests.poll();
			assertNotNull(currRequest.getReportData());
			assertEquals(currRequest.getReportData().getMessageID(),remoteMessageIDs[i*2]);
			assertEquals(currRequest.getReportData().getErrorCode(),errorCodes.get(i*2));
			assertTrue(Arrays.equals(currRequest.getReportData().getData(),"Message Delivered".getBytes()));
			assertEquals(currRequest.getReportData().getEncoding(),Encoding.OCTET_UNSPECIFIED_2);
			assertEquals(currRequest.getReportData().getDeliveryStatus(),delivery.get(i*2));
			
			result=new RequestProcessingResult(Arrays.asList(new String[] { remoteMessageIDs[i*2] }), statuses.get(i*2));
			currRequest.getResponse().onResult(result, null);
			
			try
			{
				messagesSemaphores.get(connectionID1).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{				
			}
			
			responses=getResponses(connectionID1);
			currResponseWrapper=responses.poll();
			assertNotNull(currResponseWrapper);
			assertEquals(currResponseWrapper.getOriginalMessageID(), remoteMessageIDs[i*2]);
			assertNull(currResponseWrapper.getRemoteMessageID());
			assertEquals(currResponseWrapper.getStatus(), statuses.get(i*2));
			
			date=new Date();
			date.setTime(System.currentTimeMillis());
			deliveryText=SmppHelper.createDeliveryReport(remoteMessageIDs[i*2+1], date, date, errorCodes.get(i*2+1), "Message Delivered", delivery.get(i*2+1));
			reportData[i*2+1]=new ReportData(remoteMessageIDs[i*2+1], errorCodes.get(i*2+1), deliveryText.getBytes(), Encoding.OCTET_UNSPECIFIED_2, delivery.get(i*2+1));
			sendDelivery(connectionID2, remoteMessageIDs[i*2+1], generateDeliverSm(connectionID2, remoteMessageIDs[i*2+1], "Message Delivered".getBytes(),null, errorCodes.get(i*2+1), delivery.get(i*2+1), Encoding.OCTET_UNSPECIFIED_2, allMessages.get(i*2+1).getSourceAddress().getAddress(), NumberPlan.fromInt(allMessages.get(i*2+1).getSourceAddress().getNpi()),TypeOfNetwork.fromInt(allMessages.get(i*2+1).getSourceAddress().getTon()), allMessages.get(i*2+1).getDestAddress().getAddress(), NumberPlan.fromInt(allMessages.get(i*2+1).getDestAddress().getNpi()), TypeOfNetwork.fromInt(allMessages.get(i*2+1).getDestAddress().getTon()) , connectionListener));
			
			try
			{
				messagesSemaphores.get(connectionID2).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			requests=getRequests(connectionID2);
			currRequest=requests.poll();
			assertNotNull(currRequest.getReportData());
			assertEquals(currRequest.getReportData().getMessageID(),remoteMessageIDs[i*2+1]);
			assertEquals(currRequest.getReportData().getErrorCode(),errorCodes.get(i*2+1));
			assertTrue(Arrays.equals(currRequest.getReportData().getData(),"Message Delivered".getBytes()));
			assertEquals(currRequest.getReportData().getEncoding(),Encoding.OCTET_UNSPECIFIED_2);
			assertEquals(currRequest.getReportData().getDeliveryStatus(),delivery.get(i*2+1));
			
			result=new RequestProcessingResult(Arrays.asList(new String[] { remoteMessageIDs[i*2+1] }), statuses.get(i*2+1));
			currRequest.getResponse().onResult(result, null);
			
			try
			{
				messagesSemaphores.get(connectionID2).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
			}
			catch(InterruptedException ex)
			{
				
			}
			
			responses=getResponses(connectionID2);
			currResponseWrapper=responses.poll();
			assertNotNull(currResponseWrapper);
			assertEquals(currResponseWrapper.getOriginalMessageID(), remoteMessageIDs[i*2+1]);
			assertNull(currResponseWrapper.getRemoteMessageID());
			assertEquals(currResponseWrapper.getStatus(), statuses.get(i*2+1));			
		}
		
		resetConnectionListener();
		stopClient(connectionID1);
		stopClient(connectionID2);
	}
	
	@Test
	public void testNonTlsTimeouts() throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		ConcurrentHashMap<String, Semaphore> connectSemaphores=new ConcurrentHashMap<String, Semaphore>();
		ConcurrentHashMap<String, Semaphore> messagesSemaphores=new ConcurrentHashMap<String, Semaphore>();
		
		String connectionID1=(new ObjectId()).toHexString();
		String messageID1=(new ObjectId()).toHexString();
		String username1="username1";
		String password1="01020304";
		connectSemaphores.put(connectionID1,new Semaphore(0));
		messagesSemaphores.put(connectionID1,new Semaphore(0));
		String connectionID2=(new ObjectId()).toHexString();
		String messageID2=(new ObjectId()).toHexString();
		String username2="username2";
		String password2="01020305";
		connectSemaphores.put(connectionID2,new Semaphore(0));
		messagesSemaphores.put(connectionID2,new Semaphore(0));
		
		LocalConnectionListener connectionListener=new LocalConnectionListener(connectSemaphores, connectSemaphores, null, messagesSemaphores, messagesSemaphores, messagesSemaphores, messagesSemaphores);
		setConnectionListener(connectionListener);
		
		startClient(connectionID1, false, false, true, 2, 2, username1, password1);
		startClient(connectionID2, false, false, true, 2, 2, username2, password2);
		
		try
		{
			connectSemaphores.get(connectionID1).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		assertEquals(getUsedClients(connectionID1),new Integer(2));
		
		try
		{
			connectSemaphores.get(connectionID2).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		assertEquals(getUsedClients(connectionID2),new Integer(2));
		
		//waiting for bind completed on client side
		try
		{
			Thread.sleep(1000);
		}
		catch(Exception ex)
		{
							
		}
				
		Long expirationDate=System.currentTimeMillis()+24*60*60*1000;
		expirationDate=expirationDate-expirationDate%100;
		SubmitSm message1=generateSubmitSm(connectionID1, messageID1, "hello world 1".getBytes(), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL,  ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		expirationDate=System.currentTimeMillis()+24*60*60*1000;
		expirationDate=expirationDate-expirationDate%100;
		SubmitSm message2=generateSubmitSm(connectionID2, messageID2, "hello world 2".getBytes(), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.E164, TypeOfNetwork.NATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.NATIONAL,  ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		
		sendMessage(connectionID1, messageID1, message1);
		
		try
		{
			messagesSemaphores.get(connectionID1).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		ConcurrentLinkedQueue<MessageRequestWrapper> requests=getRequests(connectionID1);
		MessageRequestWrapper currRequest=requests.poll();
		assertNotNull(currRequest);
		assertArrayEquals("hello world 1".getBytes(),currRequest.getData());
		
		try
		{
			messagesSemaphores.get(connectionID1).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		ConcurrentLinkedQueue<TimeoutWrapper> timeouts=getTimeouts(connectionID1);
		TimeoutWrapper timeout=timeouts.poll();
		assertNotNull(timeout);
		assertEquals(timeout.getOriginalMessageID(),messageID1);
		
		sendMessage(connectionID2, messageID2, message2);
		
		try
		{
			messagesSemaphores.get(connectionID2).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		requests=getRequests(connectionID2);
		currRequest=requests.poll();
		assertNotNull(currRequest);
		assertArrayEquals("hello world 2".getBytes(),currRequest.getData());
		
		try
		{
			messagesSemaphores.get(connectionID2).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		timeouts=getTimeouts(connectionID2);
		timeout=timeouts.poll();
		assertNotNull(timeout);
		assertEquals(timeout.getOriginalMessageID(),messageID2);
		
		resetConnectionListener();
		stopClient(connectionID1);
		stopClient(connectionID2);
	}
	
	@Test
	public void testTlsTimeouts() throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		ConcurrentHashMap<String, Semaphore> connectSemaphores=new ConcurrentHashMap<String, Semaphore>();
		ConcurrentHashMap<String, Semaphore> messagesSemaphores=new ConcurrentHashMap<String, Semaphore>();
		
		String connectionID1=(new ObjectId()).toHexString();
		String messageID1=(new ObjectId()).toHexString();
		String username1="username1";
		String password1="01020304";
		connectSemaphores.put(connectionID1,new Semaphore(0));
		messagesSemaphores.put(connectionID1,new Semaphore(0));
		String connectionID2=(new ObjectId()).toHexString();
		String messageID2=(new ObjectId()).toHexString();
		String username2="username2";
		String password2="01020305";
		connectSemaphores.put(connectionID2,new Semaphore(0));
		messagesSemaphores.put(connectionID2,new Semaphore(0));
		
		LocalConnectionListener connectionListener=new LocalConnectionListener(connectSemaphores, connectSemaphores, null, messagesSemaphores, messagesSemaphores, messagesSemaphores, messagesSemaphores);
		setConnectionListener(connectionListener);
		
		startClient(connectionID1, true, true, true, 2, 2, username1, password1);
		startClient(connectionID2, true, true, true, 2, 2, username2, password2);
		
		try
		{
			connectSemaphores.get(connectionID1).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		assertEquals(getUsedClients(connectionID1),new Integer(2));
		
		try
		{
			connectSemaphores.get(connectionID2).tryAcquire(2, (long)(bindTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		assertEquals(getUsedClients(connectionID2),new Integer(2));
		
		//waiting for bind completed on client side
		try
		{
			Thread.sleep(1000);
		}
		catch(Exception ex)
		{
							
		}
				
		Long expirationDate=System.currentTimeMillis()+24*60*60*1000;
		expirationDate=expirationDate-expirationDate%100;
		SubmitSm message1=generateSubmitSm(connectionID1, messageID1, "hello world 1".getBytes(), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.INTERNATIONAL,  ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		expirationDate=System.currentTimeMillis()+24*60*60*1000;
		expirationDate=expirationDate-expirationDate%100;
		SubmitSm message2=generateSubmitSm(connectionID2, messageID2, "hello world 2".getBytes(), null, Encoding.OCTET_UNSPECIFIED_2, "010203", NumberPlan.E164, TypeOfNetwork.NATIONAL, "010205", NumberPlan.E164, TypeOfNetwork.NATIONAL,  ReceiptRequested.REQUESTED, IntermediateNotificationRequested.NOT_REQUESTED, SmeAckRequested.NOT_REQUESTED, expirationDate, Priority.NORMAL, connectionListener);
		
		sendMessage(connectionID1, messageID1, message1);
		
		try
		{
			messagesSemaphores.get(connectionID1).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		ConcurrentLinkedQueue<MessageRequestWrapper> requests=getRequests(connectionID1);
		MessageRequestWrapper currRequest=requests.poll();
		assertNotNull(currRequest);
		assertArrayEquals("hello world 1".getBytes(),currRequest.getData());
		
		try
		{
			messagesSemaphores.get(connectionID1).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		ConcurrentLinkedQueue<TimeoutWrapper> timeouts=getTimeouts(connectionID1);
		TimeoutWrapper timeout=timeouts.poll();
		assertNotNull(timeout);
		assertEquals(timeout.getOriginalMessageID(),messageID1);
		
		sendMessage(connectionID2, messageID2, message2);
		
		try
		{
			messagesSemaphores.get(connectionID2).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		requests=getRequests(connectionID2);
		currRequest=requests.poll();
		assertNotNull(currRequest);
		assertArrayEquals("hello world 2".getBytes(),currRequest.getData());
		
		try
		{
			messagesSemaphores.get(connectionID2).tryAcquire(1, (long)(requestTimeout*1.5), TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException ex)
		{
			
		}
		
		timeouts=getTimeouts(connectionID2);
		timeout=timeouts.poll();
		assertNotNull(timeout);
		assertEquals(timeout.getOriginalMessageID(),messageID2);
		
		resetConnectionListener();
		stopClient(connectionID1);
		stopClient(connectionID2);
	}
	
	private SubmitSm generateSubmitSm(String uniqueID, String messageID, byte[] data,byte[] udh, Encoding messageEncoding, String fromDigits,NumberPlan fromNp, TypeOfNetwork fromTon, String toDigits,NumberPlan toNp, TypeOfNetwork toTon, ReceiptRequested rr,IntermediateNotificationRequested inr,SmeAckRequested smeAr,Long expirationDate, Priority priority, ConnectionListener listener)
	{
		SubmitSm submitSm=new SubmitSm();
		
		if(messageEncoding!=null)
			submitSm.setDataCoding((byte)messageEncoding.getValue());
		
		Address fromAddress=null;
		if(fromDigits!=null){
			fromAddress=new Address();
			fromAddress.setAddress(fromDigits);
			
			if(fromNp!=null)
				fromAddress.setNpi((byte)fromNp.getValue());
			
			if(fromTon!=null)
				fromAddress.setTon((byte)fromTon.getValue());
		}			
		
		submitSm.setSourceAddress(fromAddress);
		
		Address toAddress=null;
		if(toDigits!=null){
			toAddress=new Address();
			toAddress.setAddress(toDigits);
			
			if(toNp!=null)
				toAddress.setNpi((byte)toNp.getValue());
			
			if(toTon!=null)
				toAddress.setTon((byte)toTon.getValue());
		}			
		
		submitSm.setDestAddress(toAddress);
		byte registeredDelivery=0;
		if(rr!=null)
			registeredDelivery+=rr.getValue();
		
		if(inr!=null)
			registeredDelivery+=inr.getValue();
		
		if(smeAr!=null)
			registeredDelivery+=smeAr.getValue();
		
		submitSm.setRegisteredDelivery(registeredDelivery);
		
		//it may be in past already and some providers would block it
		//if(message.getTs()!=null)
		//	submitSm.setScheduleDeliveryTime(StringFunctions.printSmppAbsoluteDate(message.getTs(), 0));
				
		if(expirationDate!=null)
			submitSm.setValidityPeriod(SmppHelper.printSmppAbsoluteDate(expirationDate, 0));
		
		if(priority!=null)
			submitSm.setPriority((byte)priority.getValue());
		
		int messageLength=0;
		if(data!=null)
			messageLength+=data.length;
		
		if(udh!=null && udh.length>0)
			messageLength+=udh.length + 1;
		
		byte[] realData=new byte[messageLength];
		int start=0;
		if(udh!=null && udh.length>0)
		{
			realData[0]=(byte)udh.length;
			System.arraycopy(udh, 0, realData, 1, udh.length);
			start += udh.length + 1;
		}
		
		if(data!=null)
			System.arraycopy(data, 0, realData, start, data.length);
		
		try
		{
			submitSm.setShortMessage(realData);
		}
		catch(SmppInvalidArgumentException ex)
		{
			listener.responseReceived(uniqueID, messageID, null, MessageStatus.INVMSGLEN);
			return submitSm;
		}
		
		byte esmClass=0;
		if(udh!=null && udh.length>0)
			esmClass+=EsmClass.UDHI.getValue();
		
		submitSm.setEsmClass(esmClass);
		submitSm.setReferenceObject(messageID);
		return submitSm;
	}
	
	private DeliverSm generateDeliverSm(String uniqueID, String messageID, byte[] data,byte[] udh, Integer deliveryErrorCode, DeliveryStatus deliveryStatus, Encoding messageEncoding, String fromDigits,NumberPlan fromNp, TypeOfNetwork fromTon, String toDigits,NumberPlan toNp, TypeOfNetwork toTon, ConnectionListener listener)
	{
		DeliverSm deliverSm=new DeliverSm();
		
		if(messageEncoding!=null)
			deliverSm.setDataCoding((byte)messageEncoding.getValue());
		
		Address fromAddress=null;
		if(fromDigits!=null){
			fromAddress=new Address();
			fromAddress.setAddress(fromDigits);
			
			if(fromNp!=null)
				fromAddress.setNpi((byte)fromNp.getValue());
			
			if(fromTon!=null)
				fromAddress.setTon((byte)fromTon.getValue());
		}			
		
		deliverSm.setDestAddress(fromAddress);
		
		Address toAddress=null;
		if(toDigits!=null){
			toAddress=new Address();
			toAddress.setAddress(toDigits);
			
			if(toNp!=null)
				toAddress.setNpi((byte)toNp.getValue());
			
			if(toTon!=null)
				toAddress.setTon((byte)toTon.getValue());
		}
		
		deliverSm.setSourceAddress(toAddress);		
		byte registeredDelivery=0;
		deliverSm.setRegisteredDelivery(registeredDelivery);
				
		try
		{
			Date submitDate=new Date();
			submitDate.setTime(System.currentTimeMillis());
			String dlrData="";
			if(data!=null)
				dlrData=new String(data);
			
			deliverSm.setShortMessage(SmppHelper.encodeMessage(SmppHelper.createDeliveryReport(messageID, submitDate, new Date(), deliveryErrorCode, dlrData, deliveryStatus), messageEncoding));
		}
		catch(SmppInvalidArgumentException ex)
		{
			listener.responseReceived(uniqueID, messageID, null, MessageStatus.INVMSGLEN);
			return deliverSm;
		}
		catch(UnsupportedEncodingException ex1)
		{
			listener.responseReceived(uniqueID, messageID, null, MessageStatus.INVDCS);
			return deliverSm;
		}
		
		byte esmClass=0;
		esmClass+=EsmClass.MT_SMSC_DELIVERY_RECEIPT.getValue();
		
		deliverSm.setEsmClass(esmClass);
		deliverSm.setReferenceObject(messageID);
		return deliverSm;
	}
}