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
import com.mobius.software.protocols.smpp.PduRequest;
import com.mobius.software.protocols.smpp.PduResponse;
import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

public interface SmppSessionHandler
{
    public void fireChannelUnexpectedlyClosed();

    @SuppressWarnings("rawtypes")
	public void firePduRequestReceived(PduRequest pduRequest);

    @SuppressWarnings("rawtypes")
	public void firePduRequestExpired(PduRequest pduRequest);

    @SuppressWarnings("rawtypes")
	public void fireExpectedPduResponseReceived(PduRequest pduRequest,PduResponse pduResponse);

    public void fireUnexpectedPduResponseReceived(PduResponse pduResponse);

    public void fireUnrecoverablePduException(UnrecoverablePduException e);

    public void fireRecoverablePduException(RecoverablePduException e);

    public void fireUnknownThrowable(Throwable t);
    
    public void setSession(SmppSession session);
}