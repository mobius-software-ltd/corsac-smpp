package com.mobius.software.protocols.smpp.channel;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;

import com.mobius.software.common.dal.timers.RunnableTask;
import com.mobius.software.common.dal.timers.TaskCallback;
import com.mobius.software.common.dal.timers.WorkerPool;
import com.mobius.software.protocols.smpp.BaseBind;
import com.mobius.software.protocols.smpp.BaseBindResp;
import com.mobius.software.protocols.smpp.MessageStatus;
import com.mobius.software.protocols.smpp.Pdu;
import com.mobius.software.protocols.smpp.PduRequest;
import com.mobius.software.protocols.smpp.PduResponse;
import com.mobius.software.protocols.smpp.PduTranscoder;
import com.mobius.software.protocols.smpp.SmppBindType;
import com.mobius.software.protocols.smpp.Tlv;
import com.mobius.software.protocols.smpp.Unbind;
import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
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
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class SmppSessionImpl implements SmppServerSession, SmppSessionChannelListener
{
	public static final Logger logger = LogManager.getLogger(SmppSessionImpl.class);
	public static final Logger networkLogger = LogManager.getLogger("NETWORK");

	private final Type localType;
	private final AtomicInteger state;
	private final AtomicLong boundTime;
	private final SmppSessionConfiguration configuration;
	private final Channel channel;
	private SmppSessionHandler sessionHandler;
	private final SequenceNumber sequenceNumber;
	private final PduTranscoder transcoder;
	private SmppVersion interfaceVersion;

	private SmppServerHandler server;
	private WorkerPool workerPool;

	private BaseBindResp preparedBindResponse;

	private ConcurrentHashMap<Integer, RequestTimeoutInterface> pendingRequests = new ConcurrentHashMap<Integer, RequestTimeoutInterface>();
	private String id;

	public SmppSessionImpl(Type localType, SmppSessionConfiguration configuration, Channel channel, SmppServerHandler server, BaseBindResp preparedBindResponse, SmppVersion interfaceVersion, WorkerPool workerPool)
	{
		this(localType, configuration, channel, (SmppSessionHandler) null, workerPool);
		this.state.set(STATE_BINDING);
		this.server = server;
		this.preparedBindResponse = preparedBindResponse;
		this.interfaceVersion = interfaceVersion;
	}

	public SmppSessionImpl(Type localType, SmppSessionConfiguration configuration, Channel channel, SmppSessionHandler sessionHandler, WorkerPool workerPool)
	{
		this.localType = localType;
		this.state = new AtomicInteger(STATE_OPEN);
		this.configuration = configuration;
		this.channel = channel;
		this.boundTime = new AtomicLong(0);
		this.sessionHandler = (sessionHandler == null ? new EmptySmppSessionHandler() : sessionHandler);
		this.sequenceNumber = new SequenceNumber();
		this.workerPool = workerPool;
		this.transcoder = new PduTranscoder();

		this.server = null;

		this.preparedBindResponse = null;
		this.id = (new ObjectId()).toHexString();
	}

	public String getId()
	{
		return id;
	}

	@Override
	public SmppBindType getBindType()
	{
		return this.configuration.getType();
	}

	@Override
	public Type getLocalType()
	{
		return this.localType;
	}

	@Override
	public Type getRemoteType()
	{
		if (this.localType == Type.CLIENT)
			return Type.SERVER;
		else
			return Type.CLIENT;
	}

	protected void setBound()
	{
		this.state.set(STATE_BOUND);
		this.boundTime.set(System.currentTimeMillis());
	}

	@Override
	public long getBoundTime()
	{
		return this.boundTime.get();
	}

	@Override
	public String getStateName()
	{
		int s = this.state.get();
		if (s >= 0 || s < STATES.length)
			return STATES[s];
		else
			return "UNKNOWN (" + s + ")";
	}

	protected void setInterfaceVersion(SmppVersion value)
	{
		this.interfaceVersion = value;
	}

	@Override
	public SmppVersion getInterfaceVersion()
	{
		return this.interfaceVersion;
	}

	@Override
	public boolean areOptionalParametersSupported()
	{
		return (this.interfaceVersion.getValue() >= SmppVersion.VERSION_3_4.getValue());
	}

	@Override
	public boolean isOpen()
	{
		return (this.state.get() == STATE_OPEN);
	}

	@Override
	public boolean isBinding()
	{
		return (this.state.get() == STATE_BINDING);
	}

	@Override
	public boolean isBound()
	{
		return (this.state.get() == STATE_BOUND);
	}

	@Override
	public boolean isUnbinding()
	{
		return (this.state.get() == STATE_UNBINDING);
	}

	@Override
	public boolean isClosed()
	{
		return (this.state.get() == STATE_CLOSED);
	}

	@Override
	public SmppSessionConfiguration getConfiguration()
	{
		return this.configuration;
	}

	public Channel getChannel()
	{
		return this.channel;
	}

	public SequenceNumber getSequenceNumber()
	{
		return this.sequenceNumber;
	}

	public PduTranscoder getTranscoder()
	{
		return this.transcoder;
	}

	@Override
	public void serverReady(SmppSessionHandler sessionHandler)
	{
		this.sessionHandler = sessionHandler;
		try
		{
			this.sendResponsePdu(this.preparedBindResponse, this.id, new TaskCallback<Exception>() 
			{				
				@Override
				public void onSuccess() 
				{
				}
				
				@Override
				public void onError(Exception exception) 
				{
					logger.error("{}", exception);
				}
			});
		}
		catch (Exception e)
		{
			logger.error("{}", e);
		}

		this.setBound();
	}

	@SuppressWarnings("rawtypes")
	protected void assertValidRequest(PduRequest request) throws NullPointerException, RecoverablePduException, UnrecoverablePduException
	{
		if (request == null)
			throw new NullPointerException("PDU request cannot be null");
	}

	@SuppressWarnings("rawtypes")
	public void expired(PduRequest request)
	{
		int id = request.getSequenceNumber();
		RequestTimeoutInterface task = pendingRequests.remove(id);
		if (task != null)
			task.stop();

		logger.warn("expiring pdu:" + request.toString());
		switch (request.getCommandId())
		{
			case CMD_ID_UNBIND:
				close();
				break;
			case CMD_ID_BIND_RECEIVER:
			case CMD_ID_BIND_TRANSCEIVER:
			case CMD_ID_BIND_TRANSMITTER:
				sessionHandler.fireRecoverablePduException(new RecoverablePduException(request, "Bind failed"));
				break;
			default:
				this.sessionHandler.firePduRequestExpired(request);
				break;
		}
	}

	public void expireAll()
	{
		logger.warn("expiring all pdus");
		ConcurrentHashMap<Integer, RequestTimeoutInterface> oldRequests = pendingRequests;
		pendingRequests = new ConcurrentHashMap<Integer, RequestTimeoutInterface>();
		if (oldRequests != null)
		{
			Iterator<RequestTimeoutInterface> iterator = oldRequests.values().iterator();
			while (iterator.hasNext())
			{
				RequestTimeoutInterface task = iterator.next();
				if (task != null)
				{
					task.stop();
					logger.warn("expiring pdu:" + task.getRequest().toString());
					this.sessionHandler.firePduRequestExpired(task.getRequest());
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public void firePduReceived(Pdu pdu)
	{
		// networkLogger.info("received PDU: " + pdu);
		if (this.sessionHandler instanceof SmppSessionListener)
		{
			if (!((SmppSessionListener) this.sessionHandler).firePduReceived(pdu))
			{
				logger.info("recieved PDU discarded: " + pdu);
				return;
			}
		}

		RunnableTask processingTask = new RunnableTask(new Runnable()
		{
			@Override
			public void run()
			{
				if (pdu instanceof PduRequest)
				{
					PduRequest requestPdu = (PduRequest) pdu;
					try
					{
						sessionHandler.firePduRequestReceived(requestPdu);
					}
					catch (Exception ex)
					{
						logger.warn("An exception occured while firing pru request received: " + ex);
					}
				}
				else
				{
					PduResponse responsePdu = (PduResponse) pdu;
					int receivedPduSeqNum = pdu.getSequenceNumber();

					RequestTimeoutInterface timeoutTask = pendingRequests.remove(receivedPduSeqNum);
					if (timeoutTask != null)
					{
						timeoutTask.stop();
						switch (timeoutTask.getRequest().getCommandId())
						{
							case CMD_ID_UNBIND:
								close();
								break;
							case CMD_ID_BIND_RECEIVER:
							case CMD_ID_BIND_TRANSCEIVER:
							case CMD_ID_BIND_TRANSMITTER:
								BaseBindResp bindResponse = (BaseBindResp) responsePdu;
								if (bindResponse.getCommandStatus() != MessageStatus.OK)
								{
									sessionHandler.fireRecoverablePduException(new RecoverablePduException(timeoutTask.getRequest(), "Bind failed"));
									return;
								}

								Tlv scInterfaceVersion = bindResponse.getOptionalParameter(SmppVersion.TAG_SC_INTERFACE_VERSION);

								if (scInterfaceVersion == null)
									interfaceVersion = SmppVersion.VERSION_3_3;
								else
								{
									if (scInterfaceVersion.getValue() == null || scInterfaceVersion.getValue().length != 1)
									{
										logger.warn("Unable to convert sc_interface_version to a byte value");
										interfaceVersion = SmppVersion.VERSION_3_3;
									}
									else
									{
										SmppVersion tempInterfaceVersion = SmppVersion.fromInt(scInterfaceVersion.getValue()[0]);
										if (tempInterfaceVersion.getValue() >= SmppVersion.VERSION_3_4.getValue())
											interfaceVersion = SmppVersion.VERSION_3_4;
										else
											interfaceVersion = SmppVersion.VERSION_3_3;
									}
								}

								setBound();
								sessionHandler.fireExpectedPduResponseReceived(timeoutTask.getRequest(), responsePdu);
								break;
							default:
								sessionHandler.fireExpectedPduResponseReceived(timeoutTask.getRequest(), responsePdu);
								break;
						}
					}
					else
						sessionHandler.fireUnexpectedPduResponseReceived(responsePdu);
				}
			}

		}, this.id, "SmppSession-PduReceivedTask");

		this.workerPool.addTaskLast(processingTask);
	}

	public void fireExceptionThrown(Throwable t)
	{
		RunnableTask processingTask = new RunnableTask(new Runnable()
		{
			@Override
			public void run()
			{
				if (t instanceof UnrecoverablePduException)
					sessionHandler.fireUnrecoverablePduException((UnrecoverablePduException) t);
				else if (t instanceof RecoverablePduException)
					sessionHandler.fireRecoverablePduException((RecoverablePduException) t);
				else
				{
					if (isUnbinding() || isClosed())
						logger.debug("Unbind/close was requested, ignoring exception thrown: {}", t);
					else
						sessionHandler.fireUnknownThrowable(t);
				}
			}
		}, this.id, "SmppSession-ExceptionThrownTask");

		this.workerPool.addTaskLast(processingTask);
	}

	public void fireChannelClosed()
	{
		if (this.server != null)
			this.server.sessionDestroyed(this);

		if (isUnbinding() || isClosed())
			logger.debug("Unbind/close was requested, ignoring channelClosed event");
		else
			this.sessionHandler.fireChannelUnexpectedlyClosed();
	}

	@SuppressWarnings("rawtypes")
	public void sendRequest(PduRequest request, long timeoutInMillis, String requestID, TaskCallback<Exception> callback)
	{
		if (!request.hasSequenceNumberAssigned())
			request.setSequenceNumber(this.sequenceNumber.next());

		if (channel.isActive())
		{
			if (this.sessionHandler instanceof SmppSessionListener)
			{
				if (!((SmppSessionListener) this.sessionHandler).firePduDispatch(request))
				{
					logger.info("dispatched request PDU discarded: " + request);
					callback.onSuccess();
					return;
				}
			}

			String taskID = requestID;
			if(requestID == null)
				taskID = id;
			
			this.workerPool.addTaskLast(new RunnableTask(new Runnable()
			{
				@Override
				public void run()
				{

					ByteBuf buffer;
					try
					{
						buffer = transcoder.encode(request);
					}
					catch (UnrecoverablePduException | RecoverablePduException e)
					{
						logger.warn("An exception occured while encoding request: " + e);
						callback.onError(new SmppChannelException("An exception occured while encoding request: " + e));
						return;
					}

					RequestTimeoutTask timeoutTask = new RequestTimeoutTask(SmppSessionImpl.this, request, timeoutInMillis, "RequestTimeoutTask");
					pendingRequests.put(request.getSequenceNumber(), timeoutTask);
					workerPool.getPeriodicQueue().store(timeoutTask.getRealTimestamp(), timeoutTask);
					channel.writeAndFlush(buffer);
					callback.onSuccess();
				}
			}, taskID, "SmppSession-SendRequestTask"));
		}
		else
		{
			callback.onError(new SmppChannelException("Channel is not active"));
			return;
		}
	}

	@SuppressWarnings("rawtypes")
	public void sendBindRequest(PduRequest request, long timeoutInMillis) throws SmppChannelException
	{
		if (!request.hasSequenceNumberAssigned())
			request.setSequenceNumber(this.sequenceNumber.next());

		if (channel.isActive())
		{
			if (this.sessionHandler instanceof SmppSessionListener)
			{
				if (!((SmppSessionListener) this.sessionHandler).firePduDispatch(request))
				{
					logger.info("dispatched request PDU discarded: " + request);
					return;
				}
			}

			this.workerPool.addTaskLast(new RunnableTask(new Runnable()
			{
				@Override
				public void run()
				{
					ByteBuf buffer;
					try
					{
						buffer = transcoder.encode(request);
					}
					catch (UnrecoverablePduException | RecoverablePduException e)
					{
						logger.warn("An exception occured while encoding bind request: " + e);
						return;
					}

					RequestBindTimeoutTask timeoutTask = new RequestBindTimeoutTask(SmppSessionImpl.this, request, timeoutInMillis, "RequestBindTimeoutTask");
					pendingRequests.put(request.getSequenceNumber(), timeoutTask);
					workerPool.getPeriodicQueue().store(timeoutTask.getRealTimestamp(), timeoutTask);
					channel.writeAndFlush(buffer);
				}
			}, this.id, "SmppSession-SendBindRequestTask"));
		}
		else
			throw new SmppChannelException("Channel is not active");
	}

	@SuppressWarnings("rawtypes")
	public void bind(BaseBind request, long timeoutInMillis, TaskCallback<Exception> callback) throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException
	{
		if (this.channel.isActive())
		{
			this.state.set(STATE_BINDING);
			sendRequest(request, timeoutInMillis, this.id, callback);
		}
		else
		{
			logger.info("Session channel is closed, not going to bind");
			throw new SmppChannelException("Channel is not active");
		}
	}

	public void unbind(long timeoutInMillis)
	{
		if (this.channel.isActive())
		{
			Unbind unbind = new Unbind();
			this.state.set(STATE_UNBINDING);
			try
			{
				sendRequest(unbind, timeoutInMillis, this.id, new TaskCallback<Exception>() 
				{					
					@Override
					public void onSuccess() 
					{						
					}
					
					@Override
					public void onError(Exception exception) 
					{
						logger.error("An error occured while unbinding, going to close the session, " + exception);
						close();
					}
				});
			}
			catch (Exception ex)
			{
				logger.error("An error occured while unbinding, going to close the session, " + ex);
				close();
			}
		}
		else
			logger.info("Session channel is already closed, not going to unbind");
	}

	public void sendRequestPdu(PduRequest<?> request, String requestID, TaskCallback<Exception> callback)
	{
		sendRequest(request, configuration.getRequestExpiryTimeout(), requestID, callback);
	}

	public void sendResponsePdu(PduResponse response, String responseID, TaskCallback<Exception> callback)
	{
		if (!response.hasSequenceNumberAssigned())
		{
			response.setSequenceNumber(this.sequenceNumber.next());
		}

		if (this.sessionHandler instanceof SmppSessionListener)
		{
			if (!((SmppSessionListener) this.sessionHandler).firePduDispatch(response))
			{
				logger.info("dispatched response PDU discarded: " + response);
				callback.onSuccess();
				return;
			}
		}

		if (channel.isActive())
		{
			String taskID = responseID;
			if(responseID==null)
				taskID = id;
			
			this.workerPool.addTaskLast(new RunnableTask(new Runnable()
			{
				@Override
				public void run()
				{
					ByteBuf buffer;
					try
					{
						buffer = transcoder.encode(response);
					}
					catch (UnrecoverablePduException | RecoverablePduException e)
					{
						logger.warn("An exception occured while encoding response: " + e);
						return;
					}

					channel.writeAndFlush(buffer);
					callback.onSuccess();
				}
			}, taskID, "SmppSession-SendResponsePduTask"));
		}
		else
		{
			callback.onError(new SmppChannelException("Channel is not active"));
			return;
		}
	}

	public void close()
	{
		if (channel.isActive())
		{
			this.state.set(STATE_UNBINDING);
			if (channel.close().awaitUninterruptibly(1000))
				logger.info("Successfully closed");
			else
				logger.warn("Unable to cleanly close channel");
		}

		this.state.set(STATE_CLOSED);
	}

	public void passiveClose()
	{
		if (channel.isActive())
		{
			if (channel.close().awaitUninterruptibly(1000))
				logger.info("Successfully passively closed");
			else
				logger.warn("Unable to cleanly close channel");
		}
	}

	public void destroy()
	{
		close();
		this.sessionHandler = null;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		SmppSessionImpl other = (SmppSessionImpl) obj;
		if (id == null)
		{
			if (other.id != null)
				return false;
		}
		else if (!id.equals(other.id))
			return false;

		return true;
	}
}