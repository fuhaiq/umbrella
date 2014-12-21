package com.umbrella.service.beanstalkd;

import org.apache.logging.log4j.LogManager;

import com.google.inject.Inject;
import com.umbrella.beanstalkd.BeanstalkdJob;

public class BeanstalkdMailService extends BeanstalkdService {
	
	@Inject private BeanstalkdMailManager manager;

	public BeanstalkdMailService() {
		super("mail", LogManager.getLogger("beanstalkd-mail-service"));
	}

	@Override
	protected void execute(BeanstalkdJob job) throws Exception {
		String email = job.getData();
		String ticket = manager.raiseTicket(email);
		manager.sendMail(ticket, email);
		LOG.info("邮件发送完毕.");
	}
}
