package com.mobius.software.protocols.smpp.server;
/* Copyright 2019(C) Mobius Software LTD - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Yulian Oifa <yulian.oifa@mobius-software.com>
 */
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;

import com.mobius.software.common.dal.timers.PeriodicQueuedTasks;
import com.mobius.software.common.dal.timers.Timer;
import com.mobius.software.protocols.smpp.BaseBind;
import com.mobius.software.protocols.smpp.BaseBindResp;
import com.mobius.software.protocols.smpp.MessageStatus;
import com.mobius.software.protocols.smpp.SmppSessionListener;
import com.mobius.software.protocols.smpp.channel.SmppServerHandler;
import com.mobius.software.protocols.smpp.channel.SmppServerSession;
import com.mobius.software.protocols.smpp.channel.SmppSessionConfiguration;
import com.mobius.software.protocols.smpp.channel.SmppSessionHandler;
import com.mobius.software.protocols.smpp.channel.SmppSessionImpl;
import com.mobius.software.protocols.smpp.exceptions.SmppProcessingException;

public class SmppServerHandlerImpl implements SmppServerHandler  
{
	public static Logger logger=LogManager.getLogger(SmppServerHandlerImpl.class);
	
	private SmppSessionListener callbackInterface;
	private Boolean isTls;
	private Long enquireLinkInterval;
	private ConcurrentHashMap<String, ConcurrentHashMap<String,SmppServerSession>> sessionsMap;
	private ConcurrentHashMap<String, ConcurrentHashMap<String,HealthCheckTimer>> timersMap;
	private PeriodicQueuedTasks<Timer> timersQueue;
	
	public SmppServerHandlerImpl(SmppSessionListener callbackInterface,PeriodicQueuedTasks<Timer> timersQueue,ConcurrentHashMap<String, ConcurrentHashMap<String,SmppServerSession>> sessionsMap,ConcurrentHashMap<String, ConcurrentHashMap<String,HealthCheckTimer>> timersMap,Boolean isTls,Long enquireLinkInterval)
	{
		this.callbackInterface=callbackInterface;
		this.timersQueue=timersQueue;
		this.sessionsMap=sessionsMap;
		this.timersMap=timersMap;
		this.isTls=isTls;
		this.enquireLinkInterval=enquireLinkInterval;		
	}
	
	@Override
	public void sessionBindRequested(SmppSessionConfiguration sessionConfiguration, @SuppressWarnings("rawtypes") BaseBind bindRequest) throws SmppProcessingException 
	{
		String originatorID=null;
		try
		{
			originatorID=callbackInterface.bindRequested(sessionConfiguration.getHost(),sessionConfiguration.getPort(), bindRequest.getSystemId(), bindRequest.getPassword(), isTls);
		}
		catch(Exception ex)
		{
			throw new SmppProcessingException(MessageStatus.ALYBND);
		}
		
		if(originatorID==null)
			throw new SmppProcessingException(MessageStatus.INVPASWD);
		
		callbackInterface.sessionBound(sessionConfiguration.getHost(),sessionConfiguration.getPort(), originatorID);
		String fullID=originatorID + "_" + ObjectId.get().toHexString();
		sessionConfiguration.setName(fullID);
	}

	@Override
	public void sessionCreated(SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException 
	{
		SmppSessionConfiguration sessionConfiguration = session.getConfiguration();
		String[] segments=sessionConfiguration.getName().split("_");
		ConcurrentHashMap<String,SmppServerSession> connectionMap=sessionsMap.get(segments[0]);
		if(connectionMap==null)
		{
			connectionMap=new ConcurrentHashMap<String,SmppServerSession>();
			ConcurrentHashMap<String,SmppServerSession> oldMap=sessionsMap.putIfAbsent(segments[0], connectionMap);
			if(oldMap!=null)
				connectionMap=oldMap;
		}
		
		connectionMap.putIfAbsent(segments[1],session);
		HealthCheckTimer timer=null;
		if(enquireLinkInterval!=null)
		{
			timer=new HealthCheckTimer(session, enquireLinkInterval);
			timersQueue.store(timer.getRealTimestamp(), timer);
			
			ConcurrentHashMap<String, HealthCheckTimer> timerMap=timersMap.get(segments[0]);
			if(timerMap==null)
			{
				timerMap=new ConcurrentHashMap<String, HealthCheckTimer>();
				ConcurrentHashMap<String, HealthCheckTimer> oldMap=timersMap.putIfAbsent(segments[0], timerMap);
				if(oldMap!=null)
					timerMap=oldMap;
			}
			
			timerMap.putIfAbsent(segments[0], timer);			
		}
		
		SmppSessionHandler handler=callbackInterface.createServerHandler(session, timer);
		handler.setSession(session);
		session.serverReady(handler);
		callbackInterface.connectionEstablished(session.getConfiguration().getHost(), session.getConfiguration().getPort(), segments[0]);
	}

	@Override
	public void sessionDestroyed(SmppServerSession session) 
	{
		String[] segments=session.getConfiguration().getName().split("_");
		ConcurrentHashMap<String,SmppServerSession> connectionMap=sessionsMap.get(segments[0]);
		if(connectionMap!=null)
			connectionMap.remove(segments[1]);

		ConcurrentHashMap<String, HealthCheckTimer> timerMap=timersMap.get(segments[0]);
		HealthCheckTimer timer=null;
		if(timerMap!=null)
			timer=timerMap.remove(segments[1]);
		
		if(timer!=null)
			timer.stop();
		
		SmppSessionImpl defaultSession = (SmppSessionImpl) session;
		defaultSession.expireAll();
		session.destroy();
		callbackInterface.unbindRequested(session.getConfiguration().getHost(), session.getConfiguration().getPort(), segments[0]);
		callbackInterface.sessionUnbound(session.getConfiguration().getHost(), session.getConfiguration().getPort(), segments[0]);
	}

	@Override
	public Boolean isUp(String uniqueID) 
	{
		ConcurrentHashMap<String,SmppServerSession> innerMap = sessionsMap.get(uniqueID);
		return innerMap!=null && innerMap.size()>0;
	}
}