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
import com.mobius.software.protocols.smpp.BaseBind;
import com.mobius.software.protocols.smpp.PduRequest;
import com.mobius.software.protocols.smpp.PduResponse;
import com.mobius.software.protocols.smpp.SmppBindType;
import com.mobius.software.protocols.smpp.exceptions.RecoverablePduException;
import com.mobius.software.protocols.smpp.exceptions.SmppChannelException;
import com.mobius.software.protocols.smpp.exceptions.SmppTimeoutException;
import com.mobius.software.protocols.smpp.exceptions.UnrecoverablePduException;

public interface SmppSession 
{
    public enum Type 
    {
        SERVER,
        CLIENT
    }

    static public final int STATE_INITIAL = 0;
    static public final int STATE_OPEN = 1;
    static public final int STATE_BINDING = 2;
    static public final int STATE_BOUND = 3;
    static public final int STATE_UNBINDING = 4;
    static public final int STATE_CLOSED = 5;
    
    static public final String[] STATES = {
        "INITIAL", "OPEN", "BINDING", "BOUND", "UNBINDING", "CLOSED"
    };

    public SmppBindType getBindType();

    public Type getLocalType();

    public Type getRemoteType();

    public SmppSessionConfiguration getConfiguration();

    public String getStateName();

    public SmppVersion getInterfaceVersion();

    public boolean areOptionalParametersSupported();

    public boolean isOpen();

    public boolean isBinding();

    public boolean isBound();

    public boolean isUnbinding();

    public boolean isClosed();

    public long getBoundTime();

    public void close();

    public void passiveClose();
    
    public void unbind(long timeoutMillis);
    
    public void destroy();

    @SuppressWarnings("rawtypes")
	public void bind(BaseBind request,long timeoutMillis) throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException;

    public void sendRequestPdu(PduRequest<?> request) throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException;

    public void sendResponsePdu(PduResponse response) throws RecoverablePduException, UnrecoverablePduException, SmppChannelException, InterruptedException;
}
