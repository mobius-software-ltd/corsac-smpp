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
import java.io.UnsupportedEncodingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mobius.software.protocols.smpp.exceptions.NotEnoughDataInBufferException;
import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.TerminatingNullByteNotFoundException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

import io.netty.buffer.ByteBuf;

public class ByteBufUtil 
{
	public static Logger logger=LogManager.getLogger(ByteBufUtil.class);
	
	public static final Address EMPTY_ADDRESS = new Address();
	public static final int PDU_CMD_ID_RESP_MASK = 0x80000000;
	
	static public boolean isRequestCommandId(int commandId) 
	{
        return ((commandId & PDU_CMD_ID_RESP_MASK) == 0);
    }
	
	static public int calculateByteSizeOfAddress(Address value) 
	{
        if (value == null) 
            return EMPTY_ADDRESS.calculateByteSize();
        else 
            return value.calculateByteSize();        
    }
	
	static public Address readAddress(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        if (buffer.readableBytes() < 3) 
            throw new NotEnoughDataInBufferException("Parsing address", buffer.readableBytes(), 3);
        
        Address address = new Address();
        address.read(buffer);
        return address;
    }

    static public void writeAddress(ByteBuf buffer, Address value) throws UnrecoverablePduException, RecoverablePduException 
    {
        if (value == null) 
            EMPTY_ADDRESS.write(buffer);
        else 
            value.write(buffer);        
    }
    
    static public Tlv readTlv(ByteBuf buffer) throws NotEnoughDataInBufferException 
    {
        if (buffer.readableBytes() < 4) 
            throw new NotEnoughDataInBufferException("Parsing TLV tag and length", buffer.readableBytes(), 4);
        
        short tag = buffer.readShort();
        int length = buffer.readUnsignedShort();

        // check if we have enough data for the TLV
        if (buffer.readableBytes() < length) 
            throw new NotEnoughDataInBufferException("Parsing TLV value", buffer.readableBytes(), length);
        
        byte[] value = new byte[length];
        buffer.readBytes(value);

        return new Tlv(tag, value);
    }
    
    static public void writeTlv(ByteBuf buffer, Tlv tlv) throws NotEnoughDataInBufferException 
    {
        if (tlv == null) 
            return;
        
        buffer.writeShort(tlv.getTag());
        buffer.writeShort(tlv.calculateLength());
        if (tlv.getValue() != null) 
            buffer.writeBytes(tlv.getValue());        
    }

    static public int calculateByteSizeOfNullTerminatedString(String value) 
    {
        if (value == null) {
            return 1;
        }
        return value.length() + 1;
    }
    
    static public void writeNullTerminatedString(ByteBuf buffer, String value) throws UnrecoverablePduException 
    {
        if (value != null) 
        {
            try 
            {
                byte[] bytes = value.getBytes("ISO-8859-1");
                buffer.writeBytes(bytes);
            } 
            catch (UnsupportedEncodingException e) 
            {
                throw new UnrecoverablePduException(e.getMessage(), e);
            }
        }
        
        buffer.writeByte((byte)0x00);
    }

    static public String readNullTerminatedString(ByteBuf buffer) throws TerminatingNullByteNotFoundException 
    {
        int maxLength = buffer.readableBytes();

        if (maxLength == 0) 
            return null;
        
        int offset = buffer.readerIndex();
        int zeroPos = 0;

        while ((zeroPos < maxLength) && (buffer.getByte(zeroPos+offset) != 0x00)) 
            zeroPos++;
        
        if (zeroPos >= maxLength) 
            throw new TerminatingNullByteNotFoundException("Terminating null byte not found after searching [" + maxLength + "] bytes");
        
        String result = null;
        if (zeroPos > 0) 
        {
            byte[] bytes = new byte[zeroPos];
            buffer.readBytes(bytes);
            try {
                result = new String(bytes, "ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                logger.error("Impossible error", e);
            }
        } 
        else 
            result = "";
        
        byte b = buffer.readByte();
        if (b != 0x00) {
            logger.error("Impossible error: last byte read SHOULD have been a null byte, but was [" + b + "]");
        }

        return result;
    }
}