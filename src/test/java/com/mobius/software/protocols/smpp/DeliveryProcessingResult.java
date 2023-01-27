package com.mobius.software.protocols.smpp;

public class DeliveryProcessingResult 
{
	private Long ts;
	private Integer deliveryErrorCode;
	private DeliveryStatus deliveryStatus;
	
	public DeliveryProcessingResult() 
	{
		
	}
	
	public DeliveryProcessingResult(Long ts, Integer deliveryErrorCode, DeliveryStatus deliveryStatus) 
	{
		this.ts = ts;
		this.deliveryErrorCode = deliveryErrorCode;
		this.deliveryStatus = deliveryStatus;
	}

	public Long getTs() 
	{
		return ts;
	}

	public void setTs(Long ts) 
	{
		this.ts = ts;
	}

	public Integer getDeliveryErrorCode() 
	{
		return deliveryErrorCode;
	}

	public void setDeliveryErrorCode(Integer deliveryErrorCode) 
	{
		this.deliveryErrorCode = deliveryErrorCode;
	}

	public DeliveryStatus getDeliveryStatus() 
	{
		return deliveryStatus;
	}

	public void setDeliveryStatus(DeliveryStatus deliveryStatus) 
	{
		this.deliveryStatus = deliveryStatus;
	}
}