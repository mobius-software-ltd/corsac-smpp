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
import java.net.InetSocketAddress;

import io.netty.channel.Channel;

public class ChannelUtil 
{
    static public String createChannelName(Channel channel) 
    {
        // check if anything is null
        if (channel == null || channel.remoteAddress() == null) 
            return "ChannelWasNull";
        
        if (channel.remoteAddress() instanceof InetSocketAddress) 
        {
            InetSocketAddress addr = (InetSocketAddress)channel.remoteAddress();
            String remoteHostAddr = addr.getAddress().getHostAddress();
            int remoteHostPort = addr.getPort();
            return remoteHostAddr + ":" + remoteHostPort;
        }
        
        return channel.remoteAddress().toString();                
    }

    static public String getChannelRemoteHost(Channel channel) 
    {
        if (channel == null || channel.remoteAddress() == null) 
            return null;
        
        if (channel.remoteAddress() instanceof InetSocketAddress) 
        {
            InetSocketAddress addr = (InetSocketAddress)channel.remoteAddress();
            return addr.getAddress().getHostAddress();
        }
        
        return null;
    }

    static public int getChannelRemotePort(Channel channel) 
    {
        if (channel == null || channel.remoteAddress() == null) 
            return 0;
        
        if (channel.remoteAddress() instanceof InetSocketAddress) 
        {
            InetSocketAddress addr = (InetSocketAddress)channel.remoteAddress();
            return addr.getPort();
        }
        
        return 0;
    }
}