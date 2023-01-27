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

public abstract class BaseSm<R extends PduResponse> extends PduRequest<R> 
{
    protected String serviceType;
    protected Address sourceAddress;
    protected Address destAddress;
    protected byte esmClass;
    protected byte protocolId;
    protected byte priority;
    protected String scheduleDeliveryTime;
    protected String validityPeriod;
    protected byte registeredDelivery;
    protected byte replaceIfPresent;
    protected byte dataCoding;
    protected byte defaultMsgId;
    protected byte[] shortMessage;         

    public BaseSm(CommandType commandId, String name) 
    {
        super(commandId, name);
    }

    public int getShortMessageLength() 
    {
        return (this.shortMessage == null ? 0 : this.shortMessage.length);
    }

    public byte[] getShortMessage() {
        return this.shortMessage;
    }

    public void setShortMessage(byte[] value) throws SmppInvalidArgumentException 
    {
        if (value != null && value.length > 255) 
            throw new SmppInvalidArgumentException("A short message in a PDU can only be a max of 255 bytes [actual=" + value.length + "]; use optional parameter message_payload as an alternative");
        
        this.shortMessage = value;
    }

    public byte getReplaceIfPresent() 
    {
        return this.replaceIfPresent;
    }

    public void setReplaceIfPresent(byte value) 
    {
        this.replaceIfPresent = value;
    }

    public byte getDataCoding() 
    {
        return this.dataCoding;
    }

    public void setDataCoding(byte value) 
    {
        this.dataCoding = value;
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

    public byte getPriority() 
    {
        return this.priority;
    }

    public void setPriority(byte value) 
    {
        this.priority = value;
    }

    public byte getEsmClass() 
    {
        return this.esmClass;
    }

    public void setEsmClass(byte value) 
    {
        this.esmClass = value;
    }

    public byte getProtocolId() 
    {
        return this.protocolId;
    }

    public void setProtocolId(byte value) 
    {
        this.protocolId = value;
    }

    public String getServiceType() 
    {
        return this.serviceType;
    }

    public void setServiceType(String value) 
    {
        this.serviceType = value;
    }

    public Address getSourceAddress() 
    {
        return this.sourceAddress;
    }

    public void setSourceAddress(Address value) 
    {
        this.sourceAddress = value;
    }

    public Address getDestAddress() 
    {
        return this.destAddress;
    }

    public void setDestAddress(Address value) 
    {
        this.destAddress = value;
    }

    @Override
    public void readBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        this.serviceType = ByteBufUtil.readNullTerminatedString(buffer);
        this.sourceAddress = ByteBufUtil.readAddress(buffer);
        this.destAddress = ByteBufUtil.readAddress(buffer);
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
    public int calculateByteSizeOfBody() 
    {
        int bodyLength = 0;
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.serviceType);
        bodyLength += ByteBufUtil.calculateByteSizeOfAddress(this.sourceAddress);
        bodyLength += ByteBufUtil.calculateByteSizeOfAddress(this.destAddress);
        bodyLength += 3;
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.scheduleDeliveryTime);
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.validityPeriod);
        bodyLength += 5;
        bodyLength += getShortMessageLength();
        return bodyLength;
    }

    @Override
    public void writeBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        ByteBufUtil.writeNullTerminatedString(buffer, this.serviceType);
        ByteBufUtil.writeAddress(buffer, this.sourceAddress);
        ByteBufUtil.writeAddress(buffer, this.destAddress);
        buffer.writeByte(this.esmClass);
        buffer.writeByte(this.protocolId);
        buffer.writeByte(this.priority);
        ByteBufUtil.writeNullTerminatedString(buffer, this.scheduleDeliveryTime);
        ByteBufUtil.writeNullTerminatedString(buffer, this.validityPeriod);
        buffer.writeByte(this.registeredDelivery);
        buffer.writeByte(this.replaceIfPresent);
        buffer.writeByte(this.dataCoding);
        buffer.writeByte(this.defaultMsgId);
        buffer.writeByte((byte)getShortMessageLength());
        
        if (this.shortMessage != null) 
            buffer.writeBytes(this.shortMessage);        
    }

    @Override
    public void appendBodyToString(StringBuilder buffer) 
    {
        buffer.append("(serviceType [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.serviceType));
        buffer.append("] sourceAddr [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.sourceAddress));
        buffer.append("] destAddr [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.destAddress));
        buffer.append("] esmCls [0x");
        buffer.append(HexUtil.toHexString(this.esmClass));
        buffer.append("] regDlvry [0x");
        buffer.append(HexUtil.toHexString(this.registeredDelivery));
        buffer.append("] dcs [0x");
        buffer.append(HexUtil.toHexString(this.dataCoding));
        buffer.append("] message [");
        HexUtil.appendHexString(buffer, this.shortMessage);
        buffer.append("])");
    }
}