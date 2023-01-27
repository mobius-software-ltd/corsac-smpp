package com.mobius.software.protocols.smpp;
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
public class SmppChannelConfig 
{
	private String username;
	private String password;
	private Long connectionTimeout;
	private Long bindTimeout;
	private Long requestExpiryTimeout;
	private Long enquireLinkInterval;
    private Integer maxChannels;
    
    public SmppChannelConfig(String username,String password,Long connectionTimeout,Long bindTimeout,Long requestExpiryTimeout,Long enquireLinkInterval,Integer maxChannels)
	{
    	this.username=username;
    	this.password=password;
		this.connectionTimeout=connectionTimeout;
		this.bindTimeout=bindTimeout;
		this.requestExpiryTimeout=requestExpiryTimeout;
		this.enquireLinkInterval=enquireLinkInterval;		
		this.maxChannels=maxChannels;
	}
    
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Long getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(Long connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Long getBindTimeout() {
		return bindTimeout;
	}

	public void setBindTimeout(Long bindTimeout) {
		this.bindTimeout = bindTimeout;
	}

	public Long getRequestExpiryTimeout() {
		return requestExpiryTimeout;
	}

	public void setRequestExpiryTimeout(Long requestExpiryTimeout) {
		this.requestExpiryTimeout = requestExpiryTimeout;
	}

	public Long getEnquireLinkInterval() {
		return enquireLinkInterval;
	}

	public void setEnquireLinkInterval(Long enquireLinkInterval) {
		this.enquireLinkInterval = enquireLinkInterval;
	}

	public Integer getMaxChannels() {
		return maxChannels;
	}

	public void setMaxChannels(Integer maxChannels) {
		this.maxChannels = maxChannels;
	}		
}