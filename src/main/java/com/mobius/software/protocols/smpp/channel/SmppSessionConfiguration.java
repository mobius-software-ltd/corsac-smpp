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
import com.mobius.software.protocols.smpp.Address;
import com.mobius.software.protocols.smpp.SmppBindType;

public class SmppSessionConfiguration extends SmppConnectionConfiguration 
{
	public static final long DEFAULT_BIND_TIMEOUT = 5000;
	public static final long DEFAULT_REQUEST_EXPIRY_TIMEOUT = 30000;
	
	// SSL
    private boolean useSsl = false;
    private SslConfiguration sslConfiguration;
    
    private String name;
    
    // configuration settings
    private SmppBindType type;
    private String systemId;
    private String password;
    private String systemType;
    private SmppVersion interfaceVersion;
    private Address addressRange;
    private long bindTimeout;
    private long requestExpiryTimeout;
    
    public SmppSessionConfiguration() 
    {
        this(SmppBindType.TRANSCEIVER, null, null, null);
    }

    public SmppSessionConfiguration(SmppBindType type, String systemId, String password) 
    {
        this(type, systemId, password, null);
    }

    public SmppSessionConfiguration(SmppBindType type, String systemId, String password, String systemType) 
    {
        this.type = type;
        this.systemId = systemId;
        this.password = password;
        this.systemType = systemType;
        this.interfaceVersion = SmppVersion.VERSION_3_4;
        this.bindTimeout = DEFAULT_BIND_TIMEOUT;
        this.requestExpiryTimeout = DEFAULT_REQUEST_EXPIRY_TIMEOUT; 
    }

    public void setName(String value) 
    {
        this.name = value;
    }

    public String getName() 
    {
        return this.name;
    }

    public void setBindTimeout(long value) 
    {
        this.bindTimeout = value;
    }

    public long getBindTimeout() 
    {
        return this.bindTimeout;
    }

    public void setType(SmppBindType bindType) 
    {
        this.type = bindType;
    }

    public SmppBindType getType() 
    {
        return this.type;
    }

    public void setSystemId(String value) 
    {
        this.systemId = value;
    }

    public String getSystemId() 
    {
        return this.systemId;
    }

    public void setPassword(String value) 
    {
        this.password = value;
    }

    public String getPassword() 
    {
        return this.password;
    }

    public void setSystemType(String value) 
    {
        this.systemType = value;
    }

    public String getSystemType() 
    {
        return this.systemType;
    }

    public void setInterfaceVersion(SmppVersion value) 
    {
        this.interfaceVersion = value;
    }

    public SmppVersion getInterfaceVersion() 
    {
        return this.interfaceVersion;
    }

    public Address getAddressRange() 
    {
        return this.addressRange;
    }

    public void setAddressRange(Address value) 
    {
        this.addressRange = value;
    }

    public void setUseSsl(boolean value) 
    {
    	if (getSslConfiguration() == null) 
    		setSslConfiguration(new SslConfiguration());
	
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

    public long getRequestExpiryTimeout() 
    {
        return requestExpiryTimeout;
    }

    public void setRequestExpiryTimeout(long requestExpiryTimeout) 
    {
        this.requestExpiryTimeout = requestExpiryTimeout;
    }
}