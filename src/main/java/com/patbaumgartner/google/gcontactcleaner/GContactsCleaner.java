package com.patbaumgartner.google.gcontactcleaner;

import java.io.Console;
import java.util.Arrays;

public class GContactsCleaner {

	public static void main(String[] args) {
		try {
			Console console = System.console();

			String username = console.readLine("Username: ");
			char[] password = console.readPassword("Password: ");

			Cleaner cleaner = new Cleaner();
			cleaner.authenticate(username, password);

			Arrays.fill(password, ' ');

			cleaner.clean();
		} catch (Exception e) {
			System.out.println("Error occured: " + e.getMessage());
		} finally {
			System.exit(0);
		}
	}
}