package com.mobius.software.protocols.smpp;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentHashMap;

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
public class ReportData 
{
	private String messageID;
	private Integer errorCode;
	private byte[] data;
	private Encoding encoding;
	private DeliveryStatus deliveryStatus;
	
	public ReportData()
	{
		
	}
	
	public ReportData(String messageID, Integer errorCode, byte[] data, Encoding encoding,DeliveryStatus deliveryStatus) 
	{
		this.messageID = messageID;
		this.errorCode = errorCode;
		this.data = data;
		this.encoding = encoding;
		this.deliveryStatus = deliveryStatus;
	}

	public String getMessageID() 
	{
		return messageID;
	}

	public void setMessageID(String messageID) 
	{
		this.messageID = messageID;
	}

	public Integer getErrorCode() 
	{
		return errorCode;
	}

	public void setErrorCode(Integer errorCode) 
	{
		this.errorCode = errorCode;
	}

	public byte[] getData() 
	{
		return data;
	}

	public void setData(byte[] data) 
	{
		this.data = data;
	}

	public Encoding getEncoding() 
	{
		return encoding;
	}

	public void setEncoding(Encoding encoding) 
	{
		this.encoding = encoding;
	}

	public DeliveryStatus getDeliveryStatus() 
	{
		return deliveryStatus;
	}

	public void setDeliveryStatus(DeliveryStatus deliveryStatus) 
	{
		this.deliveryStatus = deliveryStatus;
	}	
	
	public static ReportData parseDeliveryReceipt(DeliverSm deliverSM) {
		ReportData reportData=new ReportData();
		reportData.setEncoding(Encoding.fromInt(deliverSM.getDataCoding()));
		
		Tlv tlv = deliverSM.getOptionalParameter(SmppHelper.TAG_RECEIPTED_MSG_ID);
        if (tlv != null && tlv.calculateLength()>0) 
        {
        	String val;
        	if(tlv.getValue()[tlv.getValue().length-1]==0x00)
        		val = new String(tlv.getValue(),0,tlv.getValue().length-1);
        	else
        		val = new String(tlv.getValue());
        	
        	reportData.setMessageID(val);
        }
        
        tlv = deliverSM.getOptionalParameter(SmppHelper.TAG_MSG_STATE);
        if (tlv != null && tlv.getValue() != null && tlv.getValue().length ==1) 
        	reportData.setDeliveryStatus(DeliveryStatus.fromInt(tlv.getValue()[0] & 0x0FF));                            
        
        String msg=null;
        try
        {
        	msg=SmppHelper.parseShortMessageText(deliverSM);
        }
        catch(UnsupportedEncodingException ex)
        {
        	
        }
        
        if (msg == null || msg.length() < 50)
			return reportData;
		
        String[] namesList = new String[] { SmppHelper.DELIVERY_ACK_ID, SmppHelper.DELIVERY_ACK_SUB, SmppHelper.DELIVERY_ACK_DLVRD, SmppHelper.DELIVERY_ACK_SUBMIT_DATE,
        		SmppHelper.DELIVERY_ACK_DONE_DATE, SmppHelper.DELIVERY_ACK_STAT, SmppHelper.DELIVERY_ACK_ERR, SmppHelper.DELIVERY_ACK_TEXT, };

        String lcMessage=msg.toLowerCase();
        int pos = 0;
        ConcurrentHashMap<String,String> values = new ConcurrentHashMap<String,String>();
        String lastField=SmppHelper.DELIVERY_ACK_ID;
        for (int i1 = 0; i1 < namesList.length; i1++) {  
        	String fieldName = namesList[i1];
            int newPos = lcMessage.indexOf(fieldName, pos);
            if (newPos < 0) {
                if (fieldName.equals(SmppHelper.DELIVERY_ACK_TEXT))
                	break;
                else
                	return reportData;
            }

            if (i1 == 0) 
            {
                if (newPos != 0)
                	return reportData;	
            } 
            else 
            {
                if (newPos >= 0) 
                	values.put(namesList[i1-1], msg.substring(pos, newPos));
            }
            
            lastField = fieldName;
    		pos = newPos + fieldName.length();
        }
        
        values.put(lastField, msg.substring(pos));  
    	String idVal = values.get(SmppHelper.DELIVERY_ACK_ID);
        String statusVal = values.get(SmppHelper.DELIVERY_ACK_STAT);
        String errorVal = values.get(SmppHelper.DELIVERY_ACK_ERR);
        String textVal=values.get(SmppHelper.DELIVERY_ACK_TEXT);
        if(textVal==null)
        	textVal="";
        
        reportData.setData(textVal.getBytes());
        reportData.setMessageID(idVal);        
        if(statusVal!=null)
        {
        	if(statusVal.equals("ACCEPTD"))
        		reportData.setDeliveryStatus(DeliveryStatus.ACCEPTED);
        	else if(statusVal.equals("DELETED"))
        		reportData.setDeliveryStatus(DeliveryStatus.DELETED);
        	else if(statusVal.equals("DELIVRD"))
        		reportData.setDeliveryStatus(DeliveryStatus.DELIVERED);
        	else if(statusVal.equals("ENROUTE"))
        		reportData.setDeliveryStatus(DeliveryStatus.ENROUTE);
        	else if(statusVal.equals("EXPIRED"))
        		reportData.setDeliveryStatus(DeliveryStatus.EXPIRED);
        	else if(statusVal.equals("REJECTD"))
        		reportData.setDeliveryStatus(DeliveryStatus.REJECTED);
        	else if(statusVal.equals("UNDELIV"))
        		reportData.setDeliveryStatus(DeliveryStatus.UNDELIVERABLE);
        	else if(statusVal.equals("SKIPPED"))
        		reportData.setDeliveryStatus(DeliveryStatus.SKIPPED);
        	else if(statusVal.equals("SCHEDLD"))
        		reportData.setDeliveryStatus(DeliveryStatus.SCHEDULED);
        	else
        		reportData.setDeliveryStatus(DeliveryStatus.UNKNOWN);
        }
        
        try 
        {
            int error = Integer.parseInt(errorVal);
            reportData.setErrorCode(error);
        } 
        catch (NumberFormatException e) 
        {
        }
        
        return reportData;
    }
}