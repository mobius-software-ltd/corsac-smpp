package com.mobius.software.protocols.smpp;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.mobius.software.common.dal.timers.WorkerPool;
import com.mobius.software.protocols.smpp.channel.SmppServerConfiguration;
import com.mobius.software.protocols.smpp.channel.SmppServerSession;
import com.mobius.software.protocols.smpp.channel.SmppSessionImpl;
import com.mobius.software.protocols.smpp.channel.SmppVersion;
import com.mobius.software.protocols.smpp.channel.SslConfiguration;
import com.mobius.software.protocols.smpp.exceptions.SmppChannelException;
import com.mobius.software.protocols.smpp.server.HealthCheckTimer;
import com.mobius.software.protocols.smpp.server.SmppServer;
import com.mobius.software.protocols.smpp.server.SmppServerHandlerImpl;

import io.netty.channel.EventLoopGroup;

public class TestSmppServer
{
	private SmppServer server;
	private ConnectionListener listener;
	
	private ConcurrentHashMap<String, ConcurrentHashMap<String,SmppServerSession>> sessionMap=new ConcurrentHashMap<String, ConcurrentHashMap<String,SmppServerSession>>();
	private ConcurrentHashMap<String, ConcurrentHashMap<String,HealthCheckTimer>> timersMap=new ConcurrentHashMap<String, ConcurrentHashMap<String,HealthCheckTimer>>();
	private AtomicInteger wheel=new AtomicInteger(0);
	
	public TestSmppServer()
	{		
	}
	
	public void start(String name, Boolean isTLS, Boolean isEpoll, String host, Integer port, KeyStore keyStore, KeyStore trustStore, SmppChannelConfig config, SmppSessionListener smppListener, ConnectionListener listener, WorkerPool workerPool, EventLoopGroup acceptorGroup, EventLoopGroup clientGroup, Integer workers) throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		this.listener=listener;
		
		SmppServerConfiguration configuration = new SmppServerConfiguration();
		configuration.setAutoNegotiateInterfaceVersion(true);
		
		if(config.getBindTimeout()!=null)
			configuration.setBindTimeout(config.getBindTimeout());
		
		if(config.getConnectionTimeout()!=null)
			configuration.setConnectTimeout(config.getConnectionTimeout());
		
		if(config.getRequestExpiryTimeout()!=null)
			configuration.setDefaultRequestExpiryTimeout(config.getRequestExpiryTimeout());
		
		configuration.setName(name);
		configuration.setSystemId(name);
		
		configuration.setHost(host);
		configuration.setInterfaceVersion(SmppVersion.VERSION_5_0);
		configuration.setPort(port);
		if(isTLS)
		{
			configuration.setUseSsl(true);
			SslConfiguration sslConfiguration=new SslConfiguration();
			sslConfiguration.setWantClientAuth(false);
			sslConfiguration.setTrustAll(true);
			sslConfiguration.setExcludeProtocols("SSLv2Hello", "SSLv2");
			sslConfiguration.setValidateCerts(false);
			//sslConfiguration.setProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
			
			if(keyStore!=null)
			{
				File f=new File(this.getClass().getClassLoader().getResource("tls_keystore").getPath() + name);
				FileOutputStream fs=new FileOutputStream(f);
				keyStore.store(fs, "".toCharArray());
				
				sslConfiguration.setKeyStoreProvider(keyStore.getProvider().getName());
				sslConfiguration.setKeyStoreType(keyStore.getType());
				sslConfiguration.setKeyStorePassword("");
				sslConfiguration.setKeyStorePath(f.getAbsolutePath());
			}
			
			if(trustStore!=null)
			{
				File f=new File(this.getClass().getClassLoader().getResource("tls_keystore").getPath() + name);
				FileOutputStream fs=new FileOutputStream(f);
				trustStore.store(fs, "".toCharArray());
				
				sslConfiguration.setTrustStoreProvider(trustStore.getProvider().getName());
				sslConfiguration.setTrustStoreType(trustStore.getType());
				sslConfiguration.setTrustStorePassword("");
				sslConfiguration.setTrustStorePath(f.getAbsolutePath());
			}
			
			configuration.setSslConfiguration(sslConfiguration);
		}
		else
			configuration.setUseSsl(false);
		
