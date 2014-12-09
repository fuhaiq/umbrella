package com.umbrella.service.beanstalkd;

import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.ibatis.session.SqlSessionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.umbrella.mail.MailMessageProvider;

public class BeanstalkdMailManager {
	
	private final Logger LOG = LogManager.getLogger("beanstalkd-mail-manager");
	
	@Inject private SqlSessionManager manager;
	
	@Inject private MailMessageProvider provider;
	
	private final String body = "%s, 您好,<br>您可以通过下面的链接设置您的密码<br><a href='http://www.wiseker.com/user/reset?ticket=%s' target='_blank'>设置密码</a><br>如果您未发起这次设置密码, 请忽略这封邮件。 如果您不点击上面的链接, 并且设置密码的话, 您不会开启重置密码。";
	
	public String raiseTicket(String email) throws SQLException {
		String uuid = UUID.randomUUID().toString();
		Map<String, String> ticket = Maps.newHashMap();
		ticket.put("ticket", uuid);
		ticket.put("email", email);
		return (manager.insert("forget.insert", ticket) == 1 ? uuid : null);
	}
	
	public boolean sendMail(String ticket, String email) throws SQLException {
		String nickName = manager.selectOne("user.selectNickname", email);
		try {
			Message message = provider.get();
			Address to = new InternetAddress(email);
			message.setRecipient(Message.RecipientType.TO,to);
			message.setSubject("威思客密码找回");
			message.setSentDate(new Date());
			Multipart mainPart = new MimeMultipart();
			BodyPart html = new MimeBodyPart();
			html.setContent(String.format(body, nickName, ticket), "text/html; charset=utf-8");
			mainPart.addBodyPart(html);
			message.setContent(mainPart);   
		    Transport.send(message);   
			return true;
		} catch (MessagingException e) {
			LOG.error("发送邮件失败:", e);
			return false;
		}
	}
}
