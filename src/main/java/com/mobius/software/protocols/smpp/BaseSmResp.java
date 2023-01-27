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
import com.cloudhopper.commons.util.StringUtil;
import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

import io.netty.buffer.ByteBuf;

public abstract class BaseSmResp extends PduResponse 
{
    private String messageId;

    public BaseSmResp(CommandType commandId, String name) 
    {
        super(commandId, name);
    }

    public String getMessageId() 
    {
        return this.messageId;
    }

    public void setMessageId(String value) 
    {
        this.messageId = value;
    }

    @Override
    public void readBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        this.messageId = ByteBufUtil.readNullTerminatedString(buffer);
    }

    @Override
    public int calculateByteSizeOfBody() 
    {
        int bodyLength = 0;
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.messageId);
        return bodyLength;
    }

    @Override
    public void writeBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        if (!((buffer.writableBytes() == 0) && (this.messageId == null))) 
            ByteBufUtil.writeNullTerminatedString(buffer, this.messageId);        
    }

    @Override
    public void appendBodyToString(StringBuilder buffer) 
    {
        buffer.append("(messageId [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.messageId));
        buffer.append("])");
    }
}