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

import com.cloudhopper.commons.util.StringUtil;
import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

public class CancelSm extends PduRequest<CancelSmResp> 
{
    protected String serviceType;
    protected String messageId;
    protected Address sourceAddress;
    protected Address destAddress;

    public CancelSm() 
    {
        super(CommandType.CMD_ID_CANCEL_SM, "cancel_sm");
    }

    public String getServiceType() 
    {
        return this.serviceType;
    }

    public void setServiceType(String value) 
    {
        this.serviceType = value;
    }

    public String getMessageId() 
    {
        return this.messageId;
    }

    public void setMessageId(String value) 
    {
        this.messageId = value;
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
        this.messageId = ByteBufUtil.readNullTerminatedString(buffer);
        this.sourceAddress = ByteBufUtil.readAddress(buffer);
        this.destAddress = ByteBufUtil.readAddress(buffer);
    }

    @Override
    public int calculateByteSizeOfBody() 
    {
        int bodyLength = 0;
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.serviceType);
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.messageId);
        bodyLength += ByteBufUtil.calculateByteSizeOfAddress(this.sourceAddress);
        bodyLength += ByteBufUtil.calculateByteSizeOfAddress(this.destAddress);
        return bodyLength;
    }

    @Override
    public void writeBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        ByteBufUtil.writeNullTerminatedString(buffer, this.serviceType);
        ByteBufUtil.writeNullTerminatedString(buffer, this.messageId);
        ByteBufUtil.writeAddress(buffer, this.sourceAddress);
        ByteBufUtil.writeAddress(buffer, this.destAddress);
    }

    @Override
    public void appendBodyToString(StringBuilder buffer) 
    {
        buffer.append("(serviceType [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.serviceType));
        buffer.append("] messageId [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.messageId));
        buffer.append("] sourceAddr [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.sourceAddress));
        buffer.append("] destAddr [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.destAddress));
        buffer.append("])");
    }

    @Override
    public CancelSmResp createResponse() 
    {
        CancelSmResp resp = new CancelSmResp();
        resp.setSequenceNumber(this.getSequenceNumber());
        return resp;
    }

    @Override
    public Class<CancelSmResp> getResponseClass() 
    {
        return CancelSmResp.class;
    }
}