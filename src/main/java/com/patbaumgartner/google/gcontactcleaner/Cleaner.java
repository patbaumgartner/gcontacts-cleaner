package com.patbaumgartner.google.gcontactcleaner;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.extensions.Email;
import com.google.gdata.data.extensions.PhoneNumber;

public class Cleaner {

	private static final Logger logger = LoggerFactory.getLogger(Cleaner.class.getName());

	ContactsService myService;

	public void authenticate(String username, char[] password) throws Exception {
		myService = connectToContactsService(username, password);
	}

	public void clean() throws Exception {
		ContactFeed resultFeed = createContactFeed(myService);
		List<ContactEntry> entries = cleanupContacts(resultFeed.getEntries());
		entries = updatedContacts(myService, entries);
	}

	public String cleanPhoneNumer(String phoneNumber) {
		phoneNumber = phoneNumber.replace(" ", "");
		phoneNumber = phoneNumber.replace("-", "");
		phoneNumber = phoneNumber.trim();
		return phoneNumber;
	}

	public List<PhoneNumber> cleanPhoneNumbers(List<PhoneNumber> phoneNumbers) {
		for (PhoneNumber phone : phoneNumbers) {
			String phoneNumber = cleanPhoneNumer(phone.getPhoneNumber());
			if (!phoneNumber.equals(phone.getPhoneNumber())) {
				if (logger.isDebugEnabled()) {
					logger.debug("Phone number : " + phone.getPhoneNumber() + " cleaned phone number: " + phoneNumber);
				}
				phone.setPhoneNumber(phoneNumber);
			}
		}
		return phoneNumbers;
	}

	private List<PhoneNumber> cleanPhoneNumberDoubles(List<PhoneNumber> phoneNumbers) {
		List<String> phoneNumberList = new ArrayList<String>();
		List<PhoneNumber> tempPhoneNumbers = new ArrayList<PhoneNumber>(phoneNumbers);
		for (PhoneNumber phone : tempPhoneNumbers) {
			if (phoneNumberList.contains(phone.getPhoneNumber())) {
				phoneNumbers.remove(phone);
				if (logger.isDebugEnabled()) {
					logger.debug("Remove phone number double : " + phone.getPhoneNumber());
				}
			} else {
				phoneNumberList.add(phone.getPhoneNumber());
			}
		}
		return phoneNumbers;
	}

	private ContactEntry cleanNotes(ContactEntry contact) {
		contact.setContent(new PlainTextConstruct(null));
		return contact;
	}

	private List<Email> cleanEmailDoubles(List<Email> emailAddresses) {
		List<String> emailAddressesList = new ArrayList<String>();
		List<Email> tempEmailAddresses = new ArrayList<Email>(emailAddresses);
		for (Email email : tempEmailAddresses) {
			if (emailAddressesList.contains(email.getAddress())) {
				emailAddresses.remove(email);
				if (logger.isDebugEnabled()) {
					logger.debug("Remove email address double : " + email.getAddress());
				}
			} else {
				emailAddressesList.add(email.getAddress());
			}
		}
		return emailAddresses;
	}

	public ContactEntry cleanupContact(ContactEntry entry) {
		if (logger.isDebugEnabled()) {
			String name = entry.getTitle().getPlainText();
			if (name == "") {
				name = entry.getName().getFullName().toString();
			}
			logger.debug("Cleanup Contact Entry : " + name);
		}
		cleanPhoneNumbers(entry.getPhoneNumbers());
		cleanPhoneNumberDoubles(entry.getPhoneNumbers());
		cleanNotes(entry);
		cleanEmailDoubles(entry.getEmailAddresses());
		return entry;
	}

	public List<ContactEntry> cleanupContacts(List<ContactEntry> entries) {
		for (ContactEntry entry : entries) {
			entry = cleanupContact(entry);
		}
		return entries;
	}

	public ContactsService connectToContactsService(String userName, char[] password) throws Exception {
		ContactsService myService = new ContactsService("gcontacts-cleaner-1.0");
		myService.setUserCredentials(userName, new String(password));
		return myService;
	}

	public ContactFeed createContactFeed(ContactsService myService) throws Exception {
		URL feedUrl = new URL("https://www.google.com/m8/feeds/contacts/default/full/?max-results=1000");
		ContactFeed resultFeed = myService.getFeed(feedUrl, ContactFeed.class);
		return resultFeed;
	}

	public ContactEntry updateContact(ContactsService myService, ContactEntry entry) throws Exception {
		if (logger.isDebugEnabled()) {
			String name = entry.getTitle().getPlainText();
			if (name == "") {
				name = entry.getName().getFullName().toString();
			}
			logger.debug("Update Contact Entry : " + entry.getTitle().getPlainText());
		}
		URL editUrl = new URL(entry.getEditLink().getHref());
		return myService.update(editUrl, entry);
	}

	private List<ContactEntry> updatedContacts(ContactsService myService, List<ContactEntry> entries) throws Exception {
		for (ContactEntry entry : entries) {
			entry = updateContact(myService, entry);
		}
		return entries;
	}
}
