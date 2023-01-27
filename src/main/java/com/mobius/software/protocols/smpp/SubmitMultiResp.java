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

import java.util.ArrayList;
import java.util.List;

import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.SmppInvalidArgumentException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

public class SubmitMultiResp extends BaseSmResp 
{
	private int numberOfUnsucessfulDest;
	private List<UnsucessfulSME> unsucessfulSmes = new ArrayList<UnsucessfulSME>();

	/**
	 * @param commandId
	 * @param name
	 */
	public SubmitMultiResp() {
		super(CommandType.CMD_ID_SUBMIT_MULTI_RESP, "submit_multi_resp");
	}

	public void addUnsucessfulSME(UnsucessfulSME unsucessfulSME) throws SmppInvalidArgumentException 
	{
		this.numberOfUnsucessfulDest++;
		this.unsucessfulSmes.add(unsucessfulSME);
	}

	public int getNumberOfUnsucessfulDest() {
		return numberOfUnsucessfulDest;
	}

	public List<UnsucessfulSME> getUnsucessfulSmes() {
		return unsucessfulSmes;
	}

	@Override
	public void readBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
	{
		super.readBody(buffer);

        this.numberOfUnsucessfulDest = buffer.readByte() & 0xFF;

		for (int count = 0; count < this.numberOfUnsucessfulDest; count++) 
		{
			Address address = ByteBufUtil.readAddress(buffer);
			int errorStatusCode = buffer.readInt();

			this.unsucessfulSmes.add(new UnsucessfulSME(errorStatusCode,address));
		}
	}

	@Override
	public int calculateByteSizeOfBody() 
	{
		int bodyLength = 0;
		bodyLength = super.calculateByteSizeOfBody();

		bodyLength += 1; // no_unsuccess

		for (int count = 0; count < this.numberOfUnsucessfulDest; count++) {
			UnsucessfulSME unsucessfulSME = this.unsucessfulSmes.get(count);
			bodyLength += ByteBufUtil.calculateByteSizeOfAddress(unsucessfulSME.getAddress());
			bodyLength += 4; // error_status_code
		}

		return bodyLength;
	}

	@Override
	public void writeBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
	{
		super.writeBody(buffer);

		buffer.writeByte(this.numberOfUnsucessfulDest);

		for (int count = 0; count < this.numberOfUnsucessfulDest; count++) 
		{
			UnsucessfulSME unsucessfulSME = this.unsucessfulSmes.get(count);
			ByteBufUtil.writeAddress(buffer, unsucessfulSME.getAddress());
			buffer.writeInt(unsucessfulSME.getErrorStatusCode());
		}
	}
}