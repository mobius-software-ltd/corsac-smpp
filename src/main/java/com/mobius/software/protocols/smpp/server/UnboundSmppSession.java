package com.mobius.software.protocols.smpp.server;
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

import com.mobius.software.common.dal.timers.Task;
import com.mobius.software.common.dal.timers.WorkerPool;
import com.mobius.software.protocols.smpp.BaseBind;
import com.mobius.software.protocols.smpp.BaseBindResp;
import com.mobius.software.protocols.smpp.BindReceiver;
import com.mobius.software.protocols.smpp.BindTransceiver;
import com.mobius.software.protocols.smpp.BindTransmitter;
import com.mobius.software.protocols.smpp.EnquireLink;
import com.mobius.software.protocols.smpp.EnquireLinkResp;
import com.mobius.software.protocols.smpp.MessageStatus;
import com.mobius.software.protocols.smpp.Pdu;
import com.mobius.software.protocols.smpp.PduResponse;
import com.mobius.software.protocols.smpp.SmppBindType;
import com.mobius.software.protocols.smpp.channel.ChannelUtil;
import com.mobius.software.protocols.smpp.channel.SmppSessionChannelListener;
import com.mobius.software.protocols.smpp.channel.SmppSessionConfiguration;
import com.mobius.software.protocols.smpp.exceptions.SmppProcessingException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class UnboundSmppSession implements SmppSessionChannelListener
{
	public static Logger logger=LogManager.getLogger(UnboundSmppSession.class);
	
	private final String channelName;
    private final Channel channel;
    private final BindTimeoutTask bindTimeoutTask;
    private final SmppServer server;
	private final WorkerPool workerPool;
   
	public UnboundSmppSession(String channelName, Channel channel, SmppServer server, WorkerPool workerPool)
    {
        this.channelName = channelName;
        this.channel = channel;
        this.server = server;
		this.workerPool = workerPool;
        
        this.bindTimeoutTask = new BindTimeoutTask(channel,channelName,this.server.getConfiguration().getBindTimeout(), "SmppBindTimeoutTask");
		workerPool.getPeriodicQueue().store(bindTimeoutTask.getRealTimestamp(), bindTimeoutTask);
		
    }

    @SuppressWarnings("rawtypes")
	@Override
    public void firePduReceived(Pdu pdu) 
    {
    	logger.info("received PDU: " + pdu);

    	Task processingTask = new Task() {
			@Override
			public void execute()
			{
				if (pdu instanceof BaseBind) 
		        {
		            BaseBind bindRequest = (BaseBind)pdu;
		            SmppSessionConfiguration sessionConfiguration = createSessionConfiguration(bindRequest);
		            
		            try 
		            {
						server.bindRequested(sessionConfiguration, bindRequest);
		            } 
		            catch (SmppProcessingException e) 
		            {
						logger.info("Bind request rejected or failed for connection [" + channelName + "] with error [" + e.getMessage() + "]");
		                BaseBindResp bindResponse = server.createBindResponse(bindRequest, e.getErrorCode());
						sendResponsePdu(bindResponse);
		                closeChannelAndCancelTimer();
		                return;
		            }

					bindTimeoutTask.stop();
		            BaseBindResp preparedBindResponse = server.createBindResponse(bindRequest, MessageStatus.OK);

		            try 
		            {
		                server.createSession(channel, sessionConfiguration, preparedBindResponse);
		            } 
		            catch (SmppProcessingException e) 
		            {
		                logger.warn("Bind request was approved, but createSession failed for connection [" + channelName + "] with error [" + e.getMessage() + "]");
		                BaseBindResp bindResponse = server.createBindResponse(bindRequest, e.getErrorCode());
						sendResponsePdu(bindResponse);
		                closeChannelAndCancelTimer();
		                return;
		            }
		        } 
		        else if (pdu instanceof EnquireLink) 
		        {
		            EnquireLinkResp response = ((EnquireLink) pdu).createResponse();
		            logger.info("Responding to enquire_link with response [" + response + "]");
					sendResponsePdu(response);
		            return;
		        } 
		        else 
		        {
		            logger.warn("Only bind or enquire_link requests are permitted on new connections, closing connection [" + channelName + "]");
		            closeChannelAndCancelTimer();
		            return;
		        }
				
			}

			@Override
			public long getStartTime()
			{
				return System.currentTimeMillis();
			}

			@Override
			public String printTaskDetails()
			{
				return "Task name: SmppSessionprovessingPduTask";
			}
		};
    	
		this.workerPool.getQueue().offerLast(processingTask);
    }

    public void closeChannelAndCancelTimer() 
    {
        this.bindTimeoutTask.stop();
        this.channel.close();
        logger.error("Closing channel and canceling timer");
    }

    @Override
    public void fireExceptionThrown(Throwable t) 
    {
        logger.warn("Exception thrown, closing connection [" + channelName + "]: " + t);
        
        Task processingTask = new Task() {
			@Override
			public void execute()
			{
				logger.error("Executing SmppSession-exceptionThrownTask, closing channel and canceling timer");
				closeChannelAndCancelTimer();
			}

			@Override
			public long getStartTime()
			{
				return System.currentTimeMillis();
			}
			
			@Override
			public String printTaskDetails()
			{
				return "Task name: SmppSession-exceptionThrownTask";
			}
		};

		this.workerPool.getQueue().offerLast(processingTask);
    }

    @Override
    public void fireChannelClosed() 
    {
        logger.info("Connection closed with [" + channelName + "]");
        closeChannelAndCancelTimer();
    }

    @SuppressWarnings("rawtypes")
	protected SmppSessionConfiguration createSessionConfiguration(BaseBind bindRequest) 
    {
        SmppSessionConfiguration sessionConfiguration = new SmppSessionConfiguration();
        sessionConfiguration.setName("SmppServerSession." + bindRequest.getSystemId() + "." + bindRequest.getSystemType());
        sessionConfiguration.setSystemId(bindRequest.getSystemId());
        sessionConfiguration.setPassword(bindRequest.getPassword());
        sessionConfiguration.setSystemType(bindRequest.getSystemType());
        sessionConfiguration.setBindTimeout(server.getConfiguration().getBindTimeout());
        sessionConfiguration.setHost(ChannelUtil.getChannelRemoteHost(channel));
        sessionConfiguration.setPort(ChannelUtil.getChannelRemotePort(channel));
        sessionConfiguration.setInterfaceVersion(bindRequest.getInterfaceVersion());

        if (bindRequest instanceof BindTransceiver) 
            sessionConfiguration.setType(SmppBindType.TRANSCEIVER);
        else if (bindRequest instanceof BindReceiver) 
            sessionConfiguration.setType(SmppBindType.RECEIVER);
        else if (bindRequest instanceof BindTransmitter) 
            sessionConfiguration.setType(SmppBindType.TRANSMITTER);        
        
        sessionConfiguration.setRequestExpiryTimeout(server.getConfiguration().getDefaultRequestExpiryTimeout());
        return sessionConfiguration;
    }

    public void sendResponsePdu(PduResponse pdu) 
    {
        try 
        {
            ByteBuf buffer = server.getTranscoder().encode(pdu);
            logger.info("send PDU: " + pdu);
            this.channel.write(buffer);                     
        } 
        catch (Exception e) 
        {
            logger.error("Fatal exception thrown while attempting to send response PDU: {}", e);
        }
    }       
}