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

import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

public class UnsucessfulSME 
{

	private int errorStatusCode;
	private Address address;

	public UnsucessfulSME() 
	{
	}

	public UnsucessfulSME(int errorStatusCode, Address address) 
	{
		super();
		this.errorStatusCode = errorStatusCode;
		this.address = address;
	}

	public int getErrorStatusCode() 
	{
		return errorStatusCode;
	}

	public void setErrorStatusCode(int errorStatusCode) 
	{
		this.errorStatusCode = errorStatusCode;
	}

	public Address getAddress() 
	{
		return address;
	}

	public void setAddress(Address address) 
	{
		this.address = address;
	}

	public void read(ByteBuf buffer) throws UnrecoverablePduException,RecoverablePduException 
	{
		this.address = ByteBufUtil.readAddress(buffer);
		this.errorStatusCode = buffer.readInt();
	}

	public void write(ByteBuf buffer) throws UnrecoverablePduException,RecoverablePduException 
	{
		ByteBufUtil.writeAddress(buffer, this.address);
		buffer.writeInt(this.errorStatusCode);
	}

	@Override
	public String toString() 
	{
		StringBuilder buffer = new StringBuilder(44);
		buffer.append(this.address.toString());
		buffer.append(" errorStatusCode [");
		buffer.append(this.errorStatusCode);
		buffer.append("]");
		return buffer.toString();
	}
}