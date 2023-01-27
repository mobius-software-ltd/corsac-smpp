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
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

public class QuerySmResp extends PduResponse 
{
    private String messageId;
    private String finalDate;
    private byte messageState;
    private byte errorCode;

    public QuerySmResp() 
    {
        super(CommandType.CMD_ID_QUERY_SM_RESP, "query_sm_resp");
    }

    public String getMessageId() 
    {
        return messageId;
    }

    public void setMessageId(final String iMessageId) 
    {
        messageId = iMessageId;
    }

    public String getFinalDate() 
    {
        return finalDate;
    }

    public void setFinalDate(final String iFinalDate) 
    {
        finalDate = iFinalDate;
    }

    public byte getMessageState() 
    {
        return messageState;
    }

    public void setMessageState(final byte iMessageState) 
    {
        messageState = iMessageState;
    }

    public byte getErrorCode() 
    {
        return errorCode;
    }

    public void setErrorCode(final byte iErrorCode) 
    {
        errorCode = iErrorCode;
    }

    @Override
    public void readBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        this.messageId = ByteBufUtil.readNullTerminatedString(buffer);
        this.finalDate = ByteBufUtil.readNullTerminatedString(buffer);
        this.messageState = buffer.readByte();
        this.errorCode = buffer.readByte();
    }

    @Override
    public int calculateByteSizeOfBody() 
    {
        int bodyLength = 0;
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.messageId);
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.finalDate);
        bodyLength += 2;
        return bodyLength;
    }

    @Override
    public void writeBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException {
        ByteBufUtil.writeNullTerminatedString(buffer, this.messageId);
        ByteBufUtil.writeNullTerminatedString(buffer, this.finalDate);
        buffer.writeByte(this.messageState);
        buffer.writeByte(this.errorCode);
    }

    @Override
    public void appendBodyToString(StringBuilder buffer) 
    {
        buffer.append("(messageId [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.messageId));
        buffer.append("] finalDate [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.finalDate));
        buffer.append("] messageState [0x");
        buffer.append(HexUtil.toHexString(this.messageState));
        buffer.append("] errorCode [0x");
        buffer.append(HexUtil.toHexString(this.errorCode));
        buffer.append("])");
    }
}
