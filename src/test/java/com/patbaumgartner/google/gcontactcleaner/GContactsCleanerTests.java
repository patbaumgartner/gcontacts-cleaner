package com.patbaumgartner.google.gcontactcleaner;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class GContactsCleanerTests {

	private static final String USERNAME = "username";
	private static final char[] PASSWORD = "password".toCharArray();
	private Cleaner cleaner;

	@Before
	public void setup() {
		cleaner = new Cleaner();
	}

	@Test
	public void testAuthentication() {
		try {
			cleaner.authenticate(USERNAME, PASSWORD);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCleanContacts() throws Exception {
		cleaner.authenticate(USERNAME, PASSWORD);
		cleaner.clean();
	}

}
