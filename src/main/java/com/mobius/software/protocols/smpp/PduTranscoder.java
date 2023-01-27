package com.mobius.software.protocols.smpp;
import com.cloudhopper.commons.util.HexUtil;
import com.mobius.software.protocols.smpp.exceptions.NotEnoughDataInBufferException;
import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.UnknownCommandIdException;
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
import io.netty.buffer.Unpooled;

public class PduTranscoder 
{
	public PduTranscoder() 
    {
    }

    public ByteBuf encode(Pdu pdu) throws UnrecoverablePduException, RecoverablePduException 
    {
        if (!pdu.hasCommandLengthCalculated()) 
            pdu.calculateAndSetCommandLength();
        
        ByteBuf buffer = Unpooled.buffer(pdu.getCommandLength());

        buffer.writeInt(pdu.getCommandLength());
        buffer.writeInt(pdu.getCommandId().getValue());
        
        if(pdu.getCommandStatus()!=null)
        	buffer.writeInt(pdu.getCommandStatus().getValue());
        else
        	buffer.writeInt(0);
        
        buffer.writeInt(pdu.getSequenceNumber());

        pdu.writeBody(buffer);

        pdu.writeOptionalParameters(buffer);

        if (buffer.readableBytes() != pdu.getCommandLength()) 
            throw new NotEnoughDataInBufferException("During PDU encoding the expected commandLength did not match the actual encoded (a serious error with our own encoding process)", pdu.getCommandLength(), buffer.readableBytes());
        
        return buffer;
    }
    
    public Pdu decode(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        if (buffer.readableBytes() < Pdu.PDU_INT_LENGTH) 
            return null;

        int commandLength = buffer.getInt(buffer.readerIndex());

        if (commandLength < Pdu.PDU_HEADER_LENGTH) 
            throw new UnrecoverablePduException("Invalid PDU length [0x" + HexUtil.toHexString(commandLength) + "] parsed");
        
        if (buffer.readableBytes() < commandLength) 
            return null;
        
        ByteBuf buffer0 = buffer.readSlice(commandLength);
        return doDecode(commandLength, buffer0);
    }

    protected Pdu doDecode(int commandLength, ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException 
    {
        buffer.skipBytes(Pdu.PDU_INT_LENGTH);

        CommandType commandId = CommandType.fromInt(buffer.readInt());
        int commandStatus = buffer.readInt();
        int sequenceNumber = buffer.readInt();

        Pdu pdu = null;
        
        switch(commandId)
    	{
			case CMD_ID_ALERT_NOTIFICATION:
				pdu = new AlertNotification();
				break;
			case CMD_ID_BIND_RECEIVER:
				pdu = new BindReceiver();
				break;
			case CMD_ID_BIND_RECEIVER_RESP:
				pdu = new BindReceiverResp();
				break;
			case CMD_ID_BIND_TRANSCEIVER:
				pdu = new BindTransceiver();
				break;
			case CMD_ID_BIND_TRANSCEIVER_RESP:
				pdu = new BindTransceiverResp();
				break;
			case CMD_ID_BIND_TRANSMITTER:
				pdu = new BindTransmitter();
				break;
			case CMD_ID_BIND_TRANSMITTER_RESP:
				pdu = new BindTransmitterResp();
				break;
			case CMD_ID_BROADCAST_SM:				
				pdu = new PartialPdu(commandId);
				break;
			case CMD_ID_BROADCAST_SM_RESP:
				pdu = new PartialPduResp(commandId);
				break;
			case CMD_ID_CANCEL_BROADCAST_SM:
				pdu = new PartialPdu(commandId);
				break;
			case CMD_ID_CANCEL_BROADCAST_SM_RESP:
				pdu = new PartialPduResp(commandId);
				break;
			case CMD_ID_CANCEL_SM:
				pdu = new CancelSm();
				break;
			case CMD_ID_CANCEL_SM_RESP:
				pdu = new CancelSmResp();
				break;
			case CMD_ID_DATA_SM:
				pdu = new DataSm();
				break;
			case CMD_ID_DATA_SM_RESP:
				pdu = new DataSmResp();
				break;
			case CMD_ID_DELIVER_SM:
				pdu = new DeliverSm();
				break;
			case CMD_ID_DELIVER_SM_RESP:
				pdu = new DeliverSmResp();
				break;
			case CMD_ID_ENQUIRE_LINK:
				pdu = new EnquireLink();
				break;
			case CMD_ID_ENQUIRE_LINK_RESP:
				pdu = new EnquireLinkResp();
				break;
			case CMD_ID_GENERIC_NACK:
				pdu = new GenericNack();
				break;
			case CMD_ID_OUTBIND:
				pdu = new PartialPdu(commandId);
				break;
			case CMD_ID_QUERY_BROADCAST_SM:
				pdu = new PartialPdu(commandId);
				break;
			case CMD_ID_QUERY_BROADCAST_SM_RESP:
				pdu = new PartialPduResp(commandId);
				break;
			case CMD_ID_QUERY_SM:
				pdu = new QuerySm();
				break;
			case CMD_ID_QUERY_SM_RESP:
				pdu = new QuerySmResp();
				break;
			case CMD_ID_REPLACE_SM:
				pdu = new ReplaceSm();
				break;
			case CMD_ID_REPLACE_SM_RESP:
				pdu = new ReplaceSmResp();
				break;
			case CMD_ID_SUBMIT_MULTI:
				pdu = new SubmitMulti();
				break;
			case CMD_ID_SUBMIT_MULTI_RESP:
				pdu = new SubmitMultiResp();
				break;
			case CMD_ID_SUBMIT_SM:
				pdu = new SubmitSm();
				break;
			case CMD_ID_SUBMIT_SM_RESP:
				pdu = new SubmitSmResp();
				break;
			case CMD_ID_UNBIND:
				pdu = new Unbind();
				break;
			case CMD_ID_UNBIND_RESP:
				pdu = new UnbindResp();
				break;
			case CMD_ID_UNKNOWN:					
			default:
				pdu = new PartialPdu(commandId);
				break;        		
    	}

        // set pdu header values
        pdu.setCommandLength(commandLength);
        pdu.setCommandStatus(MessageStatus.fromInt(commandStatus));
        pdu.setSequenceNumber(sequenceNumber);

        // check if we need to throw an exception
        if (pdu instanceof PartialPdu) 
            throw new UnknownCommandIdException(pdu, "Unsupported or unknown PDU request commandId [0x" + HexUtil.toHexString(commandId.getValue()) + "]");
        else if (pdu instanceof PartialPduResp) 
            throw new UnknownCommandIdException(pdu, "Unsupported or unknown PDU response commandId [0x" + HexUtil.toHexString(commandId.getValue()) + "]");
        
        try 
        {
            // parse pdu body parameters (may throw exception)
            pdu.readBody(buffer);
            // parse pdu optional parameters (may throw exception)
            pdu.readOptionalParameters(buffer);
        } 
        catch (RecoverablePduException e) 
        {
            if (e.getPartialPdu() == null) 
                e.setPartialPdu(pdu);
            
            throw e;
        }

        return pdu;
    }
}
