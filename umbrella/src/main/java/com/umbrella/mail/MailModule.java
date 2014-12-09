package com.umbrella.mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.alibaba.fastjson.JSON;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.throwingproviders.CheckedProvides;
import com.google.inject.throwingproviders.ThrowingProviderBinder;

public class MailModule extends AbstractModule {

	private final String config;
	
	public MailModule(String config) {
		this.config = config;
	}
	
	@Override
	protected void configure() {
		try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(config);
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			bind(MailConfig.class).toInstance(JSON.parseObject(builder.toString(), MailConfig.class));
		} catch (IOException e) {
			addError(e);
		}
		install(ThrowingProviderBinder.forModule(this));
	}

	@CheckedProvides(MailMessageProvider.class)
	@Singleton
	Message provideMessage(MailConfig config) throws MessagingException {
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
