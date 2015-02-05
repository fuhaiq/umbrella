package com.umbrella.mail;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.throwingproviders.CheckedProvides;
import com.google.inject.throwingproviders.ThrowingProviderBinder;
import com.umbrella.UmbrellaConfig;

public class MailModule extends AbstractModule {
	
	@Override
	protected void configure() {
		install(ThrowingProviderBinder.forModule(this));
	}

	@CheckedProvides(MailMessageProvider.class)
	@Singleton
	Message provideMessage(UmbrellaConfig umbrella) throws MessagingException {
		MailConfig config = umbrella.getMail();
		Session mailSession = Session.getInstance(config.getProperties(), new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(config.getUser(), config.getPassword());
			}
		});
		Message mailMessage = new MimeMessage(mailSession);
		Address from = new InternetAddress(config.getUser());
		mailMessage.setFrom(from);
		return mailMessage;
	}
}
