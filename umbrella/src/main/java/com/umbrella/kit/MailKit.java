package com.umbrella.kit;

import static com.mongodb.client.model.Filters.eq;

import java.sql.SQLException;
import java.util.Date;
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

import org.bson.Document;

import com.google.inject.Inject;
import com.mongodb.MongoClient;
import com.umbrella.mail.MailMessageProvider;
import com.umbrella.mongo.MongoCycle;
import com.umbrella.session.Session;

public class MailKit {
	
	@Inject private MailMessageProvider provider;
	
	@Inject private Session<MongoClient> mongoSession;
	
	private final String body = "%s, 您好,<br>您可以通过下面的链接设置您的密码<br><a href='http://www.wiseker.com/user/reset?ticket=%s' target='_blank'>设置密码</a><br>如果您未发起这次设置密码, 请忽略这封邮件。 如果您不点击上面的链接, 并且设置密码的话, 您不会开启重置密码。";
	
	@MongoCycle
	public String raiseTicket(String email) throws SQLException {
		String uuid = UUID.randomUUID().toString();
		mongoSession.get().getDatabase("umbrella").getCollection("forget").insertOne(new Document("email", email).append("ticket", uuid));
		return uuid;
	}
	
	public void sendMail(String ticket, String email) throws SQLException, MessagingException {
		Document user = mongoSession.get().getDatabase("umbrella").getCollection("user").find(eq("email", email)).first();
		String nickName = user.getString("nickname");
		Message message = provider.get();
		Address to = new InternetAddress(email);
		message.setRecipient(Message.RecipientType.TO, to);
		message.setSubject("威思客密码找回");
		message.setSentDate(new Date());
		Multipart mainPart = new MimeMultipart();
		BodyPart html = new MimeBodyPart();
		html.setContent(String.format(body, nickName, ticket), "text/html; charset=utf-8");
		mainPart.addBodyPart(html);
		message.setContent(mainPart);
		Transport.send(message);
	}
}
