package com.mobius.software.protocols.smpp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.mobius.software.common.dal.timers.PeriodicQueuedTasks;
import com.mobius.software.common.dal.timers.Timer;
import com.mobius.software.protocols.smpp.channel.SmppSessionConfiguration;
import com.mobius.software.protocols.smpp.channel.SmppVersion;
import com.mobius.software.protocols.smpp.channel.SslConfiguration;
import com.mobius.software.protocols.smpp.client.SmppClient;
import com.mobius.software.protocols.smpp.exceptions.SmppChannelException;

import io.netty.channel.EventLoopGroup;

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
public class TestSmppClient 
{
	private ConnectionListener listener;
	private ConcurrentHashMap<String, SmppClient> clients=new ConcurrentHashMap<String,SmppClient>();
	
	public TestSmppClient()
	{		
	}
	
	public void start(String name, Boolean isTLS, Boolean isEpoll, String host, Integer port,KeyStore keyStore,KeyStore trustStore,SmppChannelConfig config, SmppSessionListener smppListener, ConnectionListener listener,PeriodicQueuedTasks<Timer> timersQueue,EventLoopGroup acceptorGroup,EventLoopGroup clientGroup,Integer workers) throws ClassNotFoundException, GeneralSecurityException, IOException, SmppChannelException
	{
		this.listener=listener;
		if(clients.contains(name))
			return;
		
		SmppSessionConfiguration configuration=new SmppSessionConfiguration();
		if(config.getBindTimeout()!=null)
			configuration.setBindTimeout(config.getBindTimeout());
		
		if(config.getConnectionTimeout()!=null)
			configuration.setConnectTimeout(config.getConnectionTimeout());
		
		if(config.getRequestExpiryTimeout()!=null)
			configuration.setRequestExpiryTimeout(config.getRequestExpiryTimeout());
		
		configuration.setName(name);
		configuration.setSystemId(config.getUsername());
		configuration.setPassword(config.getPassword());
		
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
		
		SmppClient client=new SmppClient(isEpoll, smppListener, config.getMaxChannels(), configuration, config.getEnquireLinkInterval(), acceptorGroup, timersQueue);
		clients.put(name, client);
		client.startClient();
	}

	public void removeClientSession(String originatorID)
	{
		SmppClient client=clients.remove(originatorID);
		if(client!=null)
			client.stopClient();		
	}
	
	public void stop() 
	{
		ConcurrentHashMap<String, SmppClient> oldMap=clients;
		clients=new ConcurrentHashMap<String, SmppClient>();
		Iterator<SmppClient> clients=oldMap.values().iterator();
		while(clients.hasNext())
			clients.next().stopClient();
	}
	
	public void sendMessage(String sourceID, String messageID, SubmitSm submitSm) 
	{
		SmppClient client=clients.get(sourceID);
		if(client==null)
		{
			listener.responseReceived(sourceID, messageID, null, MessageStatus.SYSERR);
			return;
		}
		
		try
		{
			submitSm.setReferenceObject(messageID);
			client.send(submitSm);
		}
		catch(Exception ex)
		{
			listener.responseReceived(sourceID, messageID, null, MessageStatus.SYSERR);			
		}
	}
	
	public void sendDelivery(String sourceID, String messageID, DeliverSm deliverSm) 
	{
		SmppClient client=clients.get(sourceID);
		if(client==null)
		{
			listener.responseReceived(sourceID, messageID, null, MessageStatus.SYSERR);
			return;
		}
		
		try
		{
			deliverSm.setReferenceObject(messageID);
			client.send(deliverSm);
		}
		catch(Exception ex)
		{
			listener.responseReceived(sourceID, messageID, null, MessageStatus.SYSERR);			
		}
	}		
}