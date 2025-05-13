package com.mobius.software.protocols.smpp;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.Date;

import org.junit.Test;

public class ParsingTest {
	
	@Test
	public void testExpirationDate() throws ParseException
	{
		String date = "250430115314000-";
		Date parsedDate = SmppHelper.parseSmppDate(date);
		assertEquals(parsedDate.getTime(), 1746013994000L);
	}
}