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
import java.util.List;

public class MessageRequestWrapper 
{
	private List<String> to;
	private byte[] data;
	private byte[] udh;
	private ReportData reportData;
	private AsyncCallback<RequestProcessingResult> response;
	
	public MessageRequestWrapper(List<String> to, byte[] data, byte[] udh,ReportData reportData,AsyncCallback<RequestProcessingResult> response) 
	{
		this.to = to;
		this.data = data;
		this.udh = udh;
		this.reportData = reportData;
		this.response = response;
	}
	
	public List<String> getTo() 
	{
		return to;
	}

	public void setTo(List<String> to) 
	{
		this.to = to;
	}

	public byte[] getData() 
	{
		return data;
	}

	public void setData(byte[] data) 
	{
		this.data = data;
	}

	public byte[] getUdh() 
	{
		return udh;
	}

	public void setUdh(byte[] udh) 
	{
		this.udh = udh;
	}

	public ReportData getReportData() 
	{
		return reportData;
	}

	public void setReportData(ReportData reportData) 
	{
		this.reportData = reportData;
	}

	public AsyncCallback<RequestProcessingResult> getResponse() 
	{
		return response;
	}
	
	public void setResponse(AsyncCallback<RequestProcessingResult> response) 
	{
		this.response = response;
	}		
}