package com.mobius.software.protocols.smpp.exceptions;

import com.mobius.software.protocols.smpp.Pdu;
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
public class RecoverablePduException extends Exception 
{
    static final long serialVersionUID = 1L;
    
    private Pdu partialPdu;

    public RecoverablePduException(String msg) 
    {
        super(msg);
    }

    public RecoverablePduException(String msg, Throwable t) 
    {
        super(msg, t);
    }

    public RecoverablePduException(Pdu partialPdu, String msg) 
    {
        super(msg);
        this.partialPdu = partialPdu;
    }

    public RecoverablePduException(Pdu partialPdu, String msg, Throwable t) 
    {
        super(msg, t);
        this.partialPdu = partialPdu;
    }

    public void setPartialPdu(Pdu pdu) 
    {
        this.partialPdu = pdu;
    }

    public Pdu getPartialPdu() 
    {
        return this.partialPdu;
    }
}