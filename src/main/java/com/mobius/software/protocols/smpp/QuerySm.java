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

public class QuerySm extends PduRequest<QuerySmResp> 
{
    private String messageId;
    private Address sourceAddress;

    public QuerySm() 
    {
        super(CommandType.CMD_ID_QUERY_SM, "query_sm");
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


    @Override
    public void readBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        this.messageId = ByteBufUtil.readNullTerminatedString(buffer);
        this.sourceAddress = ByteBufUtil.readAddress(buffer);
    }

    @Override
    public int calculateByteSizeOfBody() 
    {
        int bodyLength = 0;
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.messageId);
        bodyLength += ByteBufUtil.calculateByteSizeOfAddress(this.sourceAddress);
        return bodyLength;
    }

    @Override
    public void writeBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException {
        ByteBufUtil.writeNullTerminatedString(buffer, this.messageId);
        ByteBufUtil.writeAddress(buffer, this.sourceAddress);
    }

    @Override
    public void appendBodyToString(StringBuilder buffer) 
    {
        buffer.append("(messageId [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.messageId));
        buffer.append("] sourceAddr [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.sourceAddress));
        buffer.append("])");
    }

    @Override
    public QuerySmResp createResponse() 
    {
        QuerySmResp resp = new QuerySmResp();
        resp.setSequenceNumber(this.getSequenceNumber());
        return resp;
    }

    @Override
    public Class<QuerySmResp> getResponseClass() 
    {
        return QuerySmResp.class;
    }
}