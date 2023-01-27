package com.mobius.software.protocols.smpp;

public class StatusRequestWrapper 
{
	private String messageID;
	private AsyncCallback<DeliveryProcessingResult> response;
	
	public StatusRequestWrapper(String messageID,AsyncCallback<DeliveryProcessingResult> response) 
	{
		this.messageID = messageID;
		this.response = response;
	}

	public String getMessageID() 
	{
		return messageID;
	}
	
	public void setMessageID(String messageID) 
	{
		this.messageID = messageID;
	}
	
	public AsyncCallback<DeliveryProcessingResult> getResponse() 
	{
		return response;
	}
	
	public void setResponse(AsyncCallback<DeliveryProcessingResult> response) 
	{
		this.response = response;
	}
}