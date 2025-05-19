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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;

import com.mobius.software.common.dal.timers.PeriodicQueuedTasks;
import com.mobius.software.common.dal.timers.TaskCallback;
import com.mobius.software.common.dal.timers.Timer;
import com.mobius.software.protocols.smpp.channel.SmppServerSession;
import com.mobius.software.protocols.smpp.channel.SmppSession;
import com.mobius.software.protocols.smpp.channel.SmppSessionHandler;
import com.mobius.software.protocols.smpp.channel.SmppSessionImpl;
import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;
import com.mobius.software.protocols.smpp.server.HealthCheckTimer;
/**
 * this is just a sample server session handler, real one should be consuming more message parameters 
 * and not only byte arrays and addresses 
 **/
public class TestSmppServerSessionHandler implements SmppSessionHandler
{
	public static Logger logger=LogManager.getLogger(TestSmppServerSessionHandler.class);
	public static Logger debugLogger=LogManager.getLogger("DEBUG");
		
	private static SimpleDateFormat absoluteDateFormat=new SimpleDateFormat("yyMMddHHmmss");
	
	private SmppServerSession serverSession;
	private HealthCheckTimer healthCheckTimer;
	private ConnectionListener connectionListener;
	private PeriodicQueuedTasks<Timer> timersQueue;
	
	public TestSmppServerSessionHandler(ConnectionListener connectionListener,HealthCheckTimer healthCheckTimer,SmppServerSession serverSession,PeriodicQueuedTasks<Timer> timersQueue)
	{
		this.connectionListener=connectionListener;
		this.healthCheckTimer=healthCheckTimer;
		this.serverSession=serverSession;
		this.timersQueue=timersQueue;
	}
	
