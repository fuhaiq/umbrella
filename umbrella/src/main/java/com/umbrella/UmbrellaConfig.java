package com.umbrella;

import java.util.Map;

import com.umbrella.beanstalkd.BeanstalkdConfig;
import com.umbrella.kernel.KernelConfig;
import com.umbrella.mail.MailConfig;
import com.umbrella.redis.JedisConfig;
import com.umbrella.service.netty.NettyServiceConfig;

public class UmbrellaConfig {
	
	private BeanstalkdConfig beanstalkd;
	
	private KernelConfig kernel;
	
	private Map<String, NettyServiceConfig> service;
	
	private JedisConfig redis;
	
	private MailConfig mail;
	
	public KernelConfig getKernel() {
		return kernel;
	}

	public void setKernel(KernelConfig kernel) {
		this.kernel = kernel;
	}

	public Map<String, NettyServiceConfig> getService() {
		return service;
	}

	public void setService(Map<String, NettyServiceConfig> service) {
		this.service = service;
	}

	public BeanstalkdConfig getBeanstalkd() {
		return beanstalkd;
	}

	public void setBeanstalkd(BeanstalkdConfig beanstalkd) {
		this.beanstalkd = beanstalkd;
	}

	public JedisConfig getRedis() {
		return redis;
	}

	public void setRedis(JedisConfig redis) {
		this.redis = redis;
	}

	public MailConfig getMail() {
		return mail;
	}

	public void setMail(MailConfig mail) {
		this.mail = mail;
	}
}
