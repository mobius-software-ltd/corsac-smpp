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

import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.commons.util.StringUtil;
import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.SmppInvalidArgumentException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

public class ReplaceSm extends PduRequest<ReplaceSmResp> 
{
    private String messageId;
    private Address sourceAddress;
    private String scheduleDeliveryTime;
    private String validityPeriod;
    private byte registeredDelivery;
    private byte defaultMsgId;
    private byte[] shortMessage; 
    
    public ReplaceSm() 
    {
        super(CommandType.CMD_ID_REPLACE_SM, "replace_sm");
    }

    @Override
    public ReplaceSmResp createResponse() 
    {
        ReplaceSmResp resp = new ReplaceSmResp();
        resp.setSequenceNumber(this.getSequenceNumber());
        return resp;
    }

    @Override
    public Class<ReplaceSmResp> getResponseClass() 
    {
        return ReplaceSmResp.class;
    }
    
    public String getMessageId()
    {
        return messageId;
    }
    
    public void setMessageId(String messageId)
    {
        this.messageId = messageId;
    }
    
    public int getShortMessageLength() 
    {
        return (this.shortMessage == null ? 0 : this.shortMessage.length);
    }

    public byte[] getShortMessage() 
    {
        return this.shortMessage;
    }

    public void setShortMessage(byte[] value) throws SmppInvalidArgumentException 
    {
        if (value != null && value.length > 255) 
            throw new SmppInvalidArgumentException("A short message in a PDU can only be a max of 255 bytes [actual=" + value.length + "]; use optional parameter message_payload as an alternative");
        
        this.shortMessage = value;
    }

    public byte getDefaultMsgId() 
    {
        return this.defaultMsgId;
    }

    public void setDefaultMsgId(byte value) 
    {
        this.defaultMsgId = value;
    }

    public byte getRegisteredDelivery() 
    {
        return this.registeredDelivery;
    }

    public void setRegisteredDelivery(byte value) 
    {
        this.registeredDelivery = value;
    }

    public String getValidityPeriod() 
    {
        return this.validityPeriod;
    }

    public void setValidityPeriod(String value) 
    {
        this.validityPeriod = value;
    }

    public String getScheduleDeliveryTime() 
    {
        return this.scheduleDeliveryTime;
    }

    public void setScheduleDeliveryTime(String value) 
    {
        this.scheduleDeliveryTime = value;
    }

    public Address getSourceAddress() 
    {
        return this.sourceAddress;
    }

    public void setSourceAddress(Address value) 
    {
        this.sourceAddress = value;
    }

    @Override
    public void readBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        this.messageId = ByteBufUtil.readNullTerminatedString(buffer); 
        this.sourceAddress = ByteBufUtil.readAddress(buffer);
        this.scheduleDeliveryTime = ByteBufUtil.readNullTerminatedString(buffer);
        this.validityPeriod = ByteBufUtil.readNullTerminatedString(buffer);
        this.registeredDelivery = buffer.readByte();
        this.defaultMsgId = buffer.readByte();
        // this is always an unsigned version of the short message length
        short shortMessageLength = buffer.readUnsignedByte();
        this.shortMessage = new byte[shortMessageLength];
        buffer.readBytes(this.shortMessage);
    }

    @Override
    public int calculateByteSizeOfBody() 
    {
        int bodyLength = 0;
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.messageId);
        bodyLength += ByteBufUtil.calculateByteSizeOfAddress(this.sourceAddress);
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.scheduleDeliveryTime);
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.validityPeriod);
        bodyLength += 3;
        bodyLength += getShortMessageLength();
        return bodyLength;
    }

    @Override
    public void writeBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException {
        ByteBufUtil.writeNullTerminatedString(buffer, this.messageId);
        ByteBufUtil.writeAddress(buffer, this.sourceAddress);
        ByteBufUtil.writeNullTerminatedString(buffer, this.scheduleDeliveryTime);
        ByteBufUtil.writeNullTerminatedString(buffer, this.validityPeriod);
        buffer.writeByte(this.registeredDelivery);
        buffer.writeByte(this.defaultMsgId);
        buffer.writeByte((byte)getShortMessageLength());
        
        if (this.shortMessage != null) 
            buffer.writeBytes(this.shortMessage);        
    }

    @Override
    public void appendBodyToString(StringBuilder buffer) 
    {
        buffer.append("( messageId [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.messageId));
        buffer.append("] sourceAddr [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.sourceAddress));
        buffer.append("] scheduleDeliveryTime [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.scheduleDeliveryTime));
        buffer.append("] validityPeriod [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.validityPeriod));
        buffer.append("] regDlvry [0x");
        buffer.append(HexUtil.toHexString(this.registeredDelivery));
        buffer.append("] message [");
        HexUtil.appendHexString(buffer, this.shortMessage);
        buffer.append("])");
    }
}