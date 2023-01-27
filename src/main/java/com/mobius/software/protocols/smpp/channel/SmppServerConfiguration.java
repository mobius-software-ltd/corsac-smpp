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
public class SmppServerConfiguration extends SmppConnectionConfiguration 
{
	public static final Integer DEFAULT_SERVER_MAX_CONNECTION_SIZE=5;
	
    private String name;

    // SSL
    private boolean useSsl = false;
    private SslConfiguration sslConfiguration;
    
    private long bindTimeout;       
    private String systemId;
    
    private boolean autoNegotiateInterfaceVersion;
    private SmppVersion interfaceVersion;
    
    private int maxConnectionSize;
    
    private long defaultRequestExpiryTimeout = SmppSessionConfiguration.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
    
    public SmppServerConfiguration() 
    {
        super("0.0.0.0", 2775, 5000l);
        this.name = "SmppServer";
        this.bindTimeout = 5000;
        this.systemId = "cloudhopper";
        this.autoNegotiateInterfaceVersion = true;
        this.interfaceVersion = SmppVersion.VERSION_3_4;
        this.maxConnectionSize = DEFAULT_SERVER_MAX_CONNECTION_SIZE;
        this.defaultRequestExpiryTimeout = SmppSessionConfiguration.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
    }

    public int getMaxConnectionSize() 
    {
        return maxConnectionSize;
    }

    public void setMaxConnectionSize(int maxConnectionSize) 
    {
        if (this.maxConnectionSize < 1) 
            throw new IllegalArgumentException("Max connection size must be >= 1");
        
        this.maxConnectionSize = maxConnectionSize;
    }

    public void setName(String value) 
    {
        this.name = value;
    }

    public String getName() 
    {
        return this.name;
    }

    public void setUseSsl(boolean value) 
    {
    	this.useSsl = value;
    }

    public boolean isUseSsl() 
    { 
    	return this.useSsl;
    }

    public void setSslConfiguration(SslConfiguration value) 
    {
		this.sslConfiguration = value;
		setUseSsl(true);
    }

    public SslConfiguration getSslConfiguration() 
    {
    	return this.sslConfiguration;
    }

    public void setBindTimeout(long value) 
    {
        this.bindTimeout = value;
    }

    public long getBindTimeout() 
    {
        return this.bindTimeout;
    }

    public void setSystemId(String value) 
    {
        this.systemId = value;
    }

    public String getSystemId() 
    {
        return this.systemId;
    }

    public boolean isAutoNegotiateInterfaceVersion() 
    {
        return autoNegotiateInterfaceVersion;
    }

    public void setAutoNegotiateInterfaceVersion(boolean autoNegotiateInterfaceVersion) 
    {
        this.autoNegotiateInterfaceVersion = autoNegotiateInterfaceVersion;
    }

    public SmppVersion getInterfaceVersion() 
    {
        return interfaceVersion;
    }

    public void setInterfaceVersion(SmppVersion interfaceVersion) 
    {
        this.interfaceVersion = interfaceVersion;
    }

    public long getDefaultRequestExpiryTimeout() 
    {
        return defaultRequestExpiryTimeout;
    }

    public void setDefaultRequestExpiryTimeout(long defaultRequestExpiryTimeout) 
    {
        this.defaultRequestExpiryTimeout = defaultRequestExpiryTimeout;
    }
}