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

public class DataSm extends BaseSm<DataSmResp> 
{
    public DataSm() 
    {
        super(CommandType.CMD_ID_DATA_SM, "data_sm");
    }

    @Override
    public DataSmResp createResponse() 
    {
        DataSmResp resp = new DataSmResp();
        resp.setSequenceNumber(this.getSequenceNumber());
        return resp;
    }

    @Override
    public Class<DataSmResp> getResponseClass() 
    {
        return DataSmResp.class;
    }
    
    @Override
    public void readBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        this.serviceType = ByteBufUtil.readNullTerminatedString(buffer);
        this.sourceAddress = ByteBufUtil.readAddress(buffer);
        this.destAddress = ByteBufUtil.readAddress(buffer);
        this.esmClass = buffer.readByte();
        this.registeredDelivery = buffer.readByte();
        this.dataCoding = buffer.readByte();
    }

    @Override
    public int calculateByteSizeOfBody() 
    {
        int bodyLength = 0;
        bodyLength += ByteBufUtil.calculateByteSizeOfNullTerminatedString(this.serviceType);
        bodyLength += ByteBufUtil.calculateByteSizeOfAddress(this.sourceAddress);
        bodyLength += ByteBufUtil.calculateByteSizeOfAddress(this.destAddress);
        bodyLength += 3;    // esmClass, regDelivery, dataCoding bytes
        return bodyLength;
    }

    @Override
    public void writeBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        ByteBufUtil.writeNullTerminatedString(buffer, this.serviceType);
        ByteBufUtil.writeAddress(buffer, this.sourceAddress);
        ByteBufUtil.writeAddress(buffer, this.destAddress);
        buffer.writeByte(this.esmClass);
        buffer.writeByte(this.registeredDelivery);
        buffer.writeByte(this.dataCoding);
    }
}