	@Override
	public void fireChannelUnexpectedlyClosed() 
	{		
		((SmppSessionImpl)serverSession).expireAll();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void firePduRequestReceived(PduRequest pduRequest) 
	{
		if(healthCheckTimer!=null)
		{
			healthCheckTimer.restart();
			timersQueue.store(healthCheckTimer.getRealTimestamp(), healthCheckTimer);			
		}
		
		String[] segments=serverSession.getConfiguration().getName().split("_");
		switch(pduRequest.getCommandId())
		{
			case CMD_ID_ENQUIRE_LINK:
				connectionListener.heartbeatReceived(segments[0]);
				EnquireLink enquire=(EnquireLink)pduRequest;
				EnquireLinkResp enquireResp=enquire.createResponse();
				enquireResp.setCommandStatus(MessageStatus.OK);
				sendResponse(enquireResp);
        		break;
			case CMD_ID_UNBIND:
				Unbind unbind=(Unbind)pduRequest;
				UnbindResp unbindResp=unbind.createResponse();
				unbindResp.setCommandStatus(MessageStatus.OK);
				sendResponse(unbindResp);
        		break;
			case CMD_ID_SUBMIT_SM:
				SubmitSm submitSm=(SubmitSm)pduRequest;
				
				if(submitSm.getSourceAddress()==null)
				{
					SubmitSmResp submitSmResp=submitSm.createResponse();
					submitSmResp.setCommandStatus(MessageStatus.INVSRCADR);
					sendResponse(submitSmResp);
            		return;
				}
				
				if(submitSm.getDestAddress()==null)
				{
					SubmitSmResp submitSmResp=submitSm.createResponse();
					submitSmResp.setCommandStatus(MessageStatus.INVDSTADR);
					sendResponse(submitSmResp);
            		return;
				}
				
				if(submitSm.getScheduleDeliveryTime()!=null)
				{
					try
					{
						SmppHelper.parseSmppDate(submitSm.getScheduleDeliveryTime());
					}					
					catch(ParseException ex)
					{
						SubmitSmResp submitSmResp=submitSm.createResponse();
						submitSmResp.setCommandStatus(MessageStatus.INVSCHED);
						sendResponse(submitSmResp);
	            		return;
					}
				}
				
				if(submitSm.getValidityPeriod()!=null)
				{
					try
					{
						SmppHelper.parseSmppDate(submitSm.getValidityPeriod());
					}					
					catch(ParseException ex)
					{
						SubmitSmResp submitSmResp=submitSm.createResponse();
						submitSmResp.setCommandStatus(MessageStatus.INVEXPIRY);
						sendResponse(submitSmResp);
	            		return;
					}
				}
				
				byte[] data = null;
				byte[] udh = null;
				if(submitSm.getShortMessage()!=null && submitSm.getShortMessage().length>0)
						data=submitSm.getShortMessage();
				
		        if (data==null) 
		        {
		            Tlv messagePaylod = submitSm.getOptionalParameter(SmppHelper.TAG_MESSAGE_PAYLOAD);
		            if (messagePaylod != null) 
		                data = messagePaylod.getValue();            
		        }
		        
		        if(data!=null)
		        {
					boolean isUdh = SmppHelper.isUserDataHeaderIndicatorEnabled(submitSm.getEsmClass());
					if (isUdh) 
					{
					    byte[] userDataHeader = SmppHelper.getShortMessageUserDataHeader(data);
					    byte[] messageBytes = SmppHelper.getShortMessageUserData(data);					    
					    data = messageBytes;
					    udh = userDataHeader;
					}
		        }
		        
		        if(submitSm.getDefaultMsgId()==0 && (data==null || data.length==0 || Encoding.fromInt(submitSm.getDataCoding())==null))
			    {
		        	SubmitSmResp submitSmResp=submitSm.createResponse();
					submitSmResp.setCommandStatus(MessageStatus.INVMSGLEN);
					sendResponse(submitSmResp);
            		return;
		        }
		        
				List<String> to=new ArrayList<String>();
				to.add(submitSm.getDestAddress().getAddress());
				
				if(debugLogger.isDebugEnabled())
					debugLogger.debug("Received SUBMIT_SM:" + submitSm);
				
				connectionListener.messageReceived(segments[0], to, data, udh, null, new ResponseCallback(pduRequest));
				break;				
			case CMD_ID_DATA_SM:
				DataSm dataSm=(DataSm)pduRequest;
				
				if(dataSm.getSourceAddress()==null)
				{
					DataSmResp dataSmResp=dataSm.createResponse();
					dataSmResp.setCommandStatus(MessageStatus.INVSRCADR);
					sendResponse(dataSmResp);
            		return;
				}
				
				if(dataSm.getDestAddress()==null)
				{
					DataSmResp dataSmResp=dataSm.createResponse();
					dataSmResp.setCommandStatus(MessageStatus.INVDSTADR);
					sendResponse(dataSmResp);
            		return;
				}
				
				if(dataSm.getScheduleDeliveryTime()!=null)
				{
					try
					{
						SmppHelper.parseSmppDate(dataSm.getScheduleDeliveryTime());
					}					
					catch(ParseException ex)
					{
						DataSmResp dataSmResp=dataSm.createResponse();
						dataSmResp.setCommandStatus(MessageStatus.INVSCHED);
						sendResponse(dataSmResp);
	            		return;
					}
				}
				
				if(dataSm.getValidityPeriod()!=null)
				{
					try
					{
						SmppHelper.parseSmppDate(dataSm.getValidityPeriod());
					}					
					catch(ParseException ex)
					{
						DataSmResp dataSmResp=dataSm.createResponse();
						dataSmResp.setCommandStatus(MessageStatus.INVEXPIRY);
						sendResponse(dataSmResp);
	            		return;
					}
				}
				
				data = null;
				udh = null;
				if(dataSm.getShortMessage()!=null && dataSm.getShortMessage().length>0)
						data=dataSm.getShortMessage();
				
		        if (data==null) 
		        {
		            Tlv messagePaylod = dataSm.getOptionalParameter(SmppHelper.TAG_MESSAGE_PAYLOAD);
		            if (messagePaylod != null) 
		                data = messagePaylod.getValue();            
		        }
		        
		        if(data!=null)
		        {
					boolean isUdh = SmppHelper.isUserDataHeaderIndicatorEnabled(dataSm.getEsmClass());
					if (isUdh) 
					{
					    byte[] userDataHeader = SmppHelper.getShortMessageUserDataHeader(data);
					    byte[] messageBytes = SmppHelper.getShortMessageUserData(data);
					    data = messageBytes;
					    udh = userDataHeader;
					}
		        }
		        
		        if(dataSm.getDefaultMsgId()==0 && (data==null || data.length==0 || Encoding.fromInt(dataSm.getDataCoding())==null))
				{
		        	DataSmResp dataSmResp=dataSm.createResponse();
					dataSmResp.setCommandStatus(MessageStatus.INVMSGLEN);
					sendResponse(dataSmResp);
            		return;
		        }
				
				to=new ArrayList<String>();
				to.add(dataSm.getDestAddress().getAddress());
				
				if(debugLogger.isDebugEnabled())
					debugLogger.debug("Received DATA_SM:" + dataSm);
				
				connectionListener.messageReceived(segments[0], to, data, udh, null, new ResponseCallback(pduRequest));
				break;
			case CMD_ID_SUBMIT_MULTI:
				SubmitMulti submitMulti=(SubmitMulti)pduRequest;
				
				if(submitMulti.getSourceAddress()==null)
				{
					SubmitMultiResp submitMultiResp=submitMulti.createResponse();
					submitMultiResp.setCommandStatus(MessageStatus.INVSRCADR);
					sendResponse(submitMultiResp);
            		return;
				}
				
				Boolean hasMailingList=false;
				if(submitMulti.getDestDestributionListName()!=null)
				{
					if(submitMulti.getDestDestributionListName().size()>1)
					{
						SubmitMultiResp submitMultiResp=submitMulti.createResponse();
						submitMultiResp.setCommandStatus(MessageStatus.INVDSTADR);
						sendResponse(submitMultiResp);
	            		return;
					}
					else if(submitMulti.getDestDestributionListName().size()==1)
					{
						hasMailingList=true;
						try
						{
							new ObjectId(submitMulti.getDestDestributionListName().get(0));							
						}
						catch(Exception ex)
						{
							SubmitMultiResp submitMultiResp=submitMulti.createResponse();
							submitMultiResp.setCommandStatus(MessageStatus.INVDSTADR);
							sendResponse(submitMultiResp);
		            		return;
						}
					}
				}
				
				if((submitMulti.getDestAddresses()==null || submitMulti.getDestAddresses().size()==0) && !hasMailingList)
				{
					SubmitMultiResp submitMultiResp=submitMulti.createResponse();
					submitMultiResp.setCommandStatus(MessageStatus.INVDSTADR);
					sendResponse(submitMultiResp);
            		return;
				}
				
				if(submitMulti.getDestAddresses()!=null && submitMulti.getDestAddresses().size()>0)
				{
					to=new ArrayList<String>();
					for(Address address:submitMulti.getDestAddresses())
						to.add(address.getAddress());
				}
				else
					to=null;
				
				if(submitMulti.getScheduleDeliveryTime()!=null)
				{
					try
					{
						SmppHelper.parseSmppDate(submitMulti.getScheduleDeliveryTime());
					}					
					catch(ParseException ex)
					{
						SubmitMultiResp submitMultiResp=submitMulti.createResponse();
						submitMultiResp.setCommandStatus(MessageStatus.INVSCHED);
						sendResponse(submitMultiResp);
	            		return;
					}
				}
				
				if(submitMulti.getValidityPeriod()!=null)
				{
					try
					{
						SmppHelper.parseSmppDate(submitMulti.getValidityPeriod());
					}					
					catch(ParseException ex)
					{
						SubmitMultiResp submitMultiResp=submitMulti.createResponse();
						submitMultiResp.setCommandStatus(MessageStatus.INVEXPIRY);
						sendResponse(submitMultiResp);
	            		return;
					}
				}
				
				data = null;
				udh = null;
				if(submitMulti.getShortMessage()!=null && submitMulti.getShortMessage().length>0)
						data=submitMulti.getShortMessage();
				
		        if (data==null) 
		        {
		            Tlv messagePaylod = submitMulti.getOptionalParameter(SmppHelper.TAG_MESSAGE_PAYLOAD);
		            if (messagePaylod != null) 
		                data = messagePaylod.getValue();            
		        }
		        
		        if(data!=null)
		        {
					boolean isUdh = SmppHelper.isUserDataHeaderIndicatorEnabled(submitMulti.getEsmClass());
					if (isUdh) 
					{
					    byte[] userDataHeader = SmppHelper.getShortMessageUserDataHeader(data);
					    byte[] messageBytes = SmppHelper.getShortMessageUserData(data);
					    data = messageBytes;
					    udh = userDataHeader;
					}
		        }
		        
		        if(submitMulti.getDefaultMsgId()==0 && (data==null || data.length==0 || Encoding.fromInt(submitMulti.getDataCoding())==null))
				{
		        	SubmitMultiResp submitMultiResp=submitMulti.createResponse();
					submitMultiResp.setCommandStatus(MessageStatus.INVMSGLEN);
					sendResponse(submitMultiResp);
            		return;
		        }
		        
				if(debugLogger.isDebugEnabled())
					debugLogger.debug("Received SUBMIT_SM_MULTI:" + submitMulti);
				
				connectionListener.messageReceived(segments[0], to, data, udh, null, new ResponseCallback(pduRequest));
				break;
            case CMD_ID_DELIVER_SM:
            	DeliverSm deliverSm=(DeliverSm)pduRequest;
            	if(!SmppHelper.isMessageTypeAnyDeliveryReceipt(deliverSm.getEsmClass()))
            	{
            		//for deliver SM we only handle delivery reports
            		if(deliverSm.getSourceAddress()==null)
    				{
    					DeliverSmResp deliverSmResp=deliverSm.createResponse();
    					deliverSmResp.setCommandStatus(MessageStatus.INVSRCADR);
    					sendResponse(deliverSmResp);
                		return;
    				}
    				
    				if(deliverSm.getDestAddress()==null)
    				{
    					DeliverSmResp deliverSmResp=deliverSm.createResponse();
    					deliverSmResp.setCommandStatus(MessageStatus.INVDSTADR);
    					sendResponse(deliverSmResp);
    					return;
    				}
    				
    				if(deliverSm.getScheduleDeliveryTime()!=null)
    				{
    					try
    					{
    						SmppHelper.parseSmppDate(deliverSm.getScheduleDeliveryTime());
    					}					
    					catch(ParseException ex)
    					{
    						DeliverSmResp deliverSmResp=deliverSm.createResponse();
    						deliverSmResp.setCommandStatus(MessageStatus.INVSCHED);
    						sendResponse(deliverSmResp);
    	            		return;
    					}
    				}
    				
    				if(deliverSm.getValidityPeriod()!=null)
    				{
    					try
    					{
    						SmppHelper.parseSmppDate(deliverSm.getValidityPeriod());
    					}					
    					catch(ParseException ex)
    					{
    						DeliverSmResp deliverSmResp=deliverSm.createResponse();
    						deliverSmResp.setCommandStatus(MessageStatus.INVEXPIRY);
    						sendResponse(deliverSmResp);
    	            		return;
    					}
    				}
    				
    				data = null;
    				udh = null;
    				if(deliverSm.getShortMessage()!=null && deliverSm.getShortMessage().length>0)
    						data=deliverSm.getShortMessage();
    				
    		        if (data==null) 
    		        {
    		            Tlv messagePaylod = deliverSm.getOptionalParameter(SmppHelper.TAG_MESSAGE_PAYLOAD);
    		            if (messagePaylod != null) 
    		                data = messagePaylod.getValue();            
    		        }
    		        
    		        if(data!=null)
    		        {
    					boolean isUdh = SmppHelper.isUserDataHeaderIndicatorEnabled(deliverSm.getEsmClass());
    					if (isUdh) 
    					{
    					    byte[] userDataHeader = SmppHelper.getShortMessageUserDataHeader(data);
    					    byte[] messageBytes = SmppHelper.getShortMessageUserData(data);					    
    					    data = messageBytes;
    					    udh = userDataHeader;
    					}
    		        }
    		        
    		        if(deliverSm.getDefaultMsgId()==0 && (data==null || data.length==0 || Encoding.fromInt(deliverSm.getDataCoding())==null))
    		        {
    		        	DeliverSmResp deliverSmResp=deliverSm.createResponse();
    		        	deliverSmResp.setCommandStatus(MessageStatus.INVMSGLEN);
    					sendResponse(deliverSmResp);
                		return;
    		        }
    		        
    				to=new ArrayList<String>();
    				to.add(deliverSm.getDestAddress().getAddress());
    				
    				if(debugLogger.isDebugEnabled())
    					debugLogger.debug("Received DELIVER_SM:" + deliverSm);
    				
    				connectionListener.messageReceived(segments[0], to, data, udh, null, new ResponseCallback(pduRequest));
    			}
            	else
            	{
            		ReportData reportData = ReportData.parseDeliveryReceipt(deliverSm);
					
            		if(deliverSm.getSourceAddress()==null)
					{
						DeliverSmResp deliverSmResp=deliverSm.createResponse();
						deliverSmResp.setCommandStatus(MessageStatus.INVSRCADR);
						sendResponse(deliverSmResp);
	            		return;
					}
					
					if(deliverSm.getDestAddress()==null)
					{
						DeliverSmResp deliverSmResp=deliverSm.createResponse();
						deliverSmResp.setCommandStatus(MessageStatus.INVDSTADR);
						sendResponse(deliverSmResp);
	            		return;
					}
					
					data = null;
    				udh = null;
    				if(deliverSm.getShortMessage()!=null && deliverSm.getShortMessage().length>0)
    						data=deliverSm.getShortMessage();
    				
    		        if (data==null) 
    		        {
    		            Tlv messagePaylod = deliverSm.getOptionalParameter(SmppHelper.TAG_MESSAGE_PAYLOAD);
    		            if (messagePaylod != null) 
    		                data = messagePaylod.getValue();            
    		        }
    		        
    		        if(data!=null)
    		        {
    					boolean isUdh = SmppHelper.isUserDataHeaderIndicatorEnabled(deliverSm.getEsmClass());
    					if (isUdh) 
    					{
    					    byte[] userDataHeader = SmppHelper.getShortMessageUserDataHeader(data);
    					    byte[] messageBytes = SmppHelper.getShortMessageUserData(data);					    
    					    data = messageBytes;
    					    udh = userDataHeader;
    					}
    		        }
    		        
    		        to=new ArrayList<String>();
					to.add(deliverSm.getDestAddress().getAddress());
    				
					if(debugLogger.isDebugEnabled())
						debugLogger.debug("Received DELIVER_SM:" + deliverSm);
					
					connectionListener.messageReceived(segments[0], to, data, udh, reportData, new ResponseCallback(pduRequest));					
            	}
				break;
			case CMD_ID_QUERY_SM:
            	QuerySm querySM=(QuerySm)pduRequest;
            	if(querySM.getMessageId()==null)
            	{
            		QuerySmResp response=querySM.createResponse();
            		response.setCommandStatus(MessageStatus.INVMSGID);
            		sendResponse(response);
            		return;
            	}
            	
            	if(debugLogger.isDebugEnabled())
            		debugLogger.debug("Received STATUS_SM:" + querySM.getMessageId());
				
            	connectionListener.statusReceived(segments[0], querySM.getMessageId(), new QueryCallback(querySM));
            	break;
			default:
				break;
		}				
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void firePduRequestExpired(PduRequest pduRequest) 
	{
		String[] segments=serverSession.getConfiguration().getName().split("_");		
		switch(pduRequest.getCommandId())
		{
			case CMD_ID_SUBMIT_SM:
			case CMD_ID_DATA_SM:
			case CMD_ID_SUBMIT_MULTI:
				String original=(String)pduRequest.getReferenceObject();
				connectionListener.timeoutReceived(segments[0], original);
				break;
			case CMD_ID_DELIVER_SM:
				original=(String)pduRequest.getReferenceObject();
				connectionListener.timeoutReceived(segments[0], original);
				break;
			default:
				break;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void fireExpectedPduResponseReceived(PduRequest pduRequest,PduResponse pduResponse) 
	{		
		String remoteMessageID=null;
		String messageType;
		switch (pduResponse.getCommandId()) 
		{
			case CMD_ID_DATA_SM_RESP:
				messageType="DATA_SM";
				remoteMessageID=((DataSmResp)pduResponse).getMessageId();
				if(remoteMessageID==null)
					remoteMessageID=(new ObjectId()).toHexString();
				break;
			case CMD_ID_SUBMIT_SM_RESP:
				messageType="SUBMIT_SM";
				remoteMessageID=((SubmitSmResp)pduResponse).getMessageId();
				if(remoteMessageID==null)
					remoteMessageID=(new ObjectId()).toHexString();
				break;
			case CMD_ID_DELIVER_SM_RESP:
				messageType="DELIVERY_SM";
				//remoteMessageID=((Message)pduRequest.getReferenceObject()).getRemoteMessageID();
				break;
			default:
				return;
		}
		
		String[] segments=serverSession.getConfiguration().getName().split("_");
		MessageStatus status=pduResponse.getCommandStatus();
		
		if(debugLogger.isDebugEnabled())
			debugLogger.debug("Received Response for " + messageType + ",Message ID:" + ((String)pduRequest.getReferenceObject()) + ",Status:" + status);
		
		connectionListener.responseReceived(segments[0], ((String)pduRequest.getReferenceObject()), remoteMessageID, status);
	}

	@Override
	public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) 
	{
		logger.warn("Unexpected response received:" + pduResponse);
	}

	@Override
	public void fireUnrecoverablePduException(UnrecoverablePduException e) 
	{
		logger.error("Unrecoverable pdu exception:" + e.getMessage(),e);
	}

	@Override
	public void fireRecoverablePduException(RecoverablePduException e) 
	{
		String[] segments=serverSession.getConfiguration().getName().split("_");
		Pdu partialPdu = e.getPartialPdu();
		switch(partialPdu.getCommandId())
		{
			case CMD_ID_SUBMIT_SM:
			case CMD_ID_DATA_SM:
			case CMD_ID_DELIVER_SM:		
				String originalMessageID=(String)partialPdu.getReferenceObject();
				connectionListener.responseReceived(segments[0], originalMessageID, null, MessageStatus.SUBMITFAIL);
				break;
			default:
				break;
		}
	}

	@Override
	public void fireUnknownThrowable(Throwable t) 
	{
		((SmppSessionImpl)serverSession).expireAll();
	}
	
	private void sendResponse(PduResponse response)
	{
		serverSession.sendResponsePdu(response, ((SmppSessionImpl) serverSession).getId(), new TaskCallback<Exception>()
		{
			@Override
			public void onSuccess()
			{
			}

			@Override
			public void onError(Exception exception)
			{
				logger.error("An error occured while sending response," + exception.getMessage() + ",response:" + response);
			}
		});
	}
	
	private class ResponseCallback implements AsyncCallback<RequestProcessingResult>
	{
		private PduRequest<?> request;
		
		public ResponseCallback(PduRequest<?> request)
		{
			this.request=request;
		}
		
		@Override
		public void onResult(RequestProcessingResult result, Throwable t) 
		{
			PduResponse response=request.createResponse();
			if(result!=null)
				response.setCommandStatus(result.getMessageStatus());
			else
				response.setCommandStatus(MessageStatus.SYSERR);				
			
			String messageType=null;
			String messageID=null;
			if(result.getMessageIDs()!=null && result.getMessageIDs().size()!=0)
			{
				switch(request.getCommandId())
				{
					case CMD_ID_SUBMIT_MULTI:
						messageType="SUBMIT_MULTI";
						messageID=result.getMessageIDs().get(0);
						((SubmitMultiResp)response).setMessageId(result.getMessageIDs().get(0));
						break;
					case CMD_ID_SUBMIT_SM:
						messageType="SUBMIT_SM";
						messageID=result.getMessageIDs().get(0);
						((SubmitSmResp)response).setMessageId(result.getMessageIDs().get(0));
						break;
					case CMD_ID_DATA_SM:
						messageType="DATA_SM";
						messageID=result.getMessageIDs().get(0);
						((DataSmResp)response).setMessageId(result.getMessageIDs().get(0));
						break;
					default:
						break;
				}
			}
			
			if(debugLogger.isDebugEnabled())
				debugLogger.debug("Sending Response for " + messageType + ",Message ID:" + messageID + ",Status:" + result.getMessageStatus());
			
			sendResponse(response);
		}		
	}
	
	private class QueryCallback implements AsyncCallback<DeliveryProcessingResult>
	{
		QuerySm querySM;
		public QueryCallback(QuerySm querySM)
		{
			this.querySM=querySM;
		}
		
		@Override
		public void onResult(DeliveryProcessingResult result, Throwable t) 
		{
			QuerySmResp response=querySM.createResponse();
			if(result!=null)
			{
				response.setCommandStatus(MessageStatus.OK);
				if(result.getDeliveryStatus()!=null)
				{
					response.setMessageState((byte)result.getDeliveryStatus().getValue());
					Date finalDate=new Date();
					finalDate.setTime(result.getTs());
					response.setFinalDate(absoluteDateFormat.format(finalDate) + "000+");
				}
				
				if(result.getDeliveryErrorCode()!=null)
					response.setErrorCode(result.getDeliveryErrorCode().byteValue());								
			}
			else
			{				
				if(t!=null)
					response.setCommandStatus(MessageStatus.SYSERR);
				else
					response.setCommandStatus(MessageStatus.INVMSGID);											
			}			
			
			if(debugLogger.isDebugEnabled())
				debugLogger.debug("Sending Response for QUERY_SM,RESPONSE:" + result);
			sendResponse(response);
		}		
	}

	@Override
	public void setSession(SmppSession session) 
	{
		//not needed here
	}
}