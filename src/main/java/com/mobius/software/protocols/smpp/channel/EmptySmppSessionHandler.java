package com.mobius.software.protocols.smpp.channel;
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
import java.nio.channels.ClosedChannelException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mobius.software.protocols.smpp.Pdu;
import com.mobius.software.protocols.smpp.PduRequest;
import com.mobius.software.protocols.smpp.PduResponse;
import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

public class EmptySmppSessionHandler implements SmppSessionListener 
{
	public static Logger logger=LogManager.getLogger(EmptySmppSessionHandler.class);
	
	public EmptySmppSessionHandler() 
	{
    }

    @Override
    public void fireChannelUnexpectedlyClosed() 
    {
        logger.info("Default handling is to discard an unexpected channel closed");
    }

    @SuppressWarnings("rawtypes")
	@Override
    public void firePduRequestReceived(PduRequest pduRequest) 
    {
        logger.warn("Default handling is to discard unexpected request PDU: " + pduRequest);        
    }

    @SuppressWarnings("rawtypes")
	@Override
    public void fireExpectedPduResponseReceived(PduRequest pduRequest,PduResponse pduResponse) 
    {
        logger.warn("Default handling is to discard expected response PDU: " + pduResponse);
    }

    @Override
    public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) 
    {
        logger.warn("Default handling is to discard unexpected response PDU: " + pduResponse);
    }

    @Override
    public void fireUnrecoverablePduException(UnrecoverablePduException e) 
    {
        logger.warn("Default handling is to discard a unrecoverable exception:", e);
    }

    @Override
    public void fireRecoverablePduException(RecoverablePduException e) 
    {
        logger.warn("Default handling is to discard a recoverable exception:", e);
    }

    @Override
    public void fireUnknownThrowable(Throwable t) 
    {
        if (t instanceof ClosedChannelException) 
        {
            logger.warn("Unknown throwable received, but it was a ClosedChannelException, calling fireChannelUnexpectedlyClosed instead");
            fireChannelUnexpectedlyClosed();
        } 
        else 
            logger.warn("Default handling is to discard an unknown throwable:", t);        
    }

    @SuppressWarnings("rawtypes")
	@Override
    public void firePduRequestExpired(PduRequest pduRequest) 
    {
        logger.warn("Default handling is to discard expired request PDU: " + pduRequest);
    }

    @Override
    public boolean firePduReceived(Pdu pdu) 
    {
        return true;
    }

    @Override
    public boolean firePduDispatch(Pdu pdu) 
    {
        return true;
    }

	@Override
	public void setSession(SmppSession session) 
	{
		//not used here
	}
}