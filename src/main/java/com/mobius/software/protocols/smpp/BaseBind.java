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
import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.commons.util.StringUtil;
import com.mobius.software.protocols.smpp.channel.SmppVersion;
import com.mobius.software.protocols.smpp.exceptions.NotEnoughDataInBufferException;
import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

import io.netty.buffer.ByteBuf;

public abstract class BaseBind<R extends PduResponse> extends PduRequest<R> 
{
    private String systemId;
    private String password;
    private String systemType;
    private SmppVersion interfaceVersion;
    private Address addressRange;

    public BaseBind(CommandType commandId, String name) 
    {
        super(commandId, name);
    }

    public void setSystemId(String value) 
    {
        this.systemId = value;
    }

    public String getSystemId() 
    {
        return this.systemId;
    }

    public void setPassword(String value) 
    {
        this.password = value;
    }

    public String getPassword() 
    {
        return this.password;
    }

    public void setSystemType(String value) 
    {
        this.systemType = value;
    }

    public String getSystemType() 
    {
        return this.systemType;
    }

    public void setInterfaceVersion(SmppVersion value) 
    {
        this.interfaceVersion = value;
    }

    public SmppVersion getInterfaceVersion() 
    {
        return this.interfaceVersion;
    }

    public Address getAddressRange() 
    {
        return this.addressRange;
    }

    public void setAddressRange(Address value) 
    {
        this.addressRange = value;
    }

    @Override
    public void readBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        this.systemId = ByteBufUtil.readNullTerminatedString(buffer);
        this.password = ByteBufUtil.readNullTerminatedString(buffer);
        this.systemType = ByteBufUtil.readNullTerminatedString(buffer);
        
        if (buffer.readableBytes() < 3) 
            throw new NotEnoughDataInBufferException("After parsing systemId, password, and systemType", buffer.readableBytes(), 3);
        
        this.interfaceVersion = SmppVersion.fromInt(buffer.readByte() & 0x0FF);
        this.addressRange = ByteBufUtil.readAddress(buffer);
    }
    
    @Override
    public int calculateByteSizeOfBody() {
        int bodyLength = 0;
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.systemId);
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.password);
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.systemType);
        bodyLength += 1; // interface version
        bodyLength += ByteBufUtil.calculateByteSizeOfAddress(this.addressRange);
        return bodyLength;
    }

    @Override
    public void writeBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
    	ByteBufUtil.writeNullTerminatedString(buffer, this.systemId);
    	ByteBufUtil.writeNullTerminatedString(buffer, this.password);
    	ByteBufUtil.writeNullTerminatedString(buffer, this.systemType);
        buffer.writeByte(this.interfaceVersion.getValue());
        ByteBufUtil.writeAddress(buffer, this.addressRange);
    }

    @Override
    public void appendBodyToString(StringBuilder buffer) 
    {
        buffer.append("systemId [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.systemId));
        buffer.append("] password [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.password));
        buffer.append("] systemType [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.systemType));
        buffer.append("] interfaceVersion [0x");
        buffer.append(HexUtil.toHexString(this.interfaceVersion.getValue()));
        buffer.append("] addressRange (");
        if (this.addressRange == null) 
            buffer.append(ByteBufUtil.EMPTY_ADDRESS.toString());
        else 
            buffer.append(StringUtil.toStringWithNullAsEmpty(this.addressRange));
        
        buffer.append(")");
    }
}