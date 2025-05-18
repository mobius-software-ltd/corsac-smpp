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
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLEngine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mobius.software.common.dal.timers.WorkerPool;
import com.mobius.software.protocols.smpp.BaseBind;
import com.mobius.software.protocols.smpp.BindReceiver;
import com.mobius.software.protocols.smpp.BindTransceiver;
import com.mobius.software.protocols.smpp.BindTransmitter;
import com.mobius.software.protocols.smpp.Pdu;
import com.mobius.software.protocols.smpp.PduRequest;
import com.mobius.software.protocols.smpp.PduResponse;
import com.mobius.software.protocols.smpp.SmppBindType;
import com.mobius.software.protocols.smpp.SmppSessionListener;
import com.mobius.software.protocols.smpp.channel.SmppMessageDecoder;
import com.mobius.software.protocols.smpp.channel.SmppSession;
import com.mobius.software.protocols.smpp.channel.SmppSessionConfiguration;
import com.mobius.software.protocols.smpp.channel.SmppSessionHandler;
import com.mobius.software.protocols.smpp.channel.SmppSessionImpl;
import com.mobius.software.protocols.smpp.channel.SmppSessionWrapper;
import com.mobius.software.protocols.smpp.channel.SslConfiguration;
import com.mobius.software.protocols.smpp.channel.SslContextFactory;
import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.SmppChannelConnectException;
import com.mobius.software.protocols.smpp.exceptions.SmppChannelException;
import com.mobius.software.protocols.smpp.exceptions.SmppTimeoutException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

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
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;

public class SmppClient
{
	public static Logger logger = LogManager.getLogger(SmppClient.class);
	public static Logger debugLogger = LogManager.getLogger("DEBUG");

	private EventLoopGroup loop;
	private ConcurrentHashMap<String, SmppSessionImpl> session = new ConcurrentHashMap<String, SmppSessionImpl>();
	private ConcurrentHashMap<String, SmppSessionImpl> pendingSessions = new ConcurrentHashMap<String, SmppSessionImpl>();
	private Integer clientsPoolSize;
	private AtomicInteger wheel = new AtomicInteger(0);
	private SmppSessionConfiguration configuration;
	private SmppSessionListener callbackInterface;
	private SmppClientConnector clientConnector = new SmppClientConnector(this);
	private Bootstrap bootstrap;
	private Long enquiryTimeout;
	
	private WorkerPool workerPool;

	private AtomicBoolean isStarted = new AtomicBoolean(false);

	public SmppClient(Boolean isEpoll, SmppSessionListener callbackInterface, Integer maxChannels, SmppSessionConfiguration configuration, Long enquiryTimeout, EventLoopGroup acceptorGroup, WorkerPool workerPool)
	{
		this(isEpoll, callbackInterface, maxChannels, configuration, enquiryTimeout, acceptorGroup, workerPool, null, null);
	}

	public SmppClient(Boolean isEpoll, SmppSessionListener callbackInterface, Integer maxChannels, SmppSessionConfiguration configuration, Long enquiryTimeout, EventLoopGroup acceptorGroup, WorkerPool workerPool, String localHost, Integer localPort)
	{
		if (maxChannels != null)
			this.clientsPoolSize = maxChannels;
		else
			this.clientsPoolSize = 1;

		this.configuration = configuration;
		this.loop = acceptorGroup;
		this.workerPool = workerPool;
		this.callbackInterface = callbackInterface;
		this.enquiryTimeout = enquiryTimeout;

		bootstrap = new Bootstrap();
		bootstrap.group(loop);
		if (isEpoll)
			bootstrap.channel(EpollSocketChannel.class);
		else
			bootstrap.channel(NioSocketChannel.class);

		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.option(ChannelOption.SO_SNDBUF, 262144);
		bootstrap.option(ChannelOption.SO_RCVBUF, 262144);

		if (localHost != null)
		{
			if (localPort != null)
				bootstrap.localAddress(localHost, localPort);
			else
				bootstrap.localAddress(localHost, 0);
		}
		else if (localPort != null)
			bootstrap.localAddress(localPort);

		bootstrap.remoteAddress(configuration.getHost(), configuration.getPort());
		bootstrap.handler(clientConnector);
	}

	public void startClient()
	{
		isStarted.set(true);
		for (int i = 0; i < clientsPoolSize; i++)
			initiateChannel();
	}

	public void stopClient()
	{
		isStarted.set(false);
		Iterator<SmppSessionImpl> iterator = session.values().iterator();
		while (iterator.hasNext())
		{
			SmppSessionImpl session = iterator.next();
			session.fireChannelClosed();
			session.unbind(500);
		}

		iterator = pendingSessions.values().iterator();
		while (iterator.hasNext())
		{
			SmppSessionImpl session = iterator.next();
			session.fireChannelClosed();
			session.expireAll();
			session.destroy();
		}
	}

	public Boolean isUp()
	{
		return session.size() > 0;
	}

	private void initiateChannel()
	{
		if (!isStarted.get())
			return;

		bootstrap.connect().addListener(new ClientChannelConnectListener(workerPool.getPeriodicQueue(), this, configuration));
	}

	public void startChannel(SmppSessionImpl oldSession)
	{
		if (oldSession != null)
			session.remove(oldSession.getId());

		if (oldSession != null && oldSession.getChannel() != null)
			oldSession.getChannel().close();

		initiateChannel();
	}

