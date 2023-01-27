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
import java.util.concurrent.atomic.AtomicInteger;

public class SequenceNumber 
{
    public static final int MIN_VALUE = 0x00000000;
    public static final int DEFAULT_VALUE = 0x00000001;
    public static final int MAX_VALUE = 0x7FFFFFFF;

    private AtomicInteger value=new AtomicInteger(DEFAULT_VALUE);

    public SequenceNumber() 
    {
    	
    }

    public SequenceNumber(int initialValue)
    {
        this.value.set(initialValue);
    }

    public int next() 
    {
    	this.value.compareAndSet(MAX_VALUE, MIN_VALUE);
    	return this.value.incrementAndGet();        
    }

    public int peek() 
    {
        return this.value.get();
    }

    public void reset() 
    {
        this.value.set(DEFAULT_VALUE);
    }
}
