package com.mobius.software.protocols.smpp.exceptions;
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
public class NotEnoughDataInBufferException extends RecoverablePduException 
{
    static final long serialVersionUID = 1L;
    
    private int available;
    private int expected;

    public NotEnoughDataInBufferException(int available, int expected) 
    {
        this(null, available, expected);
    }

    public NotEnoughDataInBufferException(String msg, int available, int expected) 
    {
        super("Not enough data in byte buffer to complete encoding/decoding [expected: " + expected + ", available: " + available + "]" + (msg == null ? "" : ": " + msg));
        this.available = available;
        this.expected = expected;
    }

    public int getAvailable() 
    {
        return available;
    }

    public int getExpected() 
    {
        return expected;
    }
}