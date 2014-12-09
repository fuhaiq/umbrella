package com.umbrella.service.beanstalkd;

import java.sql.SQLException;

import org.apache.commons.pool2.ObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.Beanstalkd;

public class BeanstalkdMailService extends AbstractExecutionThreadService {

	private final Logger LOG = LogManager.getLogger("beanstalkd-mail-service");
	
	@Inject private ObjectPool<Beanstalkd> pool;
	
	@Inject private BeanstalkdMailManager manager;
	
	private Beanstalkd bean;
	
	@Override
	protected void startUp() throws Exception {
		bean = pool.borrowObject();
		bean.watch("email");
		LOG.info("beanstalkd mail service starts");
	}

	@Override
	protected void shutDown() throws Exception {
		pool.invalidateObject(bean);
		LOG.info("beanstalkd mail service stops");
	}

	@Override
	protected void triggerShutdown() {
		bean.close();
	}
	
	@Override
	protected void run() throws Exception {
		while (isRunning()) {
			String job = bean.reserve();
			if(!Strings.isNullOrEmpty(job)) {
				BeanstalkdMailJob mailJob = new BeanstalkdMailJob(job);
				if(execute(mailJob)) {
					bean.delete(mailJob.getId());
				} else {
					bean.release(mailJob.getId(), (long) Math.pow(2, 31), 0);
				}
			}
		}
	}

	public boolean execute(BeanstalkdMailJob mailJob) throws SQLException {
		String ticket = manager.raiseTicket(mailJob.getEmail());
		if(!Strings.isNullOrEmpty(ticket)) {
			boolean ok = manager.sendMail(ticket, mailJob.getEmail());
			if(ok) {
				LOG.info("邮件发送完毕.");
			}
			return ok;
		} else {
			LOG.error("raise ticket failed.");
			return false;
		}
		
	}
}
