package com.mobius.software.protocols.smpp;

import java.util.List;

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
public interface ConnectionListener 
{
	void connected(String uniqueID);
	
	void disconnected(String uniqueID);
	
	void heartbeatReceived(String uniqueID);
	
	void messageReceived(String uniqueID,List<String> to, byte[] data, byte[] udh, ReportData reportData, AsyncCallback<RequestProcessingResult> callback);
	
	void statusReceived(String uniqueID, String messageID, AsyncCallback<DeliveryProcessingResult> callback);
	
	void responseReceived(String uniqueID, String messageID, String remoteMessageID, MessageStatus status);
	
	void timeoutReceived(String uniqueID, String messageID);
}