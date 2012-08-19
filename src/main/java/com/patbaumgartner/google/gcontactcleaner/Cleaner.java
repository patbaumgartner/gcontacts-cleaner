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
import com.google.gdata.data.extensions.AdditionalName;
import com.google.gdata.data.extensions.Email;
import com.google.gdata.data.extensions.FamilyName;
import com.google.gdata.data.extensions.FullName;
import com.google.gdata.data.extensions.GivenName;
import com.google.gdata.data.extensions.Name;
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
		removeEmptyContacts(entries);
	}

	private void removeEmptyContacts(List<ContactEntry> entries) throws Exception {
		for(ContactEntry contact: entries){
			if(!contact.hasPhoneNumbers() && !contact.hasEmailAddresses()){
				if(logger.isDebugEnabled()){
					logger.debug("Delete Empty Contact : " + contact.getName().getFullName().getValue());
					contact.delete();
				}
			}
		}
	}

	public String cleanPhoneNumer(String phoneNumber) {
		phoneNumber = phoneNumber.replace(" ", "");
		phoneNumber = phoneNumber.replace("-", "");
		phoneNumber = phoneNumber.trim();
		if (phoneNumber.startsWith("00")) {
			phoneNumber = phoneNumber.substring(2);
			phoneNumber = "+" + phoneNumber;
		}
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

	private List<Email> cleanEmailAddresses(List<Email> emailAddresses) {
		for (Email email : emailAddresses) {
			email.setAddress(email.getAddress().toLowerCase().trim());
			if (logger.isDebugEnabled()) {
				logger.debug("Cleanup email address : " + email.getAddress());
			}
		}
		return emailAddresses;
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
		cleanName(entry.getName());
		cleanPhoneNumbers(entry.getPhoneNumbers());
		cleanPhoneNumberDoubles(entry.getPhoneNumbers());
		cleanNotes(entry);
		cleanEmailAddresses(entry.getEmailAddresses());
		cleanEmailDoubles(entry.getEmailAddresses());
		return entry;
	}

	private void cleanName(Name name) {
		if (name != null) {
			GivenName givenName = name.getGivenName();
			if (givenName != null) {
				String firstName = givenName.getValue();
				firstName = firstName.trim();
				givenName.setImmutable(false);
				givenName.setValue(firstName);
				givenName.setYomi(null);
			}

			FamilyName familyName = name.getFamilyName();
			if (familyName != null) {
				String lastName = familyName.getValue();
				lastName = lastName.trim();
				familyName.setImmutable(false);
				familyName.setValue(lastName);
				familyName.setYomi(null);
			}
			
			AdditionalName additionalName = name.getAdditionalName();
			if (additionalName != null) {
				String middleName = additionalName.getValue();
				middleName = middleName.trim();
				additionalName.setImmutable(false);
				additionalName.setValue(middleName);
				additionalName.setYomi(null);
			}
			
			FullName fullName = name.getFullName();
			if (fullName != null) {
				String wholeName = fullName.getValue();
				wholeName = wholeName.trim();
				fullName.setImmutable(false);
				fullName.setValue(wholeName);
				fullName.setYomi(null);
			}

			name.setNamePrefix(null);
			name.setNameSuffix(null);
		}
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
		URL feedUrl = new URL("https://www.google.com/m8/feeds/contacts/default/full/?max-results=2000");
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
