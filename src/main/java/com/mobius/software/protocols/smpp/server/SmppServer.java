package com.mobius.software.protocols.smpp.server;
/* Copyright 2019(C) Mobius Software LTD - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Yulian Oifa <yulian.oifa@mobius-software.com>
 */
import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mobius.software.common.dal.timers.PeriodicQueuedTasks;
import com.mobius.software.common.dal.timers.Timer;
import com.mobius.software.protocols.smpp.BaseBind;
import com.mobius.software.protocols.smpp.BaseBindResp;
import com.mobius.software.protocols.smpp.MessageStatus;
import com.mobius.software.protocols.smpp.PduTranscoder;
import com.mobius.software.protocols.smpp.Tlv;
import com.mobius.software.protocols.smpp.channel.SmppServerConfiguration;
import com.mobius.software.protocols.smpp.channel.SmppServerHandler;
import com.mobius.software.protocols.smpp.channel.SmppSession;
import com.mobius.software.protocols.smpp.channel.SmppSessionConfiguration;
import com.mobius.software.protocols.smpp.channel.SmppSessionImpl;
import com.mobius.software.protocols.smpp.channel.SmppSessionWrapper;
import com.mobius.software.protocols.smpp.channel.SmppVersion;
import com.mobius.software.protocols.smpp.exceptions.SmppChannelException;
import com.mobius.software.protocols.smpp.exceptions.SmppProcessingException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class SmppServer
{
	public static Logger logger=LogManager.getLogger(SmppServer.class);
	
	private ServerBootstrap serverBootstrap;
    private Channel serverChannel; 
    
    private final PduTranscoder transcoder;    
    private final SmppServerConnector serverConnector;
    private final SmppServerConfiguration configuration;
    private final SmppServerHandler serverHandler;
    
    private PeriodicQueuedTasks<Timer> timersQueue;
    
    public SmppServer(Boolean isEpoll,SmppServerConfiguration configuration, SmppServerHandler serverHandler, EventLoopGroup acceptorGroup,EventLoopGroup clientGroup, PeriodicQueuedTasks<Timer> timersQueue) 
    {
    	this.timersQueue = timersQueue;
        this.configuration = configuration;
        this.serverHandler = serverHandler;
        
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.group(acceptorGroup, clientGroup);
        
        if(isEpoll)
        	this.serverBootstrap.channel(EpollServerSocketChannel.class);
        else
        	this.serverBootstrap.channel(NioServerSocketChannel.class);
        
        this.serverConnector = new SmppServerConnector(this, timersQueue);
        this.serverBootstrap.childHandler(serverConnector);
        
        this.transcoder = new PduTranscoder();
    }
    
	public boolean isStarted() 
	{
		return (this.serverChannel != null && this.serverChannel.isActive());
	}

	public boolean isStopped() 
	{
		return (this.serverChannel == null);
	}

	public boolean isDestroyed() 
	{
		return (this.serverBootstrap == null);
	}

	public void start() throws SmppChannelException 
	{
		if (isDestroyed()) 
		    throw new SmppChannelException("Unable to start: server is destroyed");
        
		try 
		{
			ChannelFuture future = this.serverBootstrap.bind(new InetSocketAddress(configuration.getHost(), configuration.getPort()));
			future.awaitUninterruptibly();
			serverChannel=future.channel();
            logger.info(configuration.getName() + " started at " + configuration.getHost() + ":" + configuration.getPort());
        } 
		catch (Exception e) 
		{
            throw new SmppChannelException(e.getMessage(), e);
        }
	}

	public void stop() 
	{
		if (this.serverChannel != null) 
		{
            this.serverChannel.close().awaitUninterruptibly();
            this.serverChannel = null;
        }
		
        logger.info(configuration.getName() + " stopped at " + configuration.getHost() + ":" + configuration.getPort());
	}

	public void destroy() 
	{
		stop();
        this.serverBootstrap = null;
        logger.info(configuration.getName() + " destroyed at " + configuration.getHost() + ":" + configuration.getPort());
	}

	public Boolean isUp(String uniqueID) 
	{
		return serverHandler.isUp(uniqueID);
	}

	public PduTranscoder getTranscoder() 
	{
        return this.transcoder;
    }
	
	public SmppServerConfiguration getConfiguration() 
	{
        return this.configuration;
    }
	
	protected SmppVersion autoNegotiateInterfaceVersion(SmppVersion requestedInterfaceVersion) 
	{
        if (!this.configuration.isAutoNegotiateInterfaceVersion()) 
            return requestedInterfaceVersion;
        else 
        {
            if (requestedInterfaceVersion.getValue() >= SmppVersion.VERSION_3_4.getValue()) 
                return SmppVersion.VERSION_3_4;
            else 
                return SmppVersion.VERSION_3_3;            
        }
    }
	
	@SuppressWarnings("rawtypes")
	protected BaseBindResp createBindResponse(BaseBind bindRequest, MessageStatus statusCode) 
	{
		 BaseBindResp bindResponse = (BaseBindResp)bindRequest.createResponse();
		 bindResponse.setCommandStatus(statusCode);
		 bindResponse.setSystemId(configuration.getSystemId());

		 if (configuration.getInterfaceVersion().getValue() >= SmppVersion.VERSION_3_4.getValue() && bindRequest.getInterfaceVersion().getValue() >= SmppVersion.VERSION_3_4.getValue()) 
		 {
			 Tlv scInterfaceVersion = new Tlv(SmppVersion.TAG_SC_INTERFACE_VERSION, new byte[] { (byte)configuration.getInterfaceVersion().getValue() });
			 bindResponse.addOptionalParameter(scInterfaceVersion);
		 }

		 return bindResponse;
	}
	
	@SuppressWarnings("rawtypes")
	protected void bindRequested(SmppSessionConfiguration config, BaseBind bindRequest) throws SmppProcessingException 
	{
        this.serverHandler.sessionBindRequested(config, bindRequest);
    }
	
	protected void createSession(Channel channel, SmppSessionConfiguration config, BaseBindResp preparedBindResponse) throws SmppProcessingException 
	{
		SmppVersion interfaceVersion = this.autoNegotiateInterfaceVersion(config.getInterfaceVersion());

        SmppSessionImpl session = new SmppSessionImpl(SmppSession.Type.SERVER, config, channel, serverHandler, preparedBindResponse, interfaceVersion, timersQueue);

        // create a new wrapper around a session to pass the pdu up the chain
        channel.pipeline().remove(SmppSessionWrapper.NAME);
        channel.pipeline().addLast(SmppSessionWrapper.NAME, new SmppSessionWrapper(session));
        
        this.serverHandler.sessionCreated(session, preparedBindResponse);                
    }


    protected void destroySession(SmppSessionImpl session) 
    {
        serverHandler.sessionDestroyed(session);                
    }
}