	public void restartChannel(SmppSessionImpl oldSession)
	{
		if (!isStarted.get())
			return;

		if (oldSession != null)
			session.remove(oldSession.getId());

		if (oldSession != null)
		{
			if (oldSession.getChannel().remoteAddress() != null && oldSession.getChannel().remoteAddress() instanceof InetSocketAddress)
			{
				InetSocketAddress realAddress = (InetSocketAddress) oldSession.getChannel().remoteAddress();
				callbackInterface.connectionEstablished(realAddress.getHostName(), realAddress.getPort(), configuration.getName());
			}
		}

		if (oldSession != null && oldSession.getChannel() != null)
			oldSession.getChannel().close();

		DelayedReconnectTimer reconnectTimer = new DelayedReconnectTimer(this, configuration);
		workerPool.getPeriodicQueue().store(reconnectTimer.getRealTimestamp(), reconnectTimer);
	}

	public Long getEnquiryTimeout()
	{
		return enquiryTimeout;
	}

	@SuppressWarnings("rawtypes")
	public void channelConnected(Channel channel)
	{
		try
		{
			SmppSessionHandler sessionHandler = callbackInterface.createClientHandler(this, configuration.getName());
			SmppSessionImpl session = createSession(channel, configuration, sessionHandler);
			sessionHandler.setSession(session);

			if (debugLogger.isDebugEnabled())
				debugLogger.debug("Channel connected for:" + configuration.getName() + ",Sending Bind Request");

			BaseBind bindRequest = createBindRequest(configuration);
			session.sendBindRequest(bindRequest, configuration.getBindTimeout());
		}
		catch (Exception ex)
		{
			logger.error("An error occured while requesting bind request to remote address " + channel.remoteAddress().toString(), ex);
			channel.close();
			restartChannel(null);
		}
	}

	public void sessionBound(SmppSessionImpl session)
	{
		this.session.put(session.getId(), session);
		if (session.getChannel().remoteAddress() != null && session.getChannel().remoteAddress() instanceof InetSocketAddress)
		{
			InetSocketAddress realAddress = (InetSocketAddress) session.getChannel().remoteAddress();
			callbackInterface.connectionEstablished(realAddress.getHostName(), realAddress.getPort(), configuration.getName());
		}
	}

	@SuppressWarnings("rawtypes")
	public void send(Pdu pdu, String pduID) throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException
	{
		if (session.size() == 0)
			throw new SmppChannelException("no available channels found");

		Iterator<SmppSessionImpl> iterator = session.values().iterator();
		int startEntry = wheel.incrementAndGet() % session.size();
		while (startEntry > 0)
		{
			if (iterator.hasNext())
			{
				iterator.next();
				startEntry--;
			}
			else
			{
				if (session.size() == 0)
					throw new SmppChannelException("no available channels found");

				iterator = session.values().iterator();

			}
		}

		int retries = session.size();
		while (retries > 0)
		{
			if (iterator.hasNext())
			{
				SmppSessionImpl currSession = iterator.next();
				if (currSession != null && currSession.isBound())
				{
					try
					{
						if (pdu.isRequest())
							currSession.sendRequestPdu((PduRequest) pdu, pduID);
						else
							currSession.sendResponsePdu((PduResponse) pdu, pduID);
					}
					catch (Exception e)
					{
						logger.error("An exception occured during sending pdu request/response," + e);
					}

					return;
				}
				else
					retries--;
			}
			else
			{
				if (session.size() == 0)
					throw new SmppChannelException("no available channels found");

				iterator = session.values().iterator();
			}
		}

		throw new SmppChannelException("no available channels found");
	}

	protected SmppSessionImpl createSession(Channel channel, SmppSessionConfiguration config, SmppSessionHandler sessionHandler) throws SmppTimeoutException, SmppChannelException, InterruptedException
	{
		SmppSessionImpl session = new SmppSessionImpl(SmppSession.Type.CLIENT, config, channel, sessionHandler, this.workerPool);

		// add SSL handler
		if (config.isUseSsl())
		{
			SslConfiguration sslConfig = config.getSslConfiguration();
			if (sslConfig == null)
				throw new IllegalStateException("sslConfiguration must be set");
			try
			{
				SslContextFactory factory = new SslContextFactory(sslConfig);
				SSLEngine sslEngine = factory.newSslEngine();
				sslEngine.setUseClientMode(true);
				channel.pipeline().addLast("SSL", new SslHandler(sslEngine));
			}
			catch (Exception e)
			{
				throw new SmppChannelConnectException("Unable to create SSL session]: " + e.getMessage(), e);
			}
		}

		channel.pipeline().addLast(SmppMessageDecoder.NAME, new SmppMessageDecoder(session.getTranscoder()));
		channel.pipeline().addLast(SmppSessionWrapper.NAME, new SmppSessionWrapper(session, this.workerPool.getQueue()));
		return session;
	}

	@SuppressWarnings("rawtypes")
	protected BaseBind createBindRequest(SmppSessionConfiguration config) throws UnrecoverablePduException
	{
		BaseBind bind = null;
		if (config.getType() == SmppBindType.TRANSCEIVER)
			bind = new BindTransceiver();
		else if (config.getType() == SmppBindType.RECEIVER)
			bind = new BindReceiver();
		else if (config.getType() == SmppBindType.TRANSMITTER)
			bind = new BindTransmitter();
		else
			throw new UnrecoverablePduException("Unable to convert SmppSessionConfiguration into a BaseBind request");

		bind.setSystemId(config.getSystemId());
		bind.setPassword(config.getPassword());
		bind.setSystemType(config.getSystemType());
		bind.setInterfaceVersion(config.getInterfaceVersion());
		return bind;
	}
}
