package com.umbrella.service.beanstalkd.mail;

import org.apache.logging.log4j.LogManager;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.BeanstalkdJob;
import com.umbrella.service.beanstalkd.BeanstalkdService;

public class BeanstalkdMailService extends BeanstalkdService {
	
	@Inject private BeanstalkdMailManager manager;

	public BeanstalkdMailService() {
		super("email", LogManager.getLogger("beanstalkd-mail-service"));
	}

	@Override
	protected void execute(BeanstalkdJob job) throws Exception {
		String email = job.getData();
		String ticket = manager.raiseTicket(email);
		if(Strings.isNullOrEmpty(ticket)) throw new IllegalStateException("ticket is null");
		manager.sendMail(ticket, email);
		LOG.info("邮件发送完毕.");
	}
}
