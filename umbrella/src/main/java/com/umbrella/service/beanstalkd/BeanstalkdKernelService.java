package com.umbrella.service.beanstalkd;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.BeanstalkdJob;
import com.umbrella.kit.PostKit;

public class BeanstalkdKernelService extends BeanstalkdService {

	protected BeanstalkdKernelService() {
		super("kernel", LogManager.getLogger("beanstalkd-kernel"));
	}

	@Inject private PostKit kit;
	
	@Override
	protected boolean execute(BeanstalkdJob job) throws Exception {
		JSONObject reply = JSON.parseObject(job.getData());
		String pid = checkNotNull(reply.getString("pid"), "no pid in job:" + job.getId());
		String action = checkNotNull(reply.getString("action"), "no action in job:" + job.getId());
		if (action.equals("create")) {
			return kit.create(pid);
		} else if (action.equals("delete")) {
			return kit.delete(pid);
		} else if (action.equals("update")) {
			return kit.update(pid);
		} else {
			throw new IllegalStateException("BAD kernel action:" + action);
		}
	}
	
}
