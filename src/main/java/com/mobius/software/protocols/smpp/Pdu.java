package com.mobius.software.protocols.smpp;
import java.util.ArrayList;

import com.cloudhopper.commons.util.HexUtil;
import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

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

public abstract class Pdu 
{
	public static final int PDU_HEADER_LENGTH = 16;
	public static final int PDU_INT_LENGTH = 4;
	
	private final String name;
    private final boolean isRequest;
    private Integer commandLength;
    private CommandType commandId;
    private MessageStatus commandStatus;
    private Integer sequenceNumber;
    
    private ArrayList<Tlv> optionalParameters;
    private Object referenceObject;

    public Pdu(CommandType commandId, String name, boolean isRequest) 
    {
        this.name = name;
        this.isRequest = isRequest;
        this.commandLength = null;
        this.commandId = commandId;
        this.sequenceNumber = null;
        this.referenceObject = null;
    }

    public void setReferenceObject(Object value) 
    {
        this.referenceObject = value;
    }

    public Object getReferenceObject() 
    {
        return this.referenceObject;
    }

    public String getName() 
    {
        return this.name;
    }

    public boolean isRequest() 
    {
        return this.isRequest;
    }

    public boolean isResponse() 
    {
        return !this.isRequest;
    }

    public boolean hasCommandLengthCalculated() 
    {
        return (this.commandLength != null);
    }

    public void removeCommandLength() 
    {
        this.commandLength = null;
    }

    public void setCommandLength(int value) 
    {
        this.commandLength = value;
    }

    public int getCommandLength() 
    {
        if (this.commandLength == null) 
            return 0;
        else 
            return this.commandLength.intValue();        
    }

    public int calculateAndSetCommandLength() 
    {
        int len = PDU_HEADER_LENGTH + this.calculateByteSizeOfBody() + this.calculateByteSizeOfOptionalParameters();
        this.setCommandLength(len);
        return len;
    }

    public CommandType getCommandId() 
    {
        return this.commandId;
    }

    public void setCommandStatus(MessageStatus value) 
    {
        this.commandStatus = value;
    }

    public MessageStatus getCommandStatus() 
    {
        return this.commandStatus;
    }

    public boolean hasSequenceNumberAssigned() 
    {
        return (this.sequenceNumber != null);
    }

    public void removeSequenceNumber() 
    {
        this.sequenceNumber = null;
    }

    public void setSequenceNumber(int value) 
    {
        this.sequenceNumber = value;
    }

    public int getSequenceNumber() 
    {
        if (this.sequenceNumber == null) 
            return 0;
        else 
            return this.sequenceNumber.intValue();        
    }

    public int getOptionalParameterCount() 
    {
        if (this.optionalParameters == null) 
            return 0;
        
        return this.optionalParameters.size();
    }

    public ArrayList<Tlv> getOptionalParameters() 
    {
        return this.optionalParameters;
    }

    public void addOptionalParameter(Tlv tlv) 
    {
        if (this.optionalParameters == null) 
            this.optionalParameters = new ArrayList<Tlv>();
        
        this.optionalParameters.add(tlv);
    }

    public Tlv removeOptionalParameter(short tag) 
    {
        int i = this.findOptionalParameter(tag);
        if (i < 0) 
            return null;
        else 
            return this.optionalParameters.remove(i);        
    }

    public Tlv setOptionalParameter(Tlv tlv) 
    {
        int i = this.findOptionalParameter(tlv.getTag());
        if (i < 0) 
        {
            this.addOptionalParameter(tlv);
            return null;
        } 
        else 
            return this.optionalParameters.set(i, tlv);        
    }

    public boolean hasOptionalParameter(short tag)
    {
        return (this.findOptionalParameter(tag) >= 0);
    }
    
    protected int findOptionalParameter(short tag) 
    {
        if (this.optionalParameters == null) 
            return -1;
        
        int i = 0;
        for (Tlv tlv : this.optionalParameters) 
        {
            if (tlv.getTag() == tag) 
                return i;
            
            i++;
        }
        
        return -1;
    }

    public Tlv getOptionalParameter(short tag) 
    {
        if (this.optionalParameters == null) 
            return null;
        
        int i = this.findOptionalParameter(tag);
        if (i < 0) 
            return null;
        
        return this.optionalParameters.get(i);
    }

    abstract protected int calculateByteSizeOfBody();

    abstract public void readBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException;

    abstract public void writeBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException;

    abstract protected void appendBodyToString(StringBuilder buffer);

    protected int calculateByteSizeOfOptionalParameters() 
    {
        if (this.optionalParameters == null) 
            return 0;
        
        int optParamLength = 0;
        
        for (Tlv tlv : this.optionalParameters) 
            optParamLength += tlv.calculateLength() + 4;
        
        return optParamLength;
    }

    public void readOptionalParameters(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        while (buffer.readableBytes() > 0) 
        {
            Tlv tlv = ByteBufUtil.readTlv(buffer);
            this.addOptionalParameter(tlv);
        }
    }

    public void writeOptionalParameters(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        if (this.optionalParameters == null) 
            return;
        
        for (Tlv tlv : this.optionalParameters) 
            ByteBufUtil.writeTlv(buffer, tlv);        
    }

    protected void appendOptionalParameterToString(StringBuilder buffer) 
    {
        if (this.optionalParameters == null) 
            return;
        
        int i = 0;
        for (Tlv tlv : this.optionalParameters) 
        {
            if (i != 0) 
                buffer.append(" (");
            else 
                buffer.append("(");            

            buffer.append(tlv.toString());
            buffer.append(")");
            i++;
        }
    }

    @Override
    public String toString() 
    {
        StringBuilder buffer = new StringBuilder(65 + 300 + (getOptionalParameterCount()*20));

        buffer.append("(");
        buffer.append(this.name);
        buffer.append(": 0x");
        buffer.append(HexUtil.toHexString(getCommandLength()));
        buffer.append(" 0x");
        buffer.append(HexUtil.toHexString(this.commandId.getValue()));
        buffer.append(" 0x");
        if(this.commandStatus!=null)
        	buffer.append(HexUtil.toHexString(this.commandStatus.getValue()));
        else
        	buffer.append(HexUtil.toHexString(0));
        buffer.append(" 0x");
        buffer.append(HexUtil.toHexString(getSequenceNumber()));

        if (this instanceof PduResponse) 
        {
            PduResponse response = (PduResponse)this;
            String statusMessage = response.getResultMessage();
            if (statusMessage != null) {
                buffer.append(" result: \"");
                buffer.append(statusMessage);
                buffer.append("\"");
            } 
            else 
                buffer.append(" result: <unmapped>");            
        }

        buffer.append(")");

        buffer.append(" (body: ");
        this.appendBodyToString(buffer);
        
        buffer.append(") (opts: ");
        this.appendOptionalParameterToString(buffer);
        buffer.append(")");

        return buffer.toString();
    }
}