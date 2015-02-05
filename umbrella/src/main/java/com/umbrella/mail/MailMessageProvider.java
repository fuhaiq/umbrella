package com.umbrella.mail;

import javax.mail.Message;
import javax.mail.MessagingException;

import com.google.inject.throwingproviders.CheckedProvider;

public interface MailMessageProvider extends CheckedProvider<Message>{
	
	@Override
	Message get() throws MessagingException;
	
}