		server = new SmppServer(isEpoll, configuration, new SmppServerHandlerImpl(smppListener, workerPool.getPeriodicQueue(), sessionMap, timersMap, isTLS, config.getEnquireLinkInterval()), acceptorGroup, clientGroup, workerPool);
		server.start();
	}

	public void removeServerSession(String uniqueID)
	{
		ConcurrentHashMap<String, HealthCheckTimer> timers=timersMap.remove(uniqueID);
		if(timers!=null)
		{
			Iterator<HealthCheckTimer> iterator=timers.values().iterator();
			while(iterator.hasNext())
				iterator.next().stop();							
		}	
		
		ConcurrentHashMap<String, SmppServerSession> sessions=sessionMap.remove(uniqueID);
		if(sessions!=null)
		{
			Iterator<SmppServerSession> iterator=sessions.values().iterator();
			while(iterator.hasNext())
				iterator.next().unbind(500);							
		}				
	}
	
	public void stop() 
	{
		if(server==null)
			return;
		
		ConcurrentHashMap<String, ConcurrentHashMap<String,SmppServerSession>> oldSessionMap=sessionMap;
		sessionMap = new ConcurrentHashMap<String, ConcurrentHashMap<String,SmppServerSession>>();
		Iterator<ConcurrentHashMap<String, SmppServerSession>> rootSessionsIterator=oldSessionMap.values().iterator();
		while(rootSessionsIterator.hasNext())
		{
			ConcurrentHashMap<String, SmppServerSession> sessions=rootSessionsIterator.next();
			Iterator<SmppServerSession> iterator=sessions.values().iterator();
			while(iterator.hasNext())
				iterator.next().close();
		}
		
		ConcurrentHashMap<String, ConcurrentHashMap<String,HealthCheckTimer>> oldTimersMap=timersMap;
		timersMap = new ConcurrentHashMap<String, ConcurrentHashMap<String,HealthCheckTimer>>();
		Iterator<ConcurrentHashMap<String, HealthCheckTimer>> rootTimersIterator=oldTimersMap.values().iterator();
		while(rootTimersIterator.hasNext())
		{
			ConcurrentHashMap<String, HealthCheckTimer> timers=rootTimersIterator.next();
			Iterator<HealthCheckTimer> iterator=timers.values().iterator();
			while(iterator.hasNext())
				iterator.next().stop();
		}
		
		server.stop();				
		server=null;				
	}

	private SmppServerSession chooseRandomSession(ConcurrentHashMap<String,SmppServerSession> sessions) throws SmppChannelException
	{
		if(sessions.size()==0)
    		throw new SmppChannelException("no available channels found");
    	
    	Iterator<SmppServerSession> iterator=sessions.values().iterator();
    	int startEntry=wheel.incrementAndGet()%sessions.size();
    	while(startEntry>0)
    	{
    		if(iterator.hasNext())
    		{
    			iterator.next();
    			startEntry--;
    		}
    		else
    		{
    			if(sessions.size()==0)
            		throw new SmppChannelException("no available channels found");
            	
            	iterator=sessions.values().iterator();
            	
    		}
    	}
    	
    	int retries=sessions.size();
    	while(retries>0)
    	{
    		if(iterator.hasNext())
    		{
    			SmppServerSession currSession=iterator.next();
    			if (currSession != null && currSession.isBound())
    				return currSession;
	            else
	            	retries--;
    		}
    		else
    		{
    			if(sessions.size()==0)
            		throw new SmppChannelException("no available channels found");
            	
            	iterator=sessions.values().iterator();
    		}
    	}
    	
    	return null;
	}
	
	public void sendMessage(String sourceID, String messageID, SubmitSm submitSm) 
	{
		ConcurrentHashMap<String,SmppServerSession> availableSessions=sessionMap.get(sourceID);
		if(availableSessions==null || availableSessions.size()==0)
		{
			listener.responseReceived(sourceID, messageID, null, MessageStatus.SYSERR);
			return;
		}
		
		SmppServerSession current=null;
		try
		{
			current=chooseRandomSession(availableSessions);
		}
		catch(SmppChannelException ex)
		{
			listener.responseReceived(sourceID, messageID, null, MessageStatus.SYSERR);
			return;
		}
		
		if(current==null)
		{
			listener.responseReceived(sourceID, messageID, null, MessageStatus.SYSERR);
			return;
		}
			
		try
		{
			submitSm.setReferenceObject(messageID);
			current.sendRequestPdu(submitSm);
		}
		catch(Exception ex)
		{
			listener.responseReceived(sourceID, messageID, null, MessageStatus.SYSERR);			
		}
	}
	
	public void sendDelivery(String sourceID, String messageID, DeliverSm deliverSm) 
	{
		ConcurrentHashMap<String,SmppServerSession> availableSessions=sessionMap.get(sourceID);
		if(availableSessions==null || availableSessions.size()==0)
		{
			listener.responseReceived(sourceID, messageID, null, MessageStatus.SYSERR);
			return;
		}
		
		SmppServerSession current=null;
		try
		{
			current=chooseRandomSession(availableSessions);
		}
		catch(SmppChannelException ex)
		{
			listener.responseReceived(sourceID, messageID, null, MessageStatus.SYSERR);
			return;
		}
			
		try
		{
			deliverSm.setReferenceObject(messageID);
			current.sendRequestPdu(deliverSm);
		}
		catch(Exception ex)
		{
			listener.responseReceived(sourceID, messageID, null, MessageStatus.SYSERR);			
		}
	}
	
	public void sendDelivery(String sourceID, String messageID, DeliverSm deliverSm,SmppSessionImpl session) 
	{
		try
		{
			deliverSm.setReferenceObject(messageID);
			session.sendRequestPdu(deliverSm);
		}
		catch(Exception ex)
		{
			listener.responseReceived(sourceID, messageID, null, MessageStatus.SYSERR);			
		}
	}
}