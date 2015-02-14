package com.umbrella.service.beanstalkd;

import org.apache.logging.log4j.LogManager;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.BeanstalkdJob;
import com.umbrella.kit.MailKit;

public class BeanstalkdMailService extends BeanstalkdService {
	
	@Inject private MailKit kit;

	public BeanstalkdMailService() {
		super("email-forget", LogManager.getLogger("beanstalkd-email-forget"));
	}

	@Override
	protected void execute(BeanstalkdJob job) throws Exception {
		String email = job.getData();
		String ticket = kit.raiseTicket(email);
		if(Strings.isNullOrEmpty(ticket)) throw new IllegalStateException("ticket is null");
		kit.sendMail(ticket, email);
		LOG.info("邮件发送完毕.");
	}
}
