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
public class SmppConnectionConfiguration 
{
	public static final long DEFAULT_CONNECT_TIMEOUT = 10000;
    
	private String host;
    private int port;
    private long connectTimeout;

    public SmppConnectionConfiguration() 
    {
        this(null, 0, DEFAULT_CONNECT_TIMEOUT);
    }

    public SmppConnectionConfiguration(String host, int port, long connectTimeout) 
    {
        this.host = host;
        this.port = port;
        this.connectTimeout = connectTimeout;
    }

    public void setHost(String value) 
    {
        this.host = value;
    }

    public String getHost() 
    {
        return this.host;
    }

    public void setPort(int value) 
    {
        this.port = value;
    }

    public int getPort() 
    {
        return this.port;
    }

    public void setConnectTimeout(long value) 
    {
        this.connectTimeout = value;
    }

    public long getConnectTimeout() 
    {
        return this.connectTimeout;
    }
}