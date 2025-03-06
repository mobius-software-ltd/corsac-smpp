package com.mobius.software.protocols.smpp.channel;
import com.mobius.software.common.dal.timers.Timer;
import com.mobius.software.protocols.smpp.PduRequest;

public interface RequestTimeoutInterface extends Timer
{
	@SuppressWarnings("rawtypes")
	public PduRequest getRequest();
}
