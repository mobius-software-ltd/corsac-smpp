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

public class Address 
{
    private byte ton;
    private byte npi;
    private String address;

    public Address() 
    {
        this((byte)0, (byte)0, (String)null);
    }

    public Address(byte ton, byte npi, String address) 
    {
        this.ton = ton;
        this.npi = npi;
        this.address = address;
    }

    public byte getTon() 
    {
        return this.ton;
    }

    public void setTon(byte value) 
    {
        this.ton = value;
    }

    public byte getNpi() 
    {
        return this.npi;
    }

    public void setNpi(byte value) 
    {
        this.npi = value;
    }

    public String getAddress() 
    {
        return this.address;
    }

    public void setAddress(String value) 
    {
        this.address = value;
    }

    public int calculateByteSize() 
    {
    	if(this.address==null)
    		return 3;
    	
        return 3 + this.address.length();
    }

    public void read(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        this.ton = buffer.readByte();
        this.npi = buffer.readByte();
        this.address = ByteBufUtil.readNullTerminatedString(buffer);
    }

    public void write(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        buffer.writeByte(this.ton);
        buffer.writeByte(this.npi);
        ByteBufUtil.writeNullTerminatedString(buffer, this.address);
    }

    @Override
    public String toString() 
    {
        StringBuilder buffer = new StringBuilder(40);
        buffer.append("0x");
        buffer.append(HexUtil.toHexString(this.ton));
        buffer.append(" 0x");
        buffer.append(HexUtil.toHexString(this.npi));
        buffer.append(" [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.address));
        buffer.append("]");
        return buffer.toString();
    }
}