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

public class AlertNotification extends Pdu 
{

    protected Address sourceAddress;
    protected Address esmeAddress;

    public AlertNotification()
    {
        super( CommandType.CMD_ID_ALERT_NOTIFICATION, "alert_notification", true );
    }

    public Address getSourceAddress() 
    {
        return this.sourceAddress;
    }

    public void setSourceAddress(Address value) 
    {
        this.sourceAddress = value;
    }

    public Address getEsmeAddress() 
    {
        return this.esmeAddress;
    }

    public void setEsmeAddress(Address value) 
    {
        this.esmeAddress = value;
    }

    @Override
    protected int calculateByteSizeOfBody()
    {
        int bodyLength = 0;
        bodyLength += ByteBufUtil.calculateByteSizeOfAddress(this.sourceAddress);
        bodyLength += ByteBufUtil.calculateByteSizeOfAddress(this.esmeAddress);
        return bodyLength;
    }

    @Override
    public void readBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException
    {
        this.sourceAddress = ByteBufUtil.readAddress(buffer);
        this.esmeAddress = ByteBufUtil.readAddress(buffer);
    }

    @Override
    public void writeBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException
    {
    	ByteBufUtil.writeAddress(buffer, this.sourceAddress);
    	ByteBufUtil.writeAddress(buffer, this.esmeAddress);
    }

    @Override
    protected void appendBodyToString(StringBuilder buffer)
    {
        buffer.append("( sourceAddr [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.sourceAddress));
        buffer.append("] esmeAddr [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.esmeAddress));
        buffer.append("])");
    }
}