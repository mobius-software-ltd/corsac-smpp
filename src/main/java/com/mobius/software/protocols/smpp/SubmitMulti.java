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
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.SmppInvalidArgumentException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

public class SubmitMulti extends BaseSm<SubmitMultiResp> 
{
	public static final int SME_ADDRESS = 1;
    public static final int DISTRIBUTION_LIST_NAME = 2;
	
	private int numberOfDest;

	private List<Address> destAddresses = new ArrayList<Address>();
	private List<String> destDistributionList = new ArrayList<String>();

	public SubmitMulti() 
	{
		super(CommandType.CMD_ID_SUBMIT_MULTI, "submit_multi");
	}

	@Override
	public SubmitMultiResp createResponse() 
	{
		SubmitMultiResp resp = new SubmitMultiResp();
		resp.setSequenceNumber(this.getSequenceNumber());
		return resp;
	}

	@Override
	public Class<SubmitMultiResp> getResponseClass() 
	{
		return SubmitMultiResp.class;
	}

	public Address getDestAddress() 
	{
		return null;
	}

	@Override
	public void setDestAddress(Address value) 
	{

	}

	public void addDestAddresses(Address address) throws SmppInvalidArgumentException 
	{
		this.numberOfDest++;
		this.destAddresses.add(address);
	}

	public void addDestDestributionListName(String name) 
	{
		this.numberOfDest++;
		this.destDistributionList.add(name);
	}

	public List<Address> getDestAddresses() 
	{
		return this.destAddresses;
	}

	public List<String> getDestDestributionListName() 
	{
		return this.destDistributionList;
	}
	
	public int getNumberOfDest()
	{
		return this.numberOfDest;
	}
	
    @Override
    public void readBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        this.serviceType = ByteBufUtil.readNullTerminatedString(buffer);
        this.sourceAddress = ByteBufUtil.readAddress(buffer);

        this.numberOfDest = buffer.readByte() & 0xFF;

        for(int count=0;count<this.numberOfDest; count++)
        {
        	byte flag = buffer.readByte();
        	if(flag==SME_ADDRESS)
        		this.destAddresses.add(ByteBufUtil.readAddress(buffer));
        	else if(flag==DISTRIBUTION_LIST_NAME)
        		this.destDistributionList.add(ByteBufUtil.readNullTerminatedString(buffer));        	
        }
        
        this.esmClass = buffer.readByte();
        this.protocolId = buffer.readByte();
        this.priority = buffer.readByte();
        this.scheduleDeliveryTime = ByteBufUtil.readNullTerminatedString(buffer);
        this.validityPeriod = ByteBufUtil.readNullTerminatedString(buffer);
        this.registeredDelivery = buffer.readByte();
        this.replaceIfPresent = buffer.readByte();
        this.dataCoding = buffer.readByte();
        this.defaultMsgId = buffer.readByte();

        short shortMessageLength = buffer.readUnsignedByte();
        this.shortMessage = new byte[shortMessageLength];
        buffer.readBytes(this.shortMessage);
    }

	@Override
	public void writeBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
	{
		ByteBufUtil.writeNullTerminatedString(buffer, this.serviceType);
		ByteBufUtil.writeAddress(buffer, this.sourceAddress);

		buffer.writeByte(this.numberOfDest);
		
		for(Address adress : this.destAddresses)
		{
			buffer.writeByte(SME_ADDRESS);
			ByteBufUtil.writeAddress(buffer, adress);
		}

		for(String s : this.destDistributionList)
		{
			buffer.writeByte(DISTRIBUTION_LIST_NAME);
			ByteBufUtil.writeNullTerminatedString(buffer, s);			
		}

		buffer.writeByte(this.esmClass);
		buffer.writeByte(this.protocolId);
		buffer.writeByte(this.priority);
		ByteBufUtil.writeNullTerminatedString(buffer,this.scheduleDeliveryTime);
		ByteBufUtil.writeNullTerminatedString(buffer, this.validityPeriod);
		buffer.writeByte(this.registeredDelivery);
		buffer.writeByte(this.replaceIfPresent);
		buffer.writeByte(this.dataCoding);
		buffer.writeByte(this.defaultMsgId);
		buffer.writeByte((byte) getShortMessageLength());
		
		if (this.shortMessage != null) 
			buffer.writeBytes(this.shortMessage);		
	}
	
    @Override
    public int calculateByteSizeOfBody() 
    {
        int bodyLength = 0;
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.serviceType);
        bodyLength += ByteBufUtil.calculateByteSizeOfAddress(this.sourceAddress);
        
        bodyLength +=1;
        
        for(Address adress : this.destAddresses)
        {
        	bodyLength += 1;
        	bodyLength += ByteBufUtil.calculateByteSizeOfAddress(adress);
        }
        
		for(String s : this.destDistributionList)
		{
			bodyLength += 1;
			bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(s);
		}
		
        bodyLength += 3;
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.scheduleDeliveryTime);
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.validityPeriod);
        bodyLength += 5;
        bodyLength += getShortMessageLength();
        return bodyLength;
    }
}