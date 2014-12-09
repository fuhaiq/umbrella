package com.umbrella.mail;

import java.util.Properties;

public class MailConfig {
	private String host;
	private int port;
	private String user;
	private String password;
	private String from;
	private String to;
	private String subject;
	private String content;
	private boolean validate;
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public boolean isValidate() {
		return validate;
	}
	public void setValidate(boolean validate) {
		this.validate = validate;
	}
	public Properties getProperties() {
		Properties p = new Properties();
		p.put("mail.smtp.host", host);
		p.put("mail.smtp.port", port);
		p.put("mail.smtp.auth", validate ? "true" : "false");
		return p;
	}
